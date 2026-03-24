package dev.haomin.resumer.app.module.session.mq

import dev.haomin.resumer.app.infra.mq.StreamSerde
import dev.haomin.resumer.app.infra.mq.model.MessageStatus
import dev.haomin.resumer.app.infra.mq.model.MessageWrapper
import dev.haomin.resumer.app.module.session.mq.model.AdviceJobType
import dev.haomin.resumer.app.module.session.mq.model.SessionAdviceJobPayload
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class SessionAdviceMessageSerde : StreamSerde<SessionAdviceJobPayload> {

    override fun toFields(message: MessageWrapper<SessionAdviceJobPayload>): Map<String, String> =
        mapOf(
            FIELD_ID to message.id,
            FIELD_TYPE to message.type,
            FIELD_ATTEMPT to message.attempt.toString(),
            FIELD_STATUS to message.status.name,
            FIELD_TRACE_ID to message.traceId,
            FIELD_CREATED_AT_EPOCH_MS to message.createdAtEpochMs.toString(),
            FIELD_UPDATED_AT_EPOCH_MS to message.updatedAtEpochMs.toString(),
            FIELD_PAYLOAD_SESSION_ID to message.payload.sessionId.toString(),
            FIELD_PAYLOAD_ACCOUNT_ID to message.payload.accountId.toString(),
            FIELD_PAYLOAD_JOB_TYPE to message.payload.jobType.name,
        )

    override fun fromFields(fields: Map<String, String>): MessageWrapper<SessionAdviceJobPayload> {
        val payload = SessionAdviceJobPayload(
            sessionId = UUID.fromString(fields.getValue(FIELD_PAYLOAD_SESSION_ID)),
            accountId = UUID.fromString(fields.getValue(FIELD_PAYLOAD_ACCOUNT_ID)),
            jobType = AdviceJobType.valueOf(fields.getValue(FIELD_PAYLOAD_JOB_TYPE)),
        )
        return MessageWrapper(
            id = fields.getValue(FIELD_ID),
            type = fields.getValue(FIELD_TYPE),
            payload = payload,
            attempt = fields.getValue(FIELD_ATTEMPT).toInt(),
            status = MessageStatus.valueOf(fields.getValue(FIELD_STATUS)),
            traceId = fields.getValue(FIELD_TRACE_ID),
            createdAtEpochMs = fields.getValue(FIELD_CREATED_AT_EPOCH_MS).toLong(),
            updatedAtEpochMs = fields.getValue(FIELD_UPDATED_AT_EPOCH_MS).toLong(),
        )
    }

    override fun toJson(message: MessageWrapper<SessionAdviceJobPayload>): String =
        """
        {
            "id":"${escape(message.id)}",
            "type":"${escape(message.type)}",
            "attempt":${message.attempt},
            "status":"${message.status.name}",
            "traceId":"${escape(message.traceId)}",
            "createdAtEpochMs":${message.createdAtEpochMs},
            "updatedAtEpochMs":${message.updatedAtEpochMs},
            "payload":{
                "sessionId":"${message.payload.sessionId}",
                "accountId":"${message.payload.accountId}",
                "jobType":"${message.payload.jobType.name}"
            }
        }
        """.trimIndent()

    private fun escape(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"")

    companion object {
        const val FIELD_ID = "id"
        const val FIELD_TYPE = "type"
        const val FIELD_ATTEMPT = "attempt"
        const val FIELD_STATUS = "status"
        const val FIELD_TRACE_ID = "traceId"
        const val FIELD_CREATED_AT_EPOCH_MS = "createdAtEpochMs"
        const val FIELD_UPDATED_AT_EPOCH_MS = "updatedAtEpochMs"
        const val FIELD_PAYLOAD_SESSION_ID = "sessionId"
        const val FIELD_PAYLOAD_ACCOUNT_ID = "accountId"
        const val FIELD_PAYLOAD_JOB_TYPE = "jobType"
    }
}
