package dev.haomin.resumer.app.auth.service.impl

import dev.haomin.resumer.app.auth.repo.AccountRepo
import dev.haomin.resumer.app.auth.repo.query.AccountInsertQuery
import dev.haomin.resumer.app.auth.service.RegisterService
import dev.haomin.resumer.app.auth.service.dto.CompleteRegistrationCmd
import dev.haomin.resumer.app.auth.service.dto.CompleteRegistrationResult
import dev.haomin.resumer.app.auth.service.dto.ResendVerificationCmd
import dev.haomin.resumer.app.auth.service.dto.SendVerificationCmd
import dev.haomin.resumer.app.auth.service.dto.SendVerificationResult
import dev.haomin.resumer.app.auth.service.dto.VerifyCodeCmd
import dev.haomin.resumer.app.auth.service.dto.VerifyCodeResult
import dev.haomin.resumer.app.auth.service.model.RegisterAttempt
import dev.haomin.resumer.app.auth.prop.RegisterProperties
import dev.haomin.resumer.app.common.exception.ConflictException
import dev.haomin.resumer.app.common.exception.InvalidParamException
import dev.haomin.resumer.app.common.id.OTCGenerator
import dev.haomin.resumer.app.common.id.UUIDGenerator
import dev.haomin.resumer.app.common.security.CryptoUtils
import dev.haomin.resumer.app.framework.redis.RedisClient
import dev.haomin.resumer.app.framework.redis.getObj
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit

@Service
class RegisterServiceImpl(
    private val accountRepo: AccountRepo,
    private val redisClient: RedisClient,
    private val passwordEncoder: PasswordEncoder,
    private val properties: RegisterProperties,
    private val otcGenerator: OTCGenerator,
) : RegisterService {
    private val hashSecretBytes: ByteArray = CryptoUtils.decodeString(properties.codeHashSecret)

    override fun sendVerificationEmail(cmd: SendVerificationCmd): SendVerificationResult {
        val normalizedEmail = normalizeEmail(cmd.email)
        val existingAttemptId = redisClient.get(emailKey(normalizedEmail))
        if (existingAttemptId != null) {
            val existingAttempt = redisClient.getObj<RegisterAttempt>(attemptKey(existingAttemptId))
            if (existingAttempt != null) {
                return SendVerificationResult(
                    attemptId = existingAttempt.attemptId,
                    expiresInSeconds = remainingTtlSeconds(attemptKey(existingAttempt.attemptId)),
                )
            }
            redisClient.delete(emailKey(normalizedEmail))
        }

        val attemptId = UUIDGenerator.next().toString()
        val code = otcGenerator.next(properties.codeLength)
        val now = OffsetDateTime.now()
        val ttlSeconds = durationToSeconds(properties.attemptLifetime)
        val attempt = RegisterAttempt(
            attemptId = attemptId,
            email = normalizedEmail,
            codeHash = hashVerificationCode(code),
            createdAt = now,
        )

        redisClient.setObj(attemptKey(attemptId), attempt, ttlSeconds, TimeUnit.SECONDS)
        redisClient.set(emailKey(normalizedEmail), attemptId, ttlSeconds, TimeUnit.SECONDS)
        sendEmailCode(normalizedEmail, attemptId, code)

        return SendVerificationResult(
            attemptId = attemptId,
            expiresInSeconds = ttlSeconds,
        )
    }

    override fun verifyCode(cmd: VerifyCodeCmd): VerifyCodeResult {
        validateAttemptIdFormat(cmd.attemptId)
        validateCodeFormat(cmd.code)
        val key = attemptKey(cmd.attemptId)
        val attempt = loadAttempt(cmd.attemptId)

        if (attempt.verifiedAt != null) {
            return VerifyCodeResult(verified = true, verifiedAt = attempt.verifiedAt)
        }
        if (attempt.tryCount >= properties.maxVerifyTries) {
            throw InvalidParamException("verification attempts exceeded")
        }

        val submittedHash = hashVerificationCode(cmd.code)
        if (!CryptoUtils.constantTimeEquals(submittedHash, attempt.codeHash)) {
            val updatedTryCount = attempt.tryCount + 1
            saveAttemptWithRemainingTtl(key, attempt.copy(tryCount = updatedTryCount))
            if (updatedTryCount >= properties.maxVerifyTries) {
                throw InvalidParamException("verification attempts exceeded")
            }
            throw InvalidParamException("verification code is invalid")
        }

        val verifiedAt = OffsetDateTime.now()
        saveAttemptWithRemainingTtl(
            key = key,
            attempt = attempt.copy(
                codeHash = null,
                verifiedAt = verifiedAt,
            ),
        )
        return VerifyCodeResult(verified = true, verifiedAt = verifiedAt)
    }

    override fun completeRegistration(cmd: CompleteRegistrationCmd): CompleteRegistrationResult {
        validateAttemptIdFormat(cmd.attemptId)
        validateNickname(cmd.nickname)
        validatePassword(cmd.password)

        val attempt = loadAttempt(cmd.attemptId)
        if (attempt.verifiedAt == null) {
            throw InvalidParamException("verification is required before registration")
        }

        if (accountRepo.selectByEmail(attempt.email) != null) {
            throw ConflictException("email has already been registered")
        }

        val passwordHash = requireNotNull(passwordEncoder.encode(cmd.password)) {
            "password hash generation failed"
        }
        val account = try {
            accountRepo.insertAndReturn(
                AccountInsertQuery(
                    email = attempt.email,
                    password = passwordHash,
                    name = cmd.nickname.trim(),
                ),
            )
        } catch (e: DataIntegrityViolationException) {
            throw ConflictException("email has already been registered", e)
        }

        redisClient.delete(attemptKey(cmd.attemptId))
        redisClient.delete(emailKey(attempt.email))
        redisClient.delete(cooldownKey(cmd.attemptId))

        return CompleteRegistrationResult(
            accountId = account.id,
            email = attempt.email,
            nickname = account.name,
        )
    }

    override fun resendVerification(cmd: ResendVerificationCmd): SendVerificationResult {
        validateAttemptIdFormat(cmd.attemptId)
        val attempt = loadAttempt(cmd.attemptId)
        if (attempt.verifiedAt != null) {
            throw InvalidParamException("attempt is already verified")
        }

        val cooldownKey = cooldownKey(cmd.attemptId)
        if (redisClient.hasKey(cooldownKey)) {
            throw InvalidParamException("verification resend is cooling down")
        }

        val code = otcGenerator.next(properties.codeLength)
        val attemptKey = attemptKey(cmd.attemptId)
        saveAttemptWithRemainingTtl(
            key = attemptKey,
            attempt = attempt.copy(
                codeHash = hashVerificationCode(code),
                tryCount = 0,
            ),
        )

        val cooldownSeconds = durationToSeconds(properties.resendCooldown)
        redisClient.set(cooldownKey, "1", cooldownSeconds, TimeUnit.SECONDS)
        sendEmailCode(attempt.email, attempt.attemptId, code)

        return SendVerificationResult(
            attemptId = attempt.attemptId,
            expiresInSeconds = remainingTtlSeconds(attemptKey),
        )
    }

    private fun loadAttempt(attemptId: String): RegisterAttempt =
        redisClient.getObj<RegisterAttempt>(attemptKey(attemptId))
            ?: throw InvalidParamException("invalid or expired registration attempt")

    private fun saveAttemptWithRemainingTtl(key: String, attempt: RegisterAttempt) {
        redisClient.setObj(key, attempt, remainingTtlSeconds(key), TimeUnit.SECONDS)
    }

    private fun remainingTtlSeconds(key: String): Long {
        val ttl = redisClient.getExpire(key, TimeUnit.SECONDS)
            ?: throw InvalidParamException("invalid or expired registration attempt")
        return when {
            ttl == RedisClient.KEY_NO_EXPIRE -> durationToSeconds(properties.attemptLifetime)
            ttl <= 0L -> throw InvalidParamException("invalid or expired registration attempt")
            else -> ttl
        }
    }

    private fun validateAttemptIdFormat(attemptId: String) {
        runCatching { UUID.fromString(attemptId) }
            .getOrElse { throw InvalidParamException("attemptId is not a valid UUID") }
    }

    private fun validateCodeFormat(code: String) {
        if (code.length != properties.codeLength || !code.all { it.isDigit() }) {
            throw InvalidParamException("verification code format is invalid")
        }
    }

    private fun validateNickname(nickname: String) {
        val value = nickname.trim()
        if (value.length !in 2..40) {
            throw InvalidParamException("nickname length must be between 2 and 40")
        }
    }

    private fun validatePassword(password: String) {
        if (password.length < 8) {
            throw InvalidParamException("password must be at least 8 characters")
        }
    }

    private fun normalizeEmail(email: String): String {
        val value = email.trim().lowercase()
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
        if (!emailRegex.matches(value)) {
            throw InvalidParamException("email format is invalid")
        }
        return value
    }

    private fun hashVerificationCode(code: String): String =
        CryptoUtils.sha256String(code, hashSecretBytes)

    private fun durationToSeconds(duration: Duration): Long {
        val seconds = duration.seconds
        return if (seconds <= 0) 1L else seconds
    }

    private fun sendEmailCode(email: String, attemptId: String, code: String) {
        log.info("Register code issued. attemptId={}, email={}", attemptId, maskEmail(email))
        if (code.isBlank()) {
            throw InvalidParamException("verification code generation failed")
        }
        // TODO: integrate email provider.
        // FIXME: DEBUG only
        log.info("code={}", code)
    }

    private fun attemptKey(attemptId: String): String = "register:attempt:$attemptId"

    private fun emailKey(email: String): String {
        val hash = CryptoUtils.sha256String(email, hashSecretBytes)
        return "register:email:$hash"
    }

    private fun cooldownKey(attemptId: String): String = "register:cooldown:$attemptId"

    private fun maskEmail(email: String): String {
        val atIndex = email.indexOf('@')
        if (atIndex <= 1) {
            return "***"
        }
        return "${email.substring(0, 2)}****${email.substring(atIndex)}"
    }

    private companion object {
        private val log = LoggerFactory.getLogger(RegisterServiceImpl::class.java)
    }
}
