package dev.haomin.resumer.app.auth.api

import dev.haomin.resumer.app.TestcontainersConfiguration
import dev.haomin.resumer.app.auth.REFRESH_TOKEN_COOKIE
import dev.haomin.resumer.app.auth.repo.AccountRepo
import dev.haomin.resumer.app.auth.repo.query.AccountInsertQuery
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import jakarta.servlet.http.Cookie
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@Import(TestcontainersConfiguration::class)
class AuthControllerDocumentationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val accountRepo: AccountRepo,
    private val passwordEncoder: PasswordEncoder,
) {
    @Test
    fun `auth login refresh logout should work and generate docs`() {
        val email = "auth-${UUID.randomUUID()}@example.com"
        val password = "Password123!"
        val passwordHash = requireNotNull(passwordEncoder.encode(password)) { "password encode failed" }
        accountRepo.insertAndReturn(
            AccountInsertQuery(
                email = email,
                password = passwordHash,
                name = "AuthUser",
            ),
        )

        val loginResult = mockMvc.perform(
            post("/api/auth")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "$email",
                      "password": "$password"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.payload.account.id").exists())
            .andExpect(jsonPath("$.payload.token_pair.access.token").exists())
            .andExpect(jsonPath("$.payload.token_pair.refresh.token").exists())
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("$REFRESH_TOKEN_COOKIE=")))
            .andDo(
                document(
                    "auth-login",
                    requestFields(
                        fieldWithPath("email").description("Account email"),
                        fieldWithPath("password").description("Account password"),
                    ),
                    responseHeaders(
                        headerWithName(HttpHeaders.SET_COOKIE).description("Refresh token cookie"),
                    ),
                    responseFields(
                        fieldWithPath("code").description("API response code"),
                        fieldWithPath("message").description("API response message"),
                        fieldWithPath("payload.account.id").description("Account id"),
                        fieldWithPath("payload.account.email").description("Account email"),
                        fieldWithPath("payload.account.name").description("Account nickname"),
                        fieldWithPath("payload.account.status").description("Account status"),
                        fieldWithPath("payload.account.role").description("Account role"),
                        fieldWithPath("payload.account.createdAt").description("Account creation time"),
                        fieldWithPath("payload.account.updatedAt").description("Account update time"),
                        fieldWithPath("payload.token_pair.access.token").description("Access token"),
                        fieldWithPath("payload.token_pair.access.expiry").description("Access token expiry"),
                        fieldWithPath("payload.token_pair.refresh.token").description("Refresh token"),
                        fieldWithPath("payload.token_pair.refresh.expiry").description("Refresh token expiry"),
                    ),
                ),
            )
            .andReturn()
        var refreshCookie = requireRefreshCookie(loginResult)

        val refreshResult = mockMvc.perform(
            post("/api/auth/refresh")
                .cookie(refreshCookie),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.payload.token_pair.access.token").exists())
            .andExpect(jsonPath("$.payload.token_pair.refresh.token").exists())
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("$REFRESH_TOKEN_COOKIE=")))
            .andDo(
                document(
                    "auth-refresh",
                    responseHeaders(
                        headerWithName(HttpHeaders.SET_COOKIE).description("Rotated refresh token cookie"),
                    ),
                    responseFields(
                        fieldWithPath("code").description("API response code"),
                        fieldWithPath("message").description("API response message"),
                        fieldWithPath("payload.account.id").description("Account id"),
                        fieldWithPath("payload.account.email").description("Account email"),
                        fieldWithPath("payload.account.name").description("Account nickname"),
                        fieldWithPath("payload.account.status").description("Account status"),
                        fieldWithPath("payload.account.role").description("Account role"),
                        fieldWithPath("payload.account.createdAt").description("Account creation time"),
                        fieldWithPath("payload.account.updatedAt").description("Account update time"),
                        fieldWithPath("payload.token_pair.access.token").description("Access token"),
                        fieldWithPath("payload.token_pair.access.expiry").description("Access token expiry"),
                        fieldWithPath("payload.token_pair.refresh.token").description("Refresh token"),
                        fieldWithPath("payload.token_pair.refresh.expiry").description("Refresh token expiry"),
                    ),
                ),
            )
            .andReturn()
        refreshCookie = requireRefreshCookie(refreshResult)

        mockMvc.perform(
            post("/api/auth/logout")
                .cookie(refreshCookie),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.payload.revoked").value(true))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")))
            .andDo(
                document(
                    "auth-logout",
                    responseHeaders(
                        headerWithName(HttpHeaders.SET_COOKIE).description("Cleared refresh token cookie"),
                    ),
                    responseFields(
                        fieldWithPath("code").description("API response code"),
                        fieldWithPath("message").description("API response message"),
                        fieldWithPath("payload.revoked").description("Whether session revoke succeeded"),
                    ),
                ),
            )
    }

    private fun requireRefreshCookie(result: MvcResult): Cookie {
        val response = result.response
        val cookie = response.getCookie(REFRESH_TOKEN_COOKIE)
        if (cookie != null) {
            return cookie
        }
        val raw = response.getHeader(HttpHeaders.SET_COOKIE)
        val token = raw
            ?.substringBefore(';')
            ?.substringAfter("$REFRESH_TOKEN_COOKIE=", "")
            ?.takeIf { it.isNotBlank() }
        assertNotNull(token, "Refresh token cookie not found")
        return Cookie(REFRESH_TOKEN_COOKIE, token)
    }
}
