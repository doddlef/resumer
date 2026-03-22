package dev.haomin.resumer.app.auth.service.impl

import dev.haomin.resumer.app.auth.domain.RefreshSession
import dev.haomin.resumer.app.auth.exception.ExpiredAuthenticationException
import dev.haomin.resumer.app.auth.exception.InvalidAuthenticationException
import dev.haomin.resumer.app.auth.prop.RefreshTokenProperties
import dev.haomin.resumer.app.auth.repo.RefreshSessionRepo
import dev.haomin.resumer.app.auth.repo.query.RefreshSessionInsertQuery
import dev.haomin.resumer.app.auth.repo.query.RefreshSessionUpdateQuery
import dev.haomin.resumer.app.auth.service.RefreshService
import dev.haomin.resumer.app.auth.service.dto.RefreshSessionResult
import dev.haomin.resumer.app.common.exception.DatabaseOperationException
import dev.haomin.resumer.app.common.id.UUIDGenerator
import dev.haomin.resumer.app.common.security.CryptoUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.OffsetDateTime
import java.util.Base64
import java.util.UUID

/**
 * Service implementation responsible for managing refresh sessions and tokens.
 * This class provides methods to create, rotate, and revoke refresh sessions, ensuring secure
 * and efficient handling of refresh tokens in accordance with business and security requirements.
 *
 * @constructor Initializes the service with required dependencies.
 * @param sessionRepo Repository for persistence operations related to refresh sessions.
 * @param properties Configuration properties for refresh tokens, such as lifetimes and secret values.
 */
@Service
class RefreshServiceImpl(
    private val sessionRepo: RefreshSessionRepo,
    private val properties: RefreshTokenProperties,
) : RefreshService {
    private val random = SecureRandom()
    private val pepperBytes = CryptoUtils.decodeString(properties.secret)

    override fun createSession(accountId: UUID): RefreshSessionResult {
        val sessionId = UUIDGenerator.next()
        val refreshSecret = generateSecret()
        val now = OffsetDateTime.now()

        val inserted = sessionRepo.insert(
            RefreshSessionInsertQuery(
                id = sessionId,
                accountId = accountId,
                secret = hashSecret(refreshSecret),
                createdAt = now,
                updatedAt = now,
            ),
        )
        if (inserted != 1) {
            throw DatabaseOperationException("failed to create refresh session")
        }

        log.info("Refresh session created. sessionId={}, accountId={}", sessionId, accountId)
        return RefreshSessionResult(
            accountId = accountId,
            refreshToken = buildRefreshToken(sessionId, refreshSecret),
            expiry = computeExpiry(createdAt = now, lastUsedAt = now),
        )
    }

    @Transactional(noRollbackFor = [InvalidAuthenticationException::class])
    override fun rotateSession(refreshToken: String): RefreshSessionResult {
        val parsed = parseRefreshToken(refreshToken)
        val session = sessionRepo.selectByIdForUpdate(parsed.sessionId)
            ?: throw InvalidAuthenticationException("invalid refresh token")
        val now = OffsetDateTime.now()

        if (session.isRevoked) {
            throw InvalidAuthenticationException("refresh session has been revoked")
        }
        if (isSessionExpired(session, now)) {
            revokeSessionById(session.id, "session_expired", now)
            throw ExpiredAuthenticationException("refresh session has expired")
        }

        val incomingHash = hashSecret(parsed.secret)
        if (CryptoUtils.constantTimeEquals(incomingHash, session.secret)) {
            return rotateSessionToken(session, now)
        }

        if (session.prevSecret != null && CryptoUtils.constantTimeEquals(incomingHash, session.prevSecret)) {
            val graceStartAt = session.idleBaseAt
            val withinGraceWindow = now <= graceStartAt.plus(properties.graceTime)
            if (withinGraceWindow) {
                return rotateSessionToken(session, now)
            }
        }

        revokeSessionById(session.id, "refresh_token_reuse_detected", now)
        throw InvalidAuthenticationException("refresh token reuse detected")
    }

    @Transactional
    override fun revokeSession(refreshToken: String, reason: String): Boolean {
        val parsed = parseRefreshTokenOrNull(refreshToken) ?: return false
        val session = sessionRepo.selectByIdForUpdate(parsed.sessionId) ?: return false
        if (session.isRevoked) {
            return false
        }

        val incomingHash = hashSecret(parsed.secret)
        val matchesKnownToken = CryptoUtils.constantTimeEquals(incomingHash, session.secret) ||
            (session.prevSecret != null && CryptoUtils.constantTimeEquals(incomingHash, session.prevSecret))
        if (!matchesKnownToken) {
            log.warn("Revoke rejected due to token mismatch. sessionId={}", session.id)
            return false
        }

        revokeSessionById(session.id, reason, OffsetDateTime.now())
        log.info("Refresh session revoked. sessionId={}, reason={}", session.id, reason)
        return true
    }

    private fun rotateSessionToken(session: RefreshSession, now: OffsetDateTime): RefreshSessionResult {
        val newSecret = generateSecret()
        val updated = sessionRepo.updateById(
            id = session.id,
            query = RefreshSessionUpdateQuery(
                secret = hashSecret(newSecret),
                prevSecret = session.secret,
                lastUsedAt = now,
                updatedAt = now,
            ),
        )
        if (updated != 1) {
            throw DatabaseOperationException("failed to rotate refresh session")
        }

        return RefreshSessionResult(
            accountId = session.accountId,
            refreshToken = buildRefreshToken(session.id, newSecret),
            expiry = computeExpiry(createdAt = session.createdAt, lastUsedAt = now),
        )
    }

    private fun revokeSessionById(id: UUID, reason: String, now: OffsetDateTime) {
        val updated = sessionRepo.updateById(
            id = id,
            query = RefreshSessionUpdateQuery(
                revokedAt = now,
                revokeReason = reason,
                updatedAt = now,
            ),
        )
        if (updated != 1) {
            throw DatabaseOperationException("failed to revoke refresh session")
        }
    }

    private fun isSessionExpired(session: RefreshSession, now: OffsetDateTime): Boolean {
        val idleExpiry = session.idleBaseAt.plus(properties.idleLifetime)
        val absoluteExpiry = session.createdAt.plus(properties.maxLifetime)
        return now >= idleExpiry || now >= absoluteExpiry
    }

    private fun computeExpiry(createdAt: OffsetDateTime, lastUsedAt: OffsetDateTime): OffsetDateTime {
        val idleExpiry = lastUsedAt.plus(properties.idleLifetime)
        val absoluteExpiry = createdAt.plus(properties.maxLifetime)
        return if (idleExpiry <= absoluteExpiry) idleExpiry else absoluteExpiry
    }

    private fun generateSecret(): String {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun hashSecret(secret: String): String =
        CryptoUtils.sha256String(secret, pepperBytes)

    private fun buildRefreshToken(sessionId: UUID, secret: String): String =
        "$sessionId.$secret"

    private fun parseRefreshToken(token: String): ParsedRefreshToken =
        parseRefreshTokenOrNull(token) ?: throw InvalidAuthenticationException("invalid refresh token format")

    private fun parseRefreshTokenOrNull(token: String): ParsedRefreshToken? {
        val dotIndex = token.indexOf('.')
        if (dotIndex <= 0 || dotIndex == token.lastIndex) {
            return null
        }

        val sessionId = runCatching { UUID.fromString(token.substring(0, dotIndex)) }.getOrNull() ?: return null
        val secret = token.substring(dotIndex + 1)
        if (secret.isBlank()) {
            return null
        }
        return ParsedRefreshToken(sessionId = sessionId, secret = secret)
    }

    private companion object {
        private val log = LoggerFactory.getLogger(RefreshServiceImpl::class.java)
    }
}

private data class ParsedRefreshToken(
    val sessionId: UUID,
    val secret: String,
)
