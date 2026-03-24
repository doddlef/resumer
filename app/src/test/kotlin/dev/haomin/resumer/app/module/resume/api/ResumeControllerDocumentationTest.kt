package dev.haomin.resumer.app.module.resume.api

import dev.haomin.resumer.app.TestcontainersConfiguration
import dev.haomin.resumer.app.auth.BEAR_TOKEN_PREFIX
import dev.haomin.resumer.app.auth.repo.AccountRepo
import dev.haomin.resumer.app.auth.repo.query.AccountInsertQuery
import dev.haomin.resumer.app.auth.service.AccessService
import dev.haomin.resumer.app.common.ai.LLMClient
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
import org.springframework.mock.web.MockMultipartFile
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.restdocs.request.RequestDocumentation.partWithName
import org.springframework.restdocs.request.RequestDocumentation.requestParts
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@Import(
    TestcontainersConfiguration::class,
    ResumeControllerDocumentationTest.FixedLlmTestConfig::class,
)
class ResumeControllerDocumentationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val accountRepo: AccountRepo,
    private val passwordEncoder: PasswordEncoder,
    private val accessService: AccessService,
) {

    @Test
    fun `resume upload list detail status reanalyze should work and generate docs`() {
        val token = createAccessToken()
        val uploadedResumeId = uploadResumeAndWaitCompleted(token)

        mockMvc.perform(
            get("/api/resumes")
                .header(HttpHeaders.AUTHORIZATION, "$BEAR_TOKEN_PREFIX $token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.payload.resumes[0].id").exists())
            .andDo(
                document(
                    "resume-list",
                    responseFields(
                        fieldWithPath("code").description("API response code"),
                        fieldWithPath("message").description("API response message"),
                        fieldWithPath("payload.resumes[]").description("Resume list"),
                        fieldWithPath("payload.resumes[].id").description("Resume id"),
                        fieldWithPath("payload.resumes[].name").description("Original resume file name"),
                        fieldWithPath("payload.resumes[].status").description("Resume processing status"),
                        fieldWithPath("payload.resumes[].score").description("Latest analysis score, null if not available"),
                        fieldWithPath("payload.resumes[].createdAt").description("Resume creation time"),
                    ),
                ),
            )

        mockMvc.perform(
            get("/api/resumes/{resumeId}", uploadedResumeId)
                .header(HttpHeaders.AUTHORIZATION, "$BEAR_TOKEN_PREFIX $token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.payload.resume.id").value(uploadedResumeId))
            .andExpect(jsonPath("$.payload.resume.summary").exists())
            .andDo(
                document(
                    "resume-detail",
                    pathParameters(
                        parameterWithName("resumeId").description("Resume id"),
                    ),
                    responseFields(
                        fieldWithPath("code").description("API response code"),
                        fieldWithPath("message").description("API response message"),
                        fieldWithPath("payload.resume.id").description("Resume id"),
                        fieldWithPath("payload.resume.name").description("Resume file name"),
                        fieldWithPath("payload.resume.status").description("Resume processing status"),
                        fieldWithPath("payload.resume.score").description("Latest analysis score"),
                        fieldWithPath("payload.resume.summary").description("Latest analysis summary"),
                        fieldWithPath("payload.resume.strengths[]").description("Strength points"),
                        fieldWithPath("payload.resume.suggestions[]").description("Suggestions list"),
                        fieldWithPath("payload.resume.suggestions[].category").description("Suggestion category"),
                        fieldWithPath("payload.resume.suggestions[].priority").description("Suggestion priority"),
                        fieldWithPath("payload.resume.suggestions[].issue").description("Detected issue"),
                        fieldWithPath("payload.resume.suggestions[].recommendation").description("Suggested improvement"),
                        fieldWithPath("payload.resume.error").description("Latest processing error, null if success"),
                        fieldWithPath("payload.resume.createdAt").description("Resume creation time"),
                        fieldWithPath("payload.resume.updatedAt").description("Resume update time"),
                    ),
                ),
            )

        mockMvc.perform(
            get("/api/resumes/{resumeId}/status", uploadedResumeId)
                .header(HttpHeaders.AUTHORIZATION, "$BEAR_TOKEN_PREFIX $token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.payload.resume.resumeId").value(uploadedResumeId))
            .andDo(
                document(
                    "resume-status",
                    pathParameters(
                        parameterWithName("resumeId").description("Resume id"),
                    ),
                    responseFields(
                        fieldWithPath("code").description("API response code"),
                        fieldWithPath("message").description("API response message"),
                        fieldWithPath("payload.resume.resumeId").description("Resume id"),
                        fieldWithPath("payload.resume.status").description("Resume processing status"),
                        fieldWithPath("payload.resume.score").description("Latest analysis score, null if not available"),
                        fieldWithPath("payload.resume.error").description("Processing error, null if no failure"),
                        fieldWithPath("payload.resume.updatedAt").description("Resume update time"),
                    ),
                ),
            )

        mockMvc.perform(
            post("/api/resumes/{resumeId}/reanalyze", uploadedResumeId)
                .header(HttpHeaders.AUTHORIZATION, "$BEAR_TOKEN_PREFIX $token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.payload.resume.resumeId").value(uploadedResumeId))
            .andDo(
                document(
                    "resume-reanalyze",
                    pathParameters(
                        parameterWithName("resumeId").description("Resume id"),
                    ),
                    responseFields(
                        fieldWithPath("code").description("API response code"),
                        fieldWithPath("message").description("API response message"),
                        fieldWithPath("payload.resume.resumeId").description("Resume id"),
                        fieldWithPath("payload.resume.status").description("Reanalysis queued status"),
                        fieldWithPath("payload.resume.traceId").description("Trace id for the queued reanalysis job"),
                    ),
                ),
            )
    }

    private fun uploadResumeAndWaitCompleted(token: String): String {
        val file = MockMultipartFile(
            "file",
            "resume.txt",
            MediaType.TEXT_PLAIN_VALUE,
            """
            John Student
            Email: john@example.com
            Experience: Built backend service using Spring Boot and Redis.
            """.trimIndent().toByteArray(),
        )

        val uploadResult = mockMvc.perform(
            multipart("/api/resumes/upload-and-analyze")
                .file(file)
                .header(HttpHeaders.AUTHORIZATION, "$BEAR_TOKEN_PREFIX $token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.payload.resume.resumeId").exists())
            .andDo(
                document(
                    "resume-upload-and-analyze",
                    requestParts(
                        partWithName("file").description("Resume file to upload (pdf/txt)"),
                    ),
                    responseFields(
                        fieldWithPath("code").description("API response code"),
                        fieldWithPath("message").description("API response message"),
                        fieldWithPath("payload.resume.resumeId").description("Resume id"),
                        fieldWithPath("payload.resume.name").description("Original file name"),
                        fieldWithPath("payload.resume.status").description("Initial processing status"),
                        fieldWithPath("payload.resume.duplicate").description("Whether same content already exists for account"),
                        fieldWithPath("payload.resume.createdAt").description("Resume creation time"),
                    ),
                ),
            )
            .andReturn()

        val resumeId = read(uploadResult.response.contentAsString)
            .path("payload")
            .path("resume")
            .path("resumeId")
            .asString()
        assertNotNull(resumeId)
        waitUntilCompleted(token, resumeId)
        return resumeId
    }

    private fun waitUntilCompleted(token: String, resumeId: String) {
        repeat(25) {
            val statusResult = mockMvc.perform(
                get("/api/resumes/$resumeId/status")
                    .header(HttpHeaders.AUTHORIZATION, "$BEAR_TOKEN_PREFIX $token"),
            )
                .andExpect(status().isOk)
                .andReturn()
            val status = read(statusResult.response.contentAsString)
                .path("payload")
                .path("resume")
                .path("status")
                .asString()
            if (status == "COMPLETED" || status == "FAILED") {
                return
            }
            Thread.sleep(200)
        }
        throw IllegalStateException("Resume did not finish analysis in time: $resumeId")
    }

    private fun createAccessToken(): String {
        val email = "resume-doc-${UUID.randomUUID()}@example.com"
        val passwordHash = requireNotNull(passwordEncoder.encode("Password123!")) { "password encode failed" }
        val account = accountRepo.insertAndReturn(
            AccountInsertQuery(
                email = email,
                password = passwordHash,
                name = "ResumeDocUser",
            ),
        )
        return accessService.issue(account).token
    }

    private fun read(json: String): JsonNode =
        objectMapper.readTree(json)

    @TestConfiguration
    class FixedLlmTestConfig {
        @Bean
        @Primary
        fun fixedLlmClient(): LLMClient = object : LLMClient {
            override fun chat(systemPrompt: String, userPrompt: String): String =
                """
                {
                  "score": 86,
                  "summary": "Strong backend experience with clear project context.",
                  "strengths": ["Spring Boot backend implementation", "Uses Redis for async processing"],
                  "suggestions": [
                    {
                      "category": "content",
                      "priority": "high",
                      "issue": "Missing quantified outcomes",
                      "recommendation": "Add measurable impact, such as reduced latency or throughput gains."
                    }
                  ]
                }
                """.trimIndent()
        }
    }
}
