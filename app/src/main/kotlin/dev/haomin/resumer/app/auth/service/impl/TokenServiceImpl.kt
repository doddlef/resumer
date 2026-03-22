package dev.haomin.resumer.app.auth.service.impl

import dev.haomin.resumer.app.auth.domain.Token
import dev.haomin.resumer.app.auth.domain.TokenPair
import dev.haomin.resumer.app.auth.exception.AuthAccountNotFoundException
import dev.haomin.resumer.app.auth.principal.TokenPrincipal
import dev.haomin.resumer.app.auth.repo.AccountRepo
import dev.haomin.resumer.app.auth.service.AccessService
import dev.haomin.resumer.app.auth.service.RefreshService
import dev.haomin.resumer.app.auth.service.TokenService
import dev.haomin.resumer.app.auth.service.dto.TokenIssueResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TokenServiceImpl(
    private val accountRepo: AccountRepo,
    private val accessService: AccessService,
    private val refreshService: RefreshService,
) : TokenService {

    override fun login(accountId: UUID): TokenIssueResult {
        val account = accountRepo.selectById(accountId) ?: throw AuthAccountNotFoundException()
        val access = accessService.issue(account)
        val refresh = refreshService.createSession(account.id)
        log.info("Login tokens issued. accountId={}", account.id)
        return TokenIssueResult(
            tokenPair = TokenPair(
                access = access,
                refresh = Token(token = refresh.refreshToken, expiry = refresh.expiry),
            ),
            account = account.asProfile(),
        )
    }

    override fun refresh(refreshToken: String): TokenIssueResult {
        val refreshResult = refreshService.rotateSession(refreshToken)
        val account = accountRepo.selectById(refreshResult.accountId)
            ?: throw AuthAccountNotFoundException()
        val access = accessService.issue(account)
        log.info("Tokens refreshed. accountId={}", account.id)
        return TokenIssueResult(
            tokenPair = TokenPair(
                access = access,
                refresh = Token(token = refreshResult.refreshToken, expiry = refreshResult.expiry),
            ),
            account = account.asProfile(),
        )
    }

    override fun access(accessToken: String): TokenPrincipal =
        accessService.parse(accessToken)

    override fun logout(refreshToken: String): Boolean =
        refreshService.revokeSession(refreshToken, reason = "logout")

    private companion object {
        private val log = LoggerFactory.getLogger(TokenServiceImpl::class.java)
    }
}
