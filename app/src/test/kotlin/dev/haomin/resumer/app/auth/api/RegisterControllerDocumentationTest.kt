package dev.haomin.resumer.app.auth.api

import dev.haomin.resumer.app.TestcontainersConfiguration
import dev.haomin.resumer.app.common.id.OTCGenerator
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@Import(
    TestcontainersConfiguration::class,
    RegisterControllerDocumentationTest.FixedOtcTestConfig::class,
)
class RegisterControllerDocumentationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val fixedOTCGenerator: FixedOTCGenerator,
    private val objectMapper: ObjectMapper,
) {

    @Test
    fun `register start verify confirm should work and generate docs`() {
        fixedOTCGenerator.code = "123456"
        val email = "student-${UUID.randomUUID()}@example.com"

        val startResult = mockMvc.perform(
            post("/api/register/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "$email"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.payload.attempt_id").exists())
            .andDo(
                document(
                    "register-start",
                    requestFields(
                        fieldWithPath("email").description("Email used for registration verification"),
                    ),
                    responseFields(
                        fieldWithPath("code").description("API response code"),
                        fieldWithPath("message").description("API response message"),
                        fieldWithPath("payload.attempt_id").description("Registration attempt id"),
                        fieldWithPath("payload.expires_in_seconds").description("Attempt expiration in seconds"),
                    ),
                ),
            )
            .andReturn()

        val attemptId = extractAttemptId(startResult.response.contentAsString)
        assertNotNull(attemptId)

        mockMvc.perform(
            post("/api/register/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "attemptId": "$attemptId",
                      "code": "123456"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.payload.verified").value(true))
            .andDo(
                document(
                    "register-verify",
                    requestFields(
                        fieldWithPath("attemptId").description("Registration attempt id"),
                        fieldWithPath("code").description("Verification code"),
                    ),
                    responseFields(
                        fieldWithPath("code").description("API response code"),
                        fieldWithPath("message").description("API response message"),
                        fieldWithPath("payload.verified").description("Whether verification succeeded"),
                        fieldWithPath("payload.verified_at").description("Verification timestamp"),
                    ),
                ),
            )

        mockMvc.perform(
            post("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "attemptId": "$attemptId",
                      "nickname": "Student",
                      "password": "password-123"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.payload.account.id").exists())
            .andExpect(jsonPath("$.payload.token_pair.access.token").exists())
            .andExpect(jsonPath("$.payload.token_pair.refresh.token").exists())
            .andExpect(header().exists(HttpHeaders.SET_COOKIE))
            .andDo(
                document(
                    "register-confirm",
                    requestFields(
                        fieldWithPath("attemptId").description("Verified registration attempt id"),
                        fieldWithPath("nickname").description("Account nickname"),
                        fieldWithPath("password").description("Account password"),
                    ),
                    responseHeaders(
                        headerWithName(HttpHeaders.SET_COOKIE).description("Refresh token cookie"),
                    ),
                    responseFields(
                        fieldWithPath("code").description("API response code"),
                        fieldWithPath("message").description("API response message"),
                        fieldWithPath("payload.account.id").description("Created account id"),
                        fieldWithPath("payload.account.email").description("Created account email"),
                        fieldWithPath("payload.account.name").description("Created account nickname"),
                        fieldWithPath("payload.account.status").description("Created account status"),
                        fieldWithPath("payload.account.role").description("Created account role"),
                        fieldWithPath("payload.account.createdAt").description("Account creation time"),
                        fieldWithPath("payload.account.updatedAt").description("Account update time"),
                        fieldWithPath("payload.token_pair.access.token").description("Access token"),
                        fieldWithPath("payload.token_pair.access.expiry").description("Access token expiry"),
                        fieldWithPath("payload.token_pair.refresh.token").description("Refresh token"),
                        fieldWithPath("payload.token_pair.refresh.expiry").description("Refresh token expiry"),
                    ),
                ),
            )
    }

    @Test
    fun `register resend should work and generate docs`() {
        fixedOTCGenerator.code = "654321"
        val email = "resend-${UUID.randomUUID()}@example.com"
        val startResult = mockMvc.perform(
            post("/api/register/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "$email"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(0))
            .andReturn()

        val attemptId = extractAttemptId(startResult.response.contentAsString)
        assertNotNull(attemptId)

        mockMvc.perform(
            post("/api/register/resend")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "attemptId": "$attemptId"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.payload.attempt_id").value(attemptId))
            .andDo(
                document(
                    "register-resend",
                    requestFields(
                        fieldWithPath("attemptId").description("Registration attempt id"),
                    ),
                    responseFields(
                        fieldWithPath("code").description("API response code"),
                        fieldWithPath("message").description("API response message"),
                        fieldWithPath("payload.attempt_id").description("Registration attempt id"),
                        fieldWithPath("payload.expires_in_seconds").description("Attempt expiration in seconds"),
                    ),
                ),
            )
    }

    private fun extractAttemptId(json: String): String {
        return objectMapper.readTree(json)
            .path("payload")
            .path("attempt_id")
            .asString()
            .takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("attempt_id not found in response: $json")
    }

    @TestConfiguration
    class FixedOtcTestConfig {
        @Bean
        @Primary
        fun fixedOTCGenerator(): FixedOTCGenerator =
            FixedOTCGenerator()
    }
}

class FixedOTCGenerator : OTCGenerator {
    @Volatile
    var code: String = "123456"

    override fun next(length: Int): String =
        if (code.length == length) code else code.padStart(length, '0').takeLast(length)
}
