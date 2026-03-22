package dev.haomin.resumer.app.auth.service.impl

import dev.haomin.resumer.app.auth.domain.RefreshSession
import dev.haomin.resumer.app.auth.exception.ExpiredAuthenticationException
import dev.haomin.resumer.app.auth.exception.InvalidAuthenticationException
import dev.haomin.resumer.app.auth.prop.RefreshTokenProperties
import dev.haomin.resumer.app.auth.repo.RefreshSessionRepo
import dev.haomin.resumer.app.auth.repo.query.RefreshSessionInsertQuery
import dev.haomin.resumer.app.auth.repo.query.RefreshSessionUpdateQuery
import dev.haomin.resumer.app.common.security.CryptoUtils
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Duration
import java.time.OffsetDateTime
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class RefreshServiceImplTest {

    @Mock
    private lateinit var refreshSessionRepo: RefreshSessionRepo

    private val refreshTokenProperties = RefreshTokenProperties(
        secret = "test-refresh-secret",
        idleLifetime = Duration.ofDays(7),
        maxLifetime = Duration.ofDays(120),
        graceTime = Duration.ofSeconds(5),
    )

    private lateinit var refreshService: RefreshServiceImpl

    @BeforeEach
    fun setUp() {
        refreshService = RefreshServiceImpl(
            sessionRepo = refreshSessionRepo,
            properties = refreshTokenProperties,
        )
    }

    @Test
    fun `createSession inserts row and returns refresh token payload`() {
        val accountId = UUID.randomUUID()
        whenever(refreshSessionRepo.insert(any())).thenReturn(1)

        val result = refreshService.createSession(accountId)

        val insertCaptor = argumentCaptor<RefreshSessionInsertQuery>()
        verify(refreshSessionRepo, times(1)).insert(insertCaptor.capture())
        val inserted = insertCaptor.firstValue
        val (sessionIdFromToken, rawSecretFromToken) = parseRefreshToken(result.refreshToken)
        val expectedHash = CryptoUtils.sha256String(
            rawSecretFromToken,
            CryptoUtils.decodeString(refreshTokenProperties.secret),
        )

        assertEquals(accountId, result.accountId)
        assertEquals(inserted.id, sessionIdFromToken)
        assertEquals(accountId, inserted.accountId)
        assertEquals(expectedHash, inserted.secret)
        assertTrue(result.expiry.isAfter(OffsetDateTime.now().minusSeconds(1)))
    }

    @Test
    fun `rotateSession rotates when incoming token matches current secret`() {
        val sessionId = UUID.randomUUID()
        val accountId = UUID.randomUUID()
        val oldSecret = "old-secret"
        val now = OffsetDateTime.now()
        val session = RefreshSession(
            id = sessionId,
            accountId = accountId,
            secret = hash(oldSecret),
            prevSecret = null,
            lastUsedAt = now.minusHours(1),
            revokedAt = null,
            revokeReason = null,
            createdAt = now.minusDays(1),
            updatedAt = now.minusHours(1),
        )
        whenever(refreshSessionRepo.selectByIdForUpdate(eq(sessionId))).thenReturn(session)
        whenever(refreshSessionRepo.updateById(eq(sessionId), any())).thenReturn(1)

        val result = refreshService.rotateSession("$sessionId.$oldSecret")

        val updateCaptor = argumentCaptor<RefreshSessionUpdateQuery>()
        verify(refreshSessionRepo, times(1)).updateById(eq(sessionId), updateCaptor.capture())
        val update = updateCaptor.firstValue
        val (rotatedSessionId, rotatedSecret) = parseRefreshToken(result.refreshToken)

        assertEquals(accountId, result.accountId)
        assertEquals(sessionId, rotatedSessionId)
        assertNotNull(update.lastUsedAt)
        assertEquals(session.secret, update.prevSecret)
        assertNotNull(update.secret)
        assertNotEquals(oldSecret, rotatedSecret)
    }

    @Test
    fun `rotateSession accepts previous secret within grace window`() {
        val sessionId = UUID.randomUUID()
        val accountId = UUID.randomUUID()
        val incomingSecret = "incoming-secret"
        val session = RefreshSession(
            id = sessionId,
            accountId = accountId,
            secret = hash("current-secret"),
            prevSecret = hash(incomingSecret),
            lastUsedAt = OffsetDateTime.now(),
            revokedAt = null,
            revokeReason = null,
            createdAt = OffsetDateTime.now().minusDays(1),
            updatedAt = OffsetDateTime.now(),
        )
        whenever(refreshSessionRepo.selectByIdForUpdate(eq(sessionId))).thenReturn(session)
        whenever(refreshSessionRepo.updateById(eq(sessionId), any())).thenReturn(1)

        assertDoesNotThrow {
            refreshService.rotateSession("$sessionId.$incomingSecret")
        }
        verify(refreshSessionRepo, times(1)).updateById(eq(sessionId), any())
    }

    @Test
    fun `rotateSession revokes and throws when session expired`() {
        val sessionId = UUID.randomUUID()
        val now = OffsetDateTime.now()
        val session = RefreshSession(
            id = sessionId,
            accountId = UUID.randomUUID(),
            secret = hash("old-secret"),
            prevSecret = null,
            lastUsedAt = now.minusDays(1),
            revokedAt = null,
            revokeReason = null,
            createdAt = now.minusDays(121),
            updatedAt = now.minusDays(1),
        )
        whenever(refreshSessionRepo.selectByIdForUpdate(eq(sessionId))).thenReturn(session)
        whenever(refreshSessionRepo.updateById(eq(sessionId), any())).thenReturn(1)

        assertThrows(ExpiredAuthenticationException::class.java) {
            refreshService.rotateSession("$sessionId.old-secret")
        }
        val updateCaptor = argumentCaptor<RefreshSessionUpdateQuery>()
        verify(refreshSessionRepo, times(1)).updateById(eq(sessionId), updateCaptor.capture())
        val revokeQuery = updateCaptor.firstValue
        assertEquals("session_expired", revokeQuery.revokeReason)
        assertNotNull(revokeQuery.revokedAt)
    }

    @Test
    fun `rotateSession should revoke and reject reused previous token outside grace window`() {
        val sessionId = UUID.randomUUID()
        val now = OffsetDateTime.now()
        val reusedSecret = "reused-secret"
        val session = RefreshSession(
            id = sessionId,
            accountId = UUID.randomUUID(),
            secret = hash("current-secret"),
            prevSecret = hash(reusedSecret),
            lastUsedAt = now.minusMinutes(1),
            revokedAt = null,
            revokeReason = null,
            createdAt = now.minusDays(1),
            updatedAt = now.minusMinutes(1),
        )
        whenever(refreshSessionRepo.selectByIdForUpdate(eq(sessionId))).thenReturn(session)
        whenever(refreshSessionRepo.updateById(eq(sessionId), any())).thenReturn(1)

        assertThrows(InvalidAuthenticationException::class.java) {
            refreshService.rotateSession("$sessionId.$reusedSecret")
        }
        val updateCaptor = argumentCaptor<RefreshSessionUpdateQuery>()
        verify(refreshSessionRepo, times(1)).updateById(eq(sessionId), updateCaptor.capture())
        assertEquals("refresh_token_reuse_detected", updateCaptor.firstValue.revokeReason)
        assertNotNull(updateCaptor.firstValue.revokedAt)
    }

    @Test
    fun `revokeSession returns false and does not update when token secret mismatches`() {
        val sessionId = UUID.randomUUID()
        val session = RefreshSession(
            id = sessionId,
            accountId = UUID.randomUUID(),
            secret = hash("correct"),
            prevSecret = null,
            lastUsedAt = OffsetDateTime.now(),
            revokedAt = null,
            revokeReason = null,
            createdAt = OffsetDateTime.now().minusDays(1),
            updatedAt = OffsetDateTime.now(),
        )
        whenever(refreshSessionRepo.selectByIdForUpdate(eq(sessionId))).thenReturn(session)

        val revoked = refreshService.revokeSession("$sessionId.wrong", "logout")

        assertFalse(revoked)
        verify(refreshSessionRepo, never()).updateById(any(), any())
    }

    @Test
    fun `revokeSession updates session when token matches current secret`() {
        val sessionId = UUID.randomUUID()
        val secret = "correct"
        val session = RefreshSession(
            id = sessionId,
            accountId = UUID.randomUUID(),
            secret = hash(secret),
            prevSecret = null,
            lastUsedAt = OffsetDateTime.now(),
            revokedAt = null,
            revokeReason = null,
            createdAt = OffsetDateTime.now().minusDays(1),
            updatedAt = OffsetDateTime.now(),
        )
        whenever(refreshSessionRepo.selectByIdForUpdate(eq(sessionId))).thenReturn(session)
        whenever(refreshSessionRepo.updateById(eq(sessionId), any())).thenReturn(1)

        val revoked = refreshService.revokeSession("$sessionId.$secret", "logout")

        assertTrue(revoked)
        val updateCaptor = argumentCaptor<RefreshSessionUpdateQuery>()
        verify(refreshSessionRepo, times(1)).updateById(eq(sessionId), updateCaptor.capture())
        assertEquals("logout", updateCaptor.firstValue.revokeReason)
    }

    @Test
    fun `rotateSession rejects unknown session`() {
        val sessionId = UUID.randomUUID()
        whenever(refreshSessionRepo.selectByIdForUpdate(eq(sessionId))).thenReturn(null)

        assertThrows(InvalidAuthenticationException::class.java) {
            refreshService.rotateSession("$sessionId.any-secret")
        }
    }

    private fun hash(secret: String): String =
        CryptoUtils.sha256String(secret, CryptoUtils.decodeString(refreshTokenProperties.secret))

    private fun parseRefreshToken(token: String): Pair<UUID, String> {
        val dot = token.indexOf('.')
        val sessionId = UUID.fromString(token.substring(0, dot))
        val secret = token.substring(dot + 1)
        return sessionId to secret
    }
}
