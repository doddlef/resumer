package dev.haomin.resumer.app.auth.service.impl

import dev.haomin.resumer.app.auth.domain.Account
import dev.haomin.resumer.app.auth.domain.AccountRole
import dev.haomin.resumer.app.auth.domain.AccountStatus
import dev.haomin.resumer.app.auth.prop.RegisterProperties
import dev.haomin.resumer.app.auth.repo.AccountRepo
import dev.haomin.resumer.app.auth.repo.query.AccountInsertQuery
import dev.haomin.resumer.app.auth.service.dto.CompleteRegistrationCmd
import dev.haomin.resumer.app.auth.service.dto.ResendVerificationCmd
import dev.haomin.resumer.app.auth.service.dto.SendVerificationCmd
import dev.haomin.resumer.app.auth.service.dto.VerifyCodeCmd
import dev.haomin.resumer.app.auth.service.model.RegisterAttempt
import dev.haomin.resumer.app.common.exception.InvalidParamException
import dev.haomin.resumer.app.common.id.OTCGenerator
import dev.haomin.resumer.app.common.security.CryptoUtils
import dev.haomin.resumer.app.framework.redis.RedisClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Duration
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit

@ExtendWith(MockitoExtension::class)
class RegisterServiceImplTest {

    @Mock
    private lateinit var accountRepo: AccountRepo

    @Mock
    private lateinit var redisClient: RedisClient

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @Mock
    private lateinit var otcGenerator: OTCGenerator

    private val registerProperties = RegisterProperties(
        codeHashSecret = "test-register-secret",
        codeLength = 6,
        maxVerifyTries = 5,
        attemptLifetime = Duration.ofMinutes(15),
        resendCooldown = Duration.ofSeconds(60),
    )

    private lateinit var service: RegisterServiceImpl

    @BeforeEach
    fun setUp() {
        service = RegisterServiceImpl(
            accountRepo = accountRepo,
            redisClient = redisClient,
            passwordEncoder = passwordEncoder,
            properties = registerProperties,
            otcGenerator = otcGenerator,
        )
    }

    @Test
    fun `sendVerificationEmail should create attempt when not exists`() {
        val normalizedEmail = "student@example.com"
        whenever(redisClient.get(eq(emailKey(normalizedEmail)))).thenReturn(null)
        whenever(otcGenerator.next(eq(registerProperties.codeLength))).thenReturn("123456")

        val result = service.sendVerificationEmail(SendVerificationCmd(" Student@Example.com "))

        assertTrue(result.attemptId.isNotBlank())
        assertEquals(registerProperties.attemptLifetime.seconds, result.expiresInSeconds)
        verify(redisClient, times(1)).setObj(
            eq(attemptKey(result.attemptId)),
            any(),
            eq(registerProperties.attemptLifetime.seconds),
            eq(TimeUnit.SECONDS),
        )
        verify(redisClient, times(1)).set(
            eq(emailKey(normalizedEmail)),
            eq(result.attemptId),
            eq(registerProperties.attemptLifetime.seconds),
            eq(TimeUnit.SECONDS),
        )
    }

    @Test
    fun `sendVerificationEmail should return existing attempt when exists in redis`() {
        val attemptId = UUID.randomUUID().toString()
        val normalizedEmail = "student@example.com"
        val attempt = RegisterAttempt(
            attemptId = attemptId,
            email = normalizedEmail,
            codeHash = "hashed",
            verifiedAt = null,
            tryCount = 0,
            createdAt = OffsetDateTime.now(),
        )
        whenever(redisClient.get(eq(emailKey(normalizedEmail)))).thenReturn(attemptId)
        whenever(redisClient.getObj(eq(attemptKey(attemptId)), eq(RegisterAttempt::class.java))).thenReturn(attempt)
        whenever(redisClient.getExpire(eq(attemptKey(attemptId)), eq(TimeUnit.SECONDS))).thenReturn(120L)

        val result = service.sendVerificationEmail(SendVerificationCmd(normalizedEmail))

        assertEquals(attemptId, result.attemptId)
        assertEquals(120L, result.expiresInSeconds)
        verify(otcGenerator, never()).next(any())
    }

    @Test
    fun `verifyCode should mark attempt verified when code is correct`() {
        val attemptId = UUID.randomUUID().toString()
        val code = "123456"
        val attempt = RegisterAttempt(
            attemptId = attemptId,
            email = "student@example.com",
            codeHash = codeHash(code),
            verifiedAt = null,
            tryCount = 1,
            createdAt = OffsetDateTime.now(),
        )
        whenever(redisClient.getObj(eq(attemptKey(attemptId)), eq(RegisterAttempt::class.java))).thenReturn(attempt)
        whenever(redisClient.getExpire(eq(attemptKey(attemptId)), eq(TimeUnit.SECONDS))).thenReturn(300L)

        val result = service.verifyCode(VerifyCodeCmd(attemptId, code))

        assertTrue(result.verified)
        assertNotNull(result.verifiedAt)
        val attemptCaptor = argumentCaptor<RegisterAttempt>()
        verify(redisClient).setObj(eq(attemptKey(attemptId)), attemptCaptor.capture(), eq(300L), eq(TimeUnit.SECONDS))
        assertNull(attemptCaptor.firstValue.codeHash)
        assertNotNull(attemptCaptor.firstValue.verifiedAt)
    }

    @Test
    fun `verifyCode should increment try count when code is wrong`() {
        val attemptId = UUID.randomUUID().toString()
        val attempt = RegisterAttempt(
            attemptId = attemptId,
            email = "student@example.com",
            codeHash = codeHash("123456"),
            verifiedAt = null,
            tryCount = 1,
            createdAt = OffsetDateTime.now(),
        )
        whenever(redisClient.getObj(eq(attemptKey(attemptId)), eq(RegisterAttempt::class.java))).thenReturn(attempt)
        whenever(redisClient.getExpire(eq(attemptKey(attemptId)), eq(TimeUnit.SECONDS))).thenReturn(240L)

        assertThrows(InvalidParamException::class.java) {
            service.verifyCode(VerifyCodeCmd(attemptId, "654321"))
        }

        val attemptCaptor = argumentCaptor<RegisterAttempt>()
        verify(redisClient).setObj(eq(attemptKey(attemptId)), attemptCaptor.capture(), eq(240L), eq(TimeUnit.SECONDS))
        assertEquals(2, attemptCaptor.firstValue.tryCount)
    }

    @Test
    fun `verifyCode should throw when max tries already exceeded`() {
        val attemptId = UUID.randomUUID().toString()
        val attempt = RegisterAttempt(
            attemptId = attemptId,
            email = "student@example.com",
            codeHash = codeHash("123456"),
            verifiedAt = null,
            tryCount = registerProperties.maxVerifyTries,
            createdAt = OffsetDateTime.now(),
        )
        whenever(redisClient.getObj(eq(attemptKey(attemptId)), eq(RegisterAttempt::class.java))).thenReturn(attempt)

        assertThrows(InvalidParamException::class.java) {
            service.verifyCode(VerifyCodeCmd(attemptId, "123456"))
        }
        verify(redisClient, never()).setObj(any(), any(), any(), any())
    }

    @Test
    fun `completeRegistration should create account and cleanup redis keys`() {
        val attemptId = UUID.randomUUID().toString()
        val email = "student@example.com"
        val attempt = RegisterAttempt(
            attemptId = attemptId,
            email = email,
            codeHash = null,
            verifiedAt = OffsetDateTime.now().minusMinutes(1),
            tryCount = 0,
            createdAt = OffsetDateTime.now().minusMinutes(2),
        )
        val account = Account(
            id = UUID.randomUUID(),
            email = email,
            password = "hashed-password",
            name = "Student",
            status = AccountStatus.ACTIVE,
            role = AccountRole.USER,
            createdAt = OffsetDateTime.now().minusMinutes(1),
            updatedAt = OffsetDateTime.now(),
        )
        whenever(redisClient.getObj(eq(attemptKey(attemptId)), eq(RegisterAttempt::class.java))).thenReturn(attempt)
        whenever(accountRepo.selectByEmail(eq(email))).thenReturn(null)
        whenever(passwordEncoder.encode(eq("password-123"))).thenReturn("encoded-password")
        whenever(accountRepo.insertAndReturn(any())).thenReturn(account)

        val result = service.completeRegistration(
            CompleteRegistrationCmd(
                attemptId = attemptId,
                nickname = "Student",
                password = "password-123",
            ),
        )

        assertEquals(account.id, result.accountId)
        assertEquals(email, result.email)
        assertEquals("Student", result.nickname)

        val queryCaptor = argumentCaptor<AccountInsertQuery>()
        verify(accountRepo).insertAndReturn(queryCaptor.capture())
        assertEquals(email, queryCaptor.firstValue.email)
        assertEquals("encoded-password", queryCaptor.firstValue.password)
        assertEquals("Student", queryCaptor.firstValue.name)

        verify(redisClient).delete(eq(attemptKey(attemptId)))
        verify(redisClient).delete(eq(emailKey(email)))
        verify(redisClient).delete(eq(cooldownKey(attemptId)))
    }

    @Test
    fun `completeRegistration should reject when attempt is not verified`() {
        val attemptId = UUID.randomUUID().toString()
        val attempt = RegisterAttempt(
            attemptId = attemptId,
            email = "student@example.com",
            codeHash = codeHash("123456"),
            verifiedAt = null,
            tryCount = 0,
            createdAt = OffsetDateTime.now(),
        )
        whenever(redisClient.getObj(eq(attemptKey(attemptId)), eq(RegisterAttempt::class.java))).thenReturn(attempt)

        assertThrows(InvalidParamException::class.java) {
            service.completeRegistration(
                CompleteRegistrationCmd(
                    attemptId = attemptId,
                    nickname = "Student",
                    password = "password-123",
                ),
            )
        }
        verify(accountRepo, never()).insertAndReturn(any())
    }

    @Test
    fun `resendVerification should throw when cooldown exists`() {
        val attemptId = UUID.randomUUID().toString()
        val attempt = RegisterAttempt(
            attemptId = attemptId,
            email = "student@example.com",
            codeHash = "hashed",
            verifiedAt = null,
            tryCount = 0,
            createdAt = OffsetDateTime.now(),
        )
        whenever(redisClient.getObj(eq(attemptKey(attemptId)), eq(RegisterAttempt::class.java))).thenReturn(attempt)
        whenever(redisClient.hasKey(eq(cooldownKey(attemptId)))).thenReturn(true)

        assertThrows(InvalidParamException::class.java) {
            service.resendVerification(ResendVerificationCmd(attemptId))
        }
        verify(otcGenerator, never()).next(any())
    }

    @Test
    fun `verifyCode should reject invalid attempt id format`() {
        assertThrows(InvalidParamException::class.java) {
            service.verifyCode(VerifyCodeCmd("bad-attempt-id", "123456"))
        }
        verify(redisClient, never()).getObj(any(), eq(RegisterAttempt::class.java))
    }

    private fun codeHash(code: String): String =
        CryptoUtils.sha256String(code, CryptoUtils.decodeString(registerProperties.codeHashSecret))

    private fun attemptKey(attemptId: String): String = "register:attempt:$attemptId"

    private fun cooldownKey(attemptId: String): String = "register:cooldown:$attemptId"

    private fun emailKey(email: String): String {
        val normalized = email.trim().lowercase()
        val hash = CryptoUtils.sha256String(normalized, CryptoUtils.decodeString(registerProperties.codeHashSecret))
        return "register:email:$hash"
    }
}
