package dev.haomin.resumer.app.auth.exception

import dev.haomin.resumer.app.common.exception.AppException
import dev.haomin.resumer.app.common.response.ApiCode

/**
 * Exception class for authentication-related errors.
 *
 * This exception is thrown when authentication fails due to reasons such as
 * invalid credentials, expired tokens, or lack of required authentication. It
 * extends the base `AppException` class and sets the `AUTH_FAILED` response
 * code by default.
 *
 * @constructor Creates a new instance of `AuthExceptions`.
 * @param code The API response code for the exception (default is `ApiCode.FAILURE`).
 * @param message A custom error message for the exception (default is the description of `code`).
 * @param payload Additional contextual data related to the exception (default is an empty map).
 * @param cause The root cause of the exception (default is null).
 */
open class AuthExceptions(
    code: ApiCode = ApiCode.FAILURE,
    message: String = code.description,
    payload: Map<String, Any?> = emptyMap(),
    cause: Throwable? = null,
): AppException(
    code = ApiCode.AUTH_FAILED,
    message = ApiCode.AUTH_FAILED.description,
    payload = payload,
    cause = cause
)

/**
 * Exception thrown when an action requires authentication, but none is provided.
 *
 * This class extends `AuthExceptions` and automatically sets the error code to
 * `ApiCode.REQUIRE_AUTH`, indicating that authentication is mandatory to
 * access the requested resource or perform the desired operation.
 *
 * @constructor Creates an `AuthRequiredException` with an optional custom message.
 * @param message The error message associated with the exception (default is "authentication required").
 */
class AuthRequiredException(
    message: String = "authentication required",
) : AuthExceptions(code = ApiCode.REQUIRE_AUTH, message = message)

/**
 * Thrown when an authentication flow references a non-existing account.
 */
class AuthAccountNotFoundException(
    message: String = "account not found",
) : AuthExceptions(code = ApiCode.NOT_FOUND, message = message)

/**
 * Thrown when provided authentication material is invalid.
 */
class InvalidAuthenticationException(
    message: String = ApiCode.INVALID_AUTH.description,
) : AuthExceptions(code = ApiCode.INVALID_AUTH, message = message)

/**
 * Thrown when authentication material is valid in structure but expired.
 */
class ExpiredAuthenticationException(
    message: String = ApiCode.EXPIRED_AUTH.description,
) : AuthExceptions(code = ApiCode.EXPIRED_AUTH, message = message)
