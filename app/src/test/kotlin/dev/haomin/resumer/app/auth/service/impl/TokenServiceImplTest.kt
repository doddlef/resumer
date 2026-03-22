package dev.haomin.resumer.app.auth.service.impl

import dev.haomin.resumer.app.auth.domain.Account
import dev.haomin.resumer.app.auth.domain.AccountRole
import dev.haomin.resumer.app.auth.domain.AccountStatus
import dev.haomin.resumer.app.auth.domain.Token
import dev.haomin.resumer.app.auth.exception.AuthAccountNotFoundException
import dev.haomin.resumer.app.auth.principal.TokenPrincipal
import dev.haomin.resumer.app.auth.repo.AccountRepo
import dev.haomin.resumer.app.auth.service.AccessService
import dev.haomin.resumer.app.auth.service.RefreshService
import dev.haomin.resumer.app.auth.service.dto.RefreshSessionResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.OffsetDateTime
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class TokenServiceImplTest {

    @Mock
    private lateinit var accountRepo: AccountRepo

    @Mock
    private lateinit var accessService: AccessService

    @Mock
    private lateinit var refreshService: RefreshService

    @Test
    fun `login should issue token pair and account profile`() {
        val service = TokenServiceImpl(accountRepo, accessService, refreshService)
        val account = account()
        val accessToken = Token("access-token", OffsetDateTime.now().plusMinutes(30))
        val refreshResult = RefreshSessionResult(
            accountId = account.id,
            refreshToken = "refresh-token",
            expiry = OffsetDateTime.now().plusDays(7),
        )
        whenever(accountRepo.selectById(eq(account.id))).thenReturn(account)
        whenever(accessService.issue(eq(account))).thenReturn(accessToken)
        whenever(refreshService.createSession(eq(account.id))).thenReturn(refreshResult)

        val result = service.login(account.id)

        assertEquals(account.id, result.account.id)
        assertEquals("access-token", result.tokenPair.access.token)
        assertEquals("refresh-token", result.tokenPair.refresh.token)
        verify(accountRepo).selectById(eq(account.id))
        verify(accessService).issue(eq(account))
        verify(refreshService).createSession(eq(account.id))
    }

    @Test
    fun `login should throw when account not found`() {
        val service = TokenServiceImpl(accountRepo, accessService, refreshService)
        val accountId = UUID.randomUUID()
        whenever(accountRepo.selectById(eq(accountId))).thenReturn(null)

        assertThrows(AuthAccountNotFoundException::class.java) {
            service.login(accountId)
        }
        verify(accessService, never()).issue(org.mockito.kotlin.any())
        verify(refreshService, never()).createSession(org.mockito.kotlin.any())
    }

    @Test
    fun `refresh should rotate session and issue new access token`() {
        val service = TokenServiceImpl(accountRepo, accessService, refreshService)
        val account = account()
        val accessToken = Token("new-access", OffsetDateTime.now().plusMinutes(30))
        val refreshResult = RefreshSessionResult(
            accountId = account.id,
            refreshToken = "new-refresh",
            expiry = OffsetDateTime.now().plusDays(7),
        )
        whenever(refreshService.rotateSession(eq("old-refresh"))).thenReturn(refreshResult)
        whenever(accountRepo.selectById(eq(account.id))).thenReturn(account)
        whenever(accessService.issue(eq(account))).thenReturn(accessToken)

        val result = service.refresh("old-refresh")

        assertEquals("new-access", result.tokenPair.access.token)
        assertEquals("new-refresh", result.tokenPair.refresh.token)
        assertEquals(account.id, result.account.id)
        verify(refreshService).rotateSession(eq("old-refresh"))
        verify(accountRepo).selectById(eq(account.id))
        verify(accessService).issue(eq(account))
    }

    @Test
    fun `refresh should throw when account not found`() {
        val service = TokenServiceImpl(accountRepo, accessService, refreshService)
        val accountId = UUID.randomUUID()
        whenever(refreshService.rotateSession(eq("refresh"))).thenReturn(
            RefreshSessionResult(
                accountId = accountId,
                refreshToken = "new-refresh",
                expiry = OffsetDateTime.now().plusDays(7),
            ),
        )
        whenever(accountRepo.selectById(eq(accountId))).thenReturn(null)

        assertThrows(AuthAccountNotFoundException::class.java) {
            service.refresh("refresh")
        }
        verify(accessService, never()).issue(org.mockito.kotlin.any())
    }

    @Test
    fun `access should delegate to access service parse`() {
        val service = TokenServiceImpl(accountRepo, accessService, refreshService)
        val principal = TokenPrincipal(
            id = UUID.randomUUID(),
            email = "student@example.com",
            status = AccountStatus.ACTIVE,
            role = AccountRole.USER,
        )
        whenever(accessService.parse(eq("access-token"))).thenReturn(principal)

        val result = service.access("access-token")

        assertEquals(principal, result)
        verify(accessService).parse(eq("access-token"))
    }

    @Test
    fun `logout should delegate revoke with logout reason`() {
        val service = TokenServiceImpl(accountRepo, accessService, refreshService)
        whenever(refreshService.revokeSession(eq("refresh-token"), eq("logout"))).thenReturn(true)

        val result = service.logout("refresh-token")

        assertTrue(result)
        verify(refreshService).revokeSession(eq("refresh-token"), eq("logout"))
    }

    @Test
    fun `logout should return false when revoke returns false`() {
        val service = TokenServiceImpl(accountRepo, accessService, refreshService)
        whenever(refreshService.revokeSession(eq("refresh-token"), eq("logout"))).thenReturn(false)

        val result = service.logout("refresh-token")

        assertFalse(result)
        verify(refreshService).revokeSession(eq("refresh-token"), eq("logout"))
    }

    private fun account(): Account =
        Account(
            id = UUID.randomUUID(),
            email = "student@example.com",
            password = "hashed-password",
            name = "Student",
            status = AccountStatus.ACTIVE,
            role = AccountRole.USER,
            createdAt = OffsetDateTime.now().minusDays(1),
            updatedAt = OffsetDateTime.now(),
        )
}
