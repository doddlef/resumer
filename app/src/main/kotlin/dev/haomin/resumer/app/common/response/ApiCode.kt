package dev.haomin.resumer.app.common.response

import org.springframework.http.HttpStatus

/**
 *  Standard response codes
 *
 * @param code the integer code representing the response status
 * @param description human-readable description of the response code
 * @param status the corresponding HTTP status at default
 */
enum class ApiCode(val code: Int, val description: String, val status: HttpStatus) {
    // ===== Success =====
    SUCCESS(0, "success" ,HttpStatus.OK),


    // ===== General failure =====
    FAILURE(1,  "something went wrong",HttpStatus.INTERNAL_SERVER_ERROR),


    // ===== Common client errors =====
    /** Invalid Parameter Exception, e.g., give "123456" as email */
    INVALID_PARAM(1001, "parameter is invalid",HttpStatus.BAD_REQUEST),

    /** Resource Conflict Exception, e.g., register with the same email twice */
    CONFLICT(1002, "resource conflict",HttpStatus.CONFLICT),

    /** Too Many Requests Exception, e.g., exceed rate limit */
    TOO_MANY_REQUESTS(1003, "too many requests",HttpStatus.TOO_MANY_REQUESTS),

    /** Resource is not found */
    NOT_FOUND(1004, "resource not found", HttpStatus.NOT_FOUND),


    // ===== Authentication and Authorization errors =====
    /** General Authentication Exception */
    AUTH_FAILED(1100, "failed to authenticate",HttpStatus.UNAUTHORIZED),

    /** Provided authentication is invalid, e.g., invalid token */
    INVALID_AUTH(1101, "invalid authentication",HttpStatus.UNAUTHORIZED),

    /** Provided authentication is expired, e.g., expired token */
    EXPIRED_AUTH(1102, "authentication expired",HttpStatus.UNAUTHORIZED),

    /** No authentication provided */
    REQUIRE_AUTH(1103, "authentication required",HttpStatus.UNAUTHORIZED),

    /** Access is denied, e.g., no permission to access a resource */
    ACCESS_DENIED(1104, "access denied",HttpStatus.FORBIDDEN),
}
