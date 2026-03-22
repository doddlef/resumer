package dev.haomin.resumer.app.auth.service.impl

import dev.haomin.resumer.app.auth.domain.Account
import dev.haomin.resumer.app.auth.domain.AccountRole
import dev.haomin.resumer.app.auth.domain.AccountStatus
import dev.haomin.resumer.app.auth.exception.ExpiredAuthenticationException
import dev.haomin.resumer.app.auth.exception.InvalidAuthenticationException
import dev.haomin.resumer.app.auth.prop.AccessTokenProperties
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.OffsetDateTime
import java.util.Date
import java.util.UUID

class AccessServiceImplTest {

    private val secret = "fLqXLI7VVFXexFdAiJTSLhhSjuUHG3VyGNuppEwFlndNzMZihjyyNot85YRwSGWTYN6N32zRozGeb4Qd+ZWURQ=="
    private val service = AccessServiceImpl(
        properties = AccessTokenProperties(
            secret = secret,
            lifetime = Duration.ofMinutes(30),
        ),
    )

    @Test
    fun `issue and parse should produce consistent principal`() {
        val account = Account(
            id = UUID.randomUUID(),
            email = "student@example.com",
            password = "hashed-password",
            name = "Student",
            status = AccountStatus.ACTIVE,
            role = AccountRole.USER,
            createdAt = OffsetDateTime.now().minusDays(1),
            updatedAt = OffsetDateTime.now(),
        )

        val issued = service.issue(account)
        val principal = service.parse(issued.token)

        assertEquals(account.id, principal.id)
        assertEquals(account.email, principal.email)
        assertEquals(account.status, principal.status)
        assertEquals(account.role, principal.role)
        assertTrue(issued.expiry.isAfter(OffsetDateTime.now()))
    }

    @Test
    fun `parse should throw invalid auth for malformed token`() {
        assertThrows(InvalidAuthenticationException::class.java) {
            service.parse("not-a-jwt")
        }
    }

    @Test
    fun `parse should throw expired auth for expired token`() {
        val key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))
        val now = OffsetDateTime.now()
        val expiredToken = Jwts.builder()
            .subject(UUID.randomUUID().toString())
            .claim("email", "student@example.com")
            .claim("status", "ACTIVE")
            .claim("role", "USER")
            .issuedAt(Date.from(now.minusMinutes(10).toInstant()))
            .expiration(Date.from(now.minusMinutes(1).toInstant()))
            .signWith(key)
            .compact()

        assertThrows(ExpiredAuthenticationException::class.java) {
            service.parse(expiredToken)
        }
    }
}
