package dev.haomin.resumer.app.module.session.api

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
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.restdocs.request.RequestDocumentation.queryParameters
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
    SessionControllerDocumentationTest.FixedLlmTestConfig::class,
)
class SessionControllerDocumentationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val accountRepo: AccountRepo,
    private val passwordEncoder: PasswordEncoder,
    private val accessService: AccessService,
) {

    @Test
    fun `session create list detail advice and cover letter should work and generate docs`() {
        val token = createAccessToken()
        val resumeId = uploadResumeAndWaitCompleted(token)

        val createResult = mockMvc.perform(
            post("/api/sessions")
                .header(HttpHeaders.AUTHORIZATION, "$BEAR_TOKEN_PREFIX $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "resumeId": "$resumeId",
                      "company": "OpenAI",
                      "position": "Backend Engineer",
                      "jobDescription": "Build scalable backend services with Kotlin, Spring Boot, and Redis."
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.payload.session.id").exists())
            .andDo(
                document(
                    "session-create",
                    requestFields(
                        fieldWithPath("resumeId").description("Optional resume id used for fit analysis"),
                        fieldWithPath("company").description("Target company"),
                        fieldWithPath("position").description("Target position"),
                        fieldWithPath("jobDescription").description("Job description text"),
                    ),
                    responseFields(
                        fieldWithPath("code").description("API response code"),
                        fieldWithPath("message").description("API response message"),
                        fieldWithPath("payload.session.id").description("Session id"),
                        fieldWithPath("payload.session.resumeId").description("Bound resume id"),
                        fieldWithPath("payload.session.company").description("Company"),
                        fieldWithPath("payload.session.position").description("Position"),
                        fieldWithPath("payload.session.jobDescription").description("Job description"),
                        fieldWithPath("payload.session.createdAt").description("Creation timestamp"),
                    ),
                ),
            )
            .andReturn()
        val sessionId = read(createResult.response.contentAsString).path("payload").path("session").path("id").asString()
        assertNotNull(sessionId)

        mockMvc.perform(
            get("/api/sessions")
                .header(HttpHeaders.AUTHORIZATION, "$BEAR_TOKEN_PREFIX $token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.payload.sessions[0].id").exists())
            .andDo(
                document(
                    "session-list",
                    responseFields(
                        fieldWithPath("code").description("API response code"),
                        fieldWithPath("message").description("API response message"),
                        fieldWithPath("payload.sessions[]").description("Session list"),
                        fieldWithPath("payload.sessions[].id").description("Session id"),
                        fieldWithPath("payload.sessions[].resumeId").description("Bound resume id or null"),
                        fieldWithPath("payload.sessions[].company").description("Company"),
                        fieldWithPath("payload.sessions[].position").description("Position"),
                        fieldWithPath("payload.sessions[].positionAdviceStatus").description("Position advice task status"),
                        fieldWithPath("payload.sessions[].resumeFitStatus").description("Resume-fit task status"),
                        fieldWithPath("payload.sessions[].createdAt").description("Creation timestamp"),
                    ),
                ),
            )

        mockMvc.perform(
            get("/api/sessions/{sessionId}", sessionId)
                .header(HttpHeaders.AUTHORIZATION, "$BEAR_TOKEN_PREFIX $token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.payload.session.id").value(sessionId))
            .andDo(
                document(
                    "session-detail",
                    pathParameters(
                        parameterWithName("sessionId").description("Session id"),
                    ),
                    responseFields(
                        fieldWithPath("code").description("API response code"),
                        fieldWithPath("message").description("API response message"),
                        fieldWithPath("payload.session.id").description("Session id"),
                        fieldWithPath("payload.session.resumeId").description("Bound resume id or null"),
                        fieldWithPath("payload.session.company").description("Company"),
                        fieldWithPath("payload.session.position").description("Position"),
                        fieldWithPath("payload.session.jobDescription").description("Job description"),
                        fieldWithPath("payload.session.positionAdviceStatus").description("Position advice status"),
                        fieldWithPath("payload.session.positionAdviceError").description("Position advice error or null"),
                        fieldWithPath("payload.session.resumeFitStatus").description("Resume-fit status"),
                        fieldWithPath("payload.session.resumeFitError").description("Resume-fit error or null"),
                        fieldWithPath("payload.session.createdAt").description("Creation timestamp"),
                        fieldWithPath("payload.session.updatedAt").description("Last update timestamp"),
                    ),
                ),
            )

        mockMvc.perform(
            post("/api/sessions/{sessionId}/advice/position", sessionId)
                .header(HttpHeaders.AUTHORIZATION, "$BEAR_TOKEN_PREFIX $token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(0))
            .andDo(
                document(
                    "session-queue-position-advice",
                    pathParameters(
                        parameterWithName("sessionId").description("Session id"),
                    ),
                    responseFields(
                        fieldWithPath("code").description("API response code"),
                        fieldWithPath("message").description("API response message"),
                        fieldWithPath("payload.task.sessionId").description("Session id"),
                        fieldWithPath("payload.task.jobType").description("Queued job type"),
                        fieldWithPath("payload.task.status").description("Initial task status"),
                        fieldWithPath("payload.task.traceId").description("Task trace id"),
                    ),
                ),
            )
        waitUntilSessionAdviceCompleted(token, sessionId, "positionAdviceStatus")

        mockMvc.perform(
            post("/api/sessions/{sessionId}/advice/resume-fit", sessionId)
                .header(HttpHeaders.AUTHORIZATION, "$BEAR_TOKEN_PREFIX $token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(0))
            .andDo(
                document(
                    "session-queue-resume-fit-advice",
                    pathParameters(
                        parameterWithName("sessionId").description("Session id"),
                    ),
                    responseFields(
                        fieldWithPath("code").description("API response code"),
                        fieldWithPath("message").description("API response message"),
                        fieldWithPath("payload.task.sessionId").description("Session id"),
                        fieldWithPath("payload.task.jobType").description("Queued job type"),
                        fieldWithPath("payload.task.status").description("Initial task status"),
                        fieldWithPath("payload.task.traceId").description("Task trace id"),
                    ),
                ),
            )
        waitUntilSessionAdviceCompleted(token, sessionId, "resumeFitStatus")

        mockMvc.perform(
            get("/api/sessions/{sessionId}/advice", sessionId)
                .header(HttpHeaders.AUTHORIZATION, "$BEAR_TOKEN_PREFIX $token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.payload.advice.fitScore").exists())
            .andDo(
                document(
                    "session-latest-advice",
                    pathParameters(
                        parameterWithName("sessionId").description("Session id"),
                    ),
                    responseFields(
                        fieldWithPath("code").description("API response code"),
                        fieldWithPath("message").description("API response message"),
                        fieldWithPath("payload.advice.id").description("Advice id"),
                        fieldWithPath("payload.advice.sessionId").description("Session id"),
                        fieldWithPath("payload.advice.positionSummary").description("Position summary"),
                        fieldWithPath("payload.advice.keyRequirements[]").description("Key requirement list"),
                        fieldWithPath("payload.advice.likelyInterviewFocus[]").description("Likely interview focus list"),
                        fieldWithPath("payload.advice.redFlags[]").description("Candidate risk signals"),
                        fieldWithPath("payload.advice.fitScore").description("Resume fit score"),
                        fieldWithPath("payload.advice.gaps[]").description("Fit gaps list"),
                        fieldWithPath("payload.advice.rewriteSuggestions[]").description("Rewrite suggestions"),
                        fieldWithPath("payload.advice.rewriteSuggestions[].section").description("Target section"),
                        fieldWithPath("payload.advice.rewriteSuggestions[].priority").description("Suggestion priority"),
                        fieldWithPath("payload.advice.rewriteSuggestions[].issue").description("Detected issue"),
                        fieldWithPath("payload.advice.rewriteSuggestions[].recommendation").description("Rewrite recommendation"),
                        fieldWithPath("payload.advice.rewriteSuggestions[].example").description("Optional rewrite example"),
                        fieldWithPath("payload.advice.createdAt").description("Advice creation timestamp"),
                    ),
                ),
            )

        val cover1Result = mockMvc.perform(
            post("/api/sessions/{sessionId}/cover-letters", sessionId)
                .header(HttpHeaders.AUTHORIZATION, "$BEAR_TOKEN_PREFIX $token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.payload.cover_letter.version").value(1))
            .andDo(
                document(
                    "session-cover-letter-generate",
                    pathParameters(
                        parameterWithName("sessionId").description("Session id"),
                    ),
                    responseFields(
                        fieldWithPath("code").description("API response code"),
                        fieldWithPath("message").description("API response message"),
                        fieldWithPath("payload.cover_letter.id").description("Cover letter id"),
                        fieldWithPath("payload.cover_letter.sessionId").description("Session id"),
                        fieldWithPath("payload.cover_letter.version").description("Version number"),
                        fieldWithPath("payload.cover_letter.content").description("Cover letter content"),
                        fieldWithPath("payload.cover_letter.createdAt").description("Creation timestamp"),
                    ),
                ),
            )
            .andReturn()
        val version1 = read(cover1Result.response.contentAsString).path("payload").path("cover_letter").path("version").asInt()
        assertNotNull(version1)

        mockMvc.perform(
            get("/api/sessions/{sessionId}/cover-letters", sessionId)
                .header(HttpHeaders.AUTHORIZATION, "$BEAR_TOKEN_PREFIX $token")
                .param("version", "1"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.payload.cover_letter.version").value(1))
            .andDo(
                document(
                    "session-cover-letter-get",
                    pathParameters(
                        parameterWithName("sessionId").description("Session id"),
                    ),
                    queryParameters(
                        parameterWithName("version").description("Optional cover letter version"),
                    ),
                    responseFields(
                        fieldWithPath("code").description("API response code"),
                        fieldWithPath("message").description("API response message"),
                        fieldWithPath("payload.cover_letter.id").description("Cover letter id"),
                        fieldWithPath("payload.cover_letter.sessionId").description("Session id"),
                        fieldWithPath("payload.cover_letter.version").description("Version number"),
                        fieldWithPath("payload.cover_letter.content").description("Cover letter content"),
                        fieldWithPath("payload.cover_letter.createdAt").description("Creation timestamp"),
                    ),
                ),
            )
    }

    private fun waitUntilSessionAdviceCompleted(token: String, sessionId: String, statusField: String) {
        repeat(30) {
            val result = mockMvc.perform(
                get("/api/sessions/$sessionId")
                    .header(HttpHeaders.AUTHORIZATION, "$BEAR_TOKEN_PREFIX $token"),
            )
                .andExpect(status().isOk)
                .andReturn()
            val status = read(result.response.contentAsString)
                .path("payload")
                .path("session")
                .path(statusField)
                .asString()
            if (status == "COMPLETED" || status == "FAILED") {
                return
            }
            Thread.sleep(200)
        }
        throw IllegalStateException("session advice job did not finish in time: sessionId=$sessionId, field=$statusField")
    }

    private fun uploadResumeAndWaitCompleted(token: String): String {
        val file = MockMultipartFile(
            "file",
            "resume.txt",
            MediaType.TEXT_PLAIN_VALUE,
            """
            Jane Student
            Email: jane@example.com
            Project: Built Spring Boot services with Redis and PostgreSQL.
            """.trimIndent().toByteArray(),
        )

        val uploadResult = mockMvc.perform(
            multipart("/api/resumes/upload-and-analyze")
                .file(file)
                .header(HttpHeaders.AUTHORIZATION, "$BEAR_TOKEN_PREFIX $token"),
        )
            .andExpect(status().isOk)
            .andReturn()

        val resumeId = read(uploadResult.response.contentAsString)
            .path("payload")
            .path("resume")
            .path("resumeId")
            .asString()
        assertNotNull(resumeId)

        repeat(30) {
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
                return resumeId
            }
            Thread.sleep(200)
        }
        throw IllegalStateException("resume analysis did not finish in time: $resumeId")
    }

    private fun createAccessToken(): String {
        val email = "session-doc-${UUID.randomUUID()}@example.com"
        val passwordHash = requireNotNull(passwordEncoder.encode("Password123!")) { "password encode failed" }
        val account = accountRepo.insertAndReturn(
            AccountInsertQuery(
                email = email,
                password = passwordHash,
                name = "SessionDocUser",
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
                when {
                    systemPrompt.contains("\"fitScore\"") -> {
                        """
                        {
                          "fitScore": 82,
                          "gaps": ["Missing measurable impact in recent project"],
                          "rewriteSuggestions": [
                            {
                              "section": "project",
                              "priority": "high",
                              "issue": "Project bullet is too generic",
                              "recommendation": "Add impact and concrete ownership details",
                              "example": "Built async resume pipeline and reduced average processing time by 30%."
                            }
                          ]
                        }
                        """.trimIndent()
                    }
                    systemPrompt.contains("\"keyRequirements\"") -> {
                        """
                        {
                          "summary": "Backend role with strong emphasis on service design and implementation.",
                          "keyRequirements": ["Kotlin or Java backend", "Spring Boot APIs", "Redis messaging"],
                          "likelyInterviewFocus": ["API design", "Problem solving", "Ownership"],
                          "redFlags": ["No evidence of production-like backend project"]
                        }
                        """.trimIndent()
                    }
                    systemPrompt.contains("\"score\"") && systemPrompt.contains("\"strengths\"") -> {
                        """
                        {
                          "score": 85,
                          "summary": "Good backend profile with relevant stack exposure.",
                          "strengths": ["Spring Boot backend implementation", "Redis usage"],
                          "suggestions": [
                            {
                              "category": "content",
                              "priority": "high",
                              "issue": "Missing measurable outcomes",
                              "recommendation": "Add concrete results to major project bullets."
                            }
                          ]
                        }
                        """.trimIndent()
                    }
                    else -> {
                        """
                        Dear Hiring Team,

                        I am excited to apply for the Backend Engineer role at OpenAI. I am a student-focused backend developer with hands-on experience building Spring Boot services and using Redis for async workflows.

                        In my recent project work, I implemented backend APIs and integrated a queue-based processing flow to improve reliability and responsiveness. This required strong ownership across service design, data handling, and debugging, which matches the practical expectations in your role.

                        I am particularly motivated by opportunities to build scalable systems that improve real user outcomes. The role's focus on backend engineering and service quality aligns well with my current strengths and growth direction.

                        I would welcome the chance to discuss how I can contribute to your engineering team.
                        """.trimIndent()
                    }
                }
        }
    }
}
