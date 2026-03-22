package dev.haomin.resumer.app.auth.service.impl

import dev.haomin.resumer.app.auth.domain.Account
import dev.haomin.resumer.app.auth.domain.AccountRole
import dev.haomin.resumer.app.auth.domain.AccountStatus
import dev.haomin.resumer.app.auth.domain.Token
import dev.haomin.resumer.app.auth.exception.ExpiredAuthenticationException
import dev.haomin.resumer.app.auth.exception.InvalidAuthenticationException
import dev.haomin.resumer.app.auth.principal.TokenPrincipal
import dev.haomin.resumer.app.auth.prop.AccessTokenProperties
import dev.haomin.resumer.app.auth.service.AccessService
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Service
class AccessServiceImpl(
    private val properties: AccessTokenProperties,
) : AccessService {
    private val signingKey: SecretKey = buildSigningKey(properties.secret)

    override fun issue(account: Account): Token {
        val now = OffsetDateTime.now()
        val expiry = now.plus(properties.lifetime)
        val accessToken = Jwts.builder()
            .subject(account.id.toString())
            .claim(CLAIM_EMAIL, account.email)
            .claim(CLAIM_STATUS, account.status.name)
            .claim(CLAIM_ROLE, account.role.name)
            .issuedAt(Date.from(now.toInstant()))
            .expiration(Date.from(expiry.toInstant()))
            .signWith(signingKey)
            .compact()
        return Token(token = accessToken, expiry = expiry)
    }

    override fun parse(accessToken: String): TokenPrincipal =
        try {
            val claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(accessToken)
                .payload

            val id = runCatching { UUID.fromString(claims.subject) }.getOrNull()
                ?: throw InvalidAuthenticationException("invalid access token subject")
            val email = claims[CLAIM_EMAIL] as? String
                ?: throw InvalidAuthenticationException("missing email claim")
            val statusText = claims[CLAIM_STATUS] as? String
                ?: throw InvalidAuthenticationException("missing status claim")
            val roleText = claims[CLAIM_ROLE] as? String
                ?: throw InvalidAuthenticationException("missing role claim")

            TokenPrincipal(
                id = id,
                email = email,
                status = AccountStatus.fromString(statusText),
                role = AccountRole.fromString(roleText),
            )
        } catch (_: ExpiredJwtException) {
            throw ExpiredAuthenticationException("access token has expired")
        } catch (_: JwtException) {
            throw InvalidAuthenticationException("invalid access token")
        } catch (_: IllegalArgumentException) {
            throw InvalidAuthenticationException("invalid access token")
        }

    private fun buildSigningKey(secret: String): SecretKey {
        val keyBytes = runCatching { Decoders.BASE64.decode(secret) }
            .getOrElse { secret.toByteArray(StandardCharsets.UTF_8) }
        return Keys.hmacShaKeyFor(keyBytes)
    }

    private companion object {
        const val CLAIM_EMAIL = "email"
        const val CLAIM_STATUS = "status"
        const val CLAIM_ROLE = "role"
    }
}
