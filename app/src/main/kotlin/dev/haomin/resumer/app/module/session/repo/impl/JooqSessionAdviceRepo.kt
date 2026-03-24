package dev.haomin.resumer.app.module.session.repo.impl

import dev.haomin.filesheep.jooq.tables.pojos.P_SessionAdvice
import dev.haomin.filesheep.jooq.tables.references.SESSION_ADVICE
import dev.haomin.resumer.app.module.session.domain.RewriteSuggestion
import dev.haomin.resumer.app.module.session.domain.SessionAdvice
import dev.haomin.resumer.app.module.session.repo.SessionAdviceRepo
import dev.haomin.resumer.app.module.session.repo.query.SessionAdviceInsertQuery
import org.jooq.DSLContext
import org.jooq.JSONB
import org.springframework.stereotype.Repository
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Repository
class JooqSessionAdviceRepo(
    private val dsl: DSLContext,
    private val objectMapper: ObjectMapper,
) : SessionAdviceRepo {

    override fun selectLatestBySessionId(sessionId: UUID): SessionAdvice? =
        dsl.selectFrom(SESSION_ADVICE)
            .where(SESSION_ADVICE.sessionId.eq(sessionId))
            .orderBy(SESSION_ADVICE.createdAt.desc())
            .limit(1)
            .fetchOneInto(P_SessionAdvice::class.java)
            ?.toDomain(objectMapper)

    override fun insertAndReturn(query: SessionAdviceInsertQuery): SessionAdvice =
        dsl.newRecord(SESSION_ADVICE)
            .apply {
                this.id = query.id
                this.sessionId = query.sessionId
                this.positionSummary = query.positionSummary
                this.keyRequirementsJson = JSONB.valueOf(objectMapper.writeValueAsString(query.keyRequirements))
                this.likelyInterviewFocusJson = JSONB.valueOf(objectMapper.writeValueAsString(query.likelyInterviewFocus))
                this.redFlagsJson = JSONB.valueOf(objectMapper.writeValueAsString(query.redFlags))
                this.fitScore = query.fitScore
                this.gapsJson = JSONB.valueOf(objectMapper.writeValueAsString(query.gaps))
                this.rewriteSuggestionsJson = JSONB.valueOf(objectMapper.writeValueAsString(query.rewriteSuggestions))
                this.createdAt = query.createdAt
            }
            .let { dsl.insertInto(SESSION_ADVICE).set(it).returning().fetchOneInto(P_SessionAdvice::class.java) }
            ?.toDomain(objectMapper)
            ?: throw IllegalStateException("Failed to insert session advice and return the record")
}

internal fun P_SessionAdvice.toDomain(objectMapper: ObjectMapper): SessionAdvice =
    SessionAdvice(
        id = requireNotNull(id) { "SessionAdvice.id is null" },
        sessionId = requireNotNull(sessionId) { "SessionAdvice.sessionId is null" },
        positionSummary = requireNotNull(positionSummary) { "SessionAdvice.positionSummary is null" },
        keyRequirements = parseStringList(requireNotNull(keyRequirementsJson) { "SessionAdvice.keyRequirementsJson is null" }, objectMapper),
        likelyInterviewFocus = parseStringList(requireNotNull(likelyInterviewFocusJson) { "SessionAdvice.likelyInterviewFocusJson is null" }, objectMapper),
        redFlags = parseStringList(requireNotNull(redFlagsJson) { "SessionAdvice.redFlagsJson is null" }, objectMapper),
        fitScore = requireNotNull(fitScore) { "SessionAdvice.fitScore is null" },
        gaps = parseStringList(requireNotNull(gapsJson) { "SessionAdvice.gapsJson is null" }, objectMapper),
        rewriteSuggestions = parseRewriteSuggestions(
            requireNotNull(rewriteSuggestionsJson) { "SessionAdvice.rewriteSuggestionsJson is null" },
            objectMapper,
        ),
        createdAt = requireNotNull(createdAt) { "SessionAdvice.createdAt is null" },
    )

private fun parseStringList(value: JSONB, objectMapper: ObjectMapper): List<String> =
    objectMapper.readValue(value.data(), object : TypeReference<List<String>>() {})

private fun parseRewriteSuggestions(value: JSONB, objectMapper: ObjectMapper): List<RewriteSuggestion> =
    objectMapper.readValue(value.data(), object : TypeReference<List<RewriteSuggestion>>() {})
