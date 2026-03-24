package dev.haomin.resumer.app.module.resume.repo.impl

import dev.haomin.filesheep.jooq.tables.pojos.P_ResumeAnalysis
import dev.haomin.filesheep.jooq.tables.references.RESUME_ANALYSIS
import dev.haomin.resumer.app.module.resume.domain.ResumeAnalysis
import dev.haomin.resumer.app.module.resume.domain.ResumeSuggestion
import dev.haomin.resumer.app.module.resume.repo.ResumeAnalysisRepo
import dev.haomin.resumer.app.module.resume.repo.query.ResumeAnalysisInsertQuery
import org.jooq.DSLContext
import org.jooq.JSONB
import org.springframework.stereotype.Repository
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Repository
class JooqResumeAnalysisRepo(
    private val dsl: DSLContext,
    private val objectMapper: ObjectMapper,
) : ResumeAnalysisRepo {

    override fun selectLatestByResumeId(resumeId: UUID): ResumeAnalysis? =
        dsl.selectFrom(RESUME_ANALYSIS)
            .where(RESUME_ANALYSIS.resumeId.eq(resumeId))
            .orderBy(RESUME_ANALYSIS.createdAt.desc())
            .limit(1)
            .fetchOneInto(P_ResumeAnalysis::class.java)
            ?.toDomain(objectMapper)

    override fun selectLatestByResumeIds(resumeIds: Collection<UUID>): Map<UUID, ResumeAnalysis> {
        if (resumeIds.isEmpty()) {
            return emptyMap()
        }
        return dsl.selectFrom(RESUME_ANALYSIS)
            .where(RESUME_ANALYSIS.resumeId.`in`(resumeIds))
            .orderBy(RESUME_ANALYSIS.resumeId.asc(), RESUME_ANALYSIS.createdAt.desc())
            .fetchInto(P_ResumeAnalysis::class.java)
            .map { it.toDomain(objectMapper) }
            .groupBy { it.resumeId }
            .mapValues { (_, analyses) -> analyses.first() }
    }

    override fun insertAndReturn(query: ResumeAnalysisInsertQuery): ResumeAnalysis =
        dsl.newRecord(RESUME_ANALYSIS)
            .apply {
                this.id = query.id
                this.resumeId = query.resumeId
                this.score = query.score
                this.summary = query.summary
                this.strengthsJson = JSONB.valueOf(objectMapper.writeValueAsString(query.strengths))
                this.suggestionsJson = JSONB.valueOf(objectMapper.writeValueAsString(query.suggestions))
                this.createdAt = query.createdAt
            }
            .let { dsl.insertInto(RESUME_ANALYSIS).set(it).returning().fetchOneInto(P_ResumeAnalysis::class.java) }
            ?.toDomain(objectMapper)
            ?: throw IllegalStateException("Failed to insert resume analysis and return the record")
}

internal fun P_ResumeAnalysis.toDomain(objectMapper: ObjectMapper): ResumeAnalysis =
    ResumeAnalysis(
        id = requireNotNull(id) { "ResumeAnalysis.id is null" },
        resumeId = requireNotNull(resumeId) { "ResumeAnalysis.resumeId is null" },
        score = requireNotNull(score) { "ResumeAnalysis.score is null" },
        summary = requireNotNull(summary) { "ResumeAnalysis.summary is null" },
        strengths = parseStrengths(requireNotNull(strengthsJson) { "ResumeAnalysis.strengthsJson is null" }, objectMapper),
        suggestions = parseSuggestions(requireNotNull(suggestionsJson) { "ResumeAnalysis.suggestionsJson is null" }, objectMapper),
        createdAt = requireNotNull(createdAt) { "ResumeAnalysis.createdAt is null" },
    )

private fun parseStrengths(value: JSONB, objectMapper: ObjectMapper): List<String> =
    objectMapper.readValue(value.data(), object : TypeReference<List<String>>() {})

private fun parseSuggestions(value: JSONB, objectMapper: ObjectMapper): List<ResumeSuggestion> =
    objectMapper.readValue(value.data(), object : TypeReference<List<ResumeSuggestion>>() {})
