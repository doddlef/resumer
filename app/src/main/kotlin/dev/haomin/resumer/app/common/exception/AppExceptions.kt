package dev.haomin.resumer.app.common.exception

import dev.haomin.resumer.app.common.response.ApiCode
import org.springframework.http.HttpStatus

/**
 * The base exception for all custom exceptions in the Secure Share application.
 *
 * @param code The response code associated with the exception.
 * @param message The detail message for the exception.
 * @param payload Additional data related to the exception.
 * @param status The HTTP status code for the exception.
 * @param cause The underlying cause of the exception.
 */
open class AppException(
    val code: ApiCode = ApiCode.FAILURE,
    message: String = code.description,
    val payload: Map<String, Any?> = emptyMap(),
    cause: Throwable? = null,
): RuntimeException(message, cause) {
    val status: HttpStatus get() = code.status
}

/**
 * Exception thrown when an invalid parameter is encountered.
 *
 * @param message The detail message for the exception.
 * @param cause The underlying cause of the exception.
 */
class InvalidParamException(
    message: String = ApiCode.INVALID_PARAM.description,
    cause: Throwable? = null,
): AppException(code = ApiCode.INVALID_PARAM, message = message, cause = cause)

/**
 * Exception thrown when a requested resource is not found.
 *
 * @param message The detail message for the exception.
 * @param cause The underlying cause of the exception.
 */
class ConflictException(
    message: String = ApiCode.CONFLICT.description,
    cause: Throwable? = null,
): AppException(code = ApiCode.CONFLICT, message = message, cause = cause)

/**
 * Exception thrown when a requested resource is not found.
 *
 * @param message The detail message for the exception.
 * @param cause The underlying cause of the exception.
 */
class NotFoundException(
    message: String = ApiCode.NOT_FOUND.description,
    cause: Throwable? = null,
) : AppException(code = ApiCode.NOT_FOUND, message = message, cause = cause)

/**
 * Exception thrown when an operation is forbidden due to insufficient permissions.
 *
 * @param message The detail message for the exception.
 * @param cause The underlying cause of the exception.
 */
class AccessDeniedException(
    message: String = ApiCode.ACCESS_DENIED.description,
    cause: Throwable? = null,
) : AppException(code = ApiCode.ACCESS_DENIED, message = message, cause = cause)

/**
 * Exception thrown when a database operation fails.
 *
 * @param message The detail message for the exception.
 * @param cause The underlying cause of the exception.
 */
class DatabaseOperationException(
    message: String = "Database operation failed",
    cause: Throwable? = null,
): AppException(code = ApiCode.FAILURE, message = message, cause = cause)