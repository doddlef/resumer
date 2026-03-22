package dev.haomin.resumer.app.framework.advice

import dev.haomin.resumer.app.common.exception.AppException
import dev.haomin.resumer.app.common.response.ApiCode
import dev.haomin.resumer.app.common.response.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.BindException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

import org.slf4j.LoggerFactory

/**
 * Global exception advice for transforming server-side exceptions into standardized API responses.
 */
@RestControllerAdvice
class GeneralAdvice {

    private val log = LoggerFactory.getLogger(GeneralAdvice::class.java)

    /**
     * Handles domain-level application exceptions.
     *
     * Response uses exception-defined code/message/payload/status.
     */
    @ExceptionHandler(AppException::class)
    fun handleAppException(ex: AppException): ResponseEntity<ApiResponse> {
        val responseCode = ex.code
        log.debug(
            "Handling AppException with code={}, status={}, detail={}",
            responseCode.code,
            responseCode.status,
            ex.message,
        )

        val body = ApiResponse.builder(responseCode)
            .message(ex.message ?: responseCode.description)
            .withAll(ex.payload)
            .build()
        return ResponseEntity.status(ex.status).body(body)
    }

    /**
     * Handles malformed requests and bean validation failures.
     */
    @ExceptionHandler(
        MethodArgumentNotValidException::class,
        BindException::class,
        MissingServletRequestParameterException::class,
        HttpMessageNotReadableException::class,
    )
    fun handleBadRequest(ex: Exception): ResponseEntity<ApiResponse> {
        log.warn("Bad request exception: {}", ex.message)
        return buildResponse(
            code = ApiCode.INVALID_PARAM,
            status = ApiCode.INVALID_PARAM.status,
        )
    }

    /**
     * Handles all unexpected exceptions.
     */
    @ExceptionHandler(Exception::class)
    fun handleUnexpectedException(ex: Exception): ResponseEntity<ApiResponse> {
        log.error("Unexpected server exception", ex)
        return buildResponse(
            code = ApiCode.FAILURE,
            status = HttpStatus.INTERNAL_SERVER_ERROR,
        )
    }

    private fun buildResponse(code: ApiCode, status: HttpStatus): ResponseEntity<ApiResponse> {
        val body = ApiResponse.builder(code)
            .message(code.description)
            .build()
        return ResponseEntity.status(status).body(body)
    }
}
