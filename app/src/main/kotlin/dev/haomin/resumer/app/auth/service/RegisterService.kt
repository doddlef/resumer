package dev.haomin.resumer.app.auth.service

import dev.haomin.resumer.app.auth.service.dto.CompleteRegistrationCmd
import dev.haomin.resumer.app.auth.service.dto.CompleteRegistrationResult
import dev.haomin.resumer.app.auth.service.dto.ResendVerificationCmd
import dev.haomin.resumer.app.auth.service.dto.SendVerificationCmd
import dev.haomin.resumer.app.auth.service.dto.SendVerificationResult
import dev.haomin.resumer.app.auth.service.dto.VerifyCodeCmd
import dev.haomin.resumer.app.auth.service.dto.VerifyCodeResult

/**
 * Service interface for managing the user registration process.
 *
 * Provides methods for handling various stages of the registration workflow,
 * including sending verification emails, verifying codes, completing registration,
 * and resending verification emails.
 */
interface RegisterService {

    /**
     * Sends a verification email to the specified email address.
     *
     * This method initiates the process of verifying an email address by
     * generating a verification attempt and sending a corresponding email
     * containing the verification code. The result includes the unique attempt
     * identifier and the duration in seconds for which the verification attempt
     * remains valid.
     *
     * @param cmd The command object containing the email address to which the
     *            verification email will be sent.
     * @return A result containing the unique attempt ID and the expiration time
     *         in seconds for the verification attempt.
     */
    fun sendVerificationEmail(cmd: SendVerificationCmd): SendVerificationResult

    /**
     * Verifies the correctness of a given verification code for a specific attempt.
     *
     * This method validates the provided verification code against the stored code
     * associated with the given attempt ID. If the code is correct, the verification
     * is marked as successful, and the verification timestamp is recorded. If the
     * code is invalid or the attempt has expired, the verification fails.
     *
     * @param cmd An instance of `VerifyCodeCmd` containing the details of the verification
     *            attempt, including the unique attempt ID and the code to validate.
     * @return A `VerifyCodeResult` indicating whether the verification was successful
     *         (`verified` field) and the timestamp of successful verification, if applicable.
     */
    fun verifyCode(cmd: VerifyCodeCmd): VerifyCodeResult

    /**
     * Completes the registration process by finalizing the account setup.
     *
     * This method processes the provided registration command, validates
     * the input, and creates a new account using the specified details.
     * It returns the result containing information about the newly
     * created account.
     *
     * @param cmd The command object containing the details required to
     *            complete the registration process. This includes:
     *            - attemptId: The unique ID of the registration attempt.
     *            - nickname: The chosen nickname for the account.
     *            - password: The account password.
     * @return A result object containing information about the newly
     *         created account, including:
     *         - accountId: The unique identifier of the account.
     *         - email: The email address associated with the account.
     *         - nickname: The nickname of the account.
     */
    fun completeRegistration(cmd: CompleteRegistrationCmd): CompleteRegistrationResult

    /**
     * Resends a verification email for a previously initiated registration attempt.
     *
     * This method allows users to request the resending of a verification email
     * associated with a specific registration attempt. The result includes the unique
     * attempt identifier and the duration (in seconds) for which the verification attempt
     * remains valid.
     *
     * @param cmd The command object containing the details of the resend request.
     *            It includes the `attemptId` identifying the registration attempt
     *            for which the verification email will be resent.
     * @return A result object containing the unique attempt ID (`attemptId`)
     *         and the expiration time in seconds (`expiresInSeconds`) for the verification attempt.
     */
    fun resendVerification(cmd: ResendVerificationCmd): SendVerificationResult
}
