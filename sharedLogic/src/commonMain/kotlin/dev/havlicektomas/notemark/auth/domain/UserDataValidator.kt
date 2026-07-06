package dev.havlicektomas.notemark.auth.domain

/** Snapshot of which password rules currently pass. */
data class PasswordValidationState(
    val hasMinLength: Boolean = false,
    val hasNumberOrSymbol: Boolean = false,
) {
    val isValid: Boolean
        get() = hasMinLength && hasNumberOrSymbol
}

/**
 * Client-side validation for the auth forms, matching the NoteMark spec:
 * username 3–20 chars, a basic email shape, password 8+ chars containing a
 * number or symbol. The backend itself only requires non-blank values, so
 * these rules are enforced here before any request is made.
 */
class UserDataValidator {

    fun isValidUsername(username: String): Boolean {
        val length = username.trim().length
        return length in MIN_USERNAME_LENGTH..MAX_USERNAME_LENGTH
    }

    fun isValidEmail(email: String): Boolean {
        return EMAIL_REGEX.matches(email.trim())
    }

    fun validatePassword(password: String): PasswordValidationState {
        val hasMinLength = password.length >= MIN_PASSWORD_LENGTH
        val hasNumberOrSymbol = password.any { it.isDigit() } ||
            password.any { !it.isLetterOrDigit() }
        return PasswordValidationState(
            hasMinLength = hasMinLength,
            hasNumberOrSymbol = hasNumberOrSymbol,
        )
    }

    companion object {
        const val MIN_USERNAME_LENGTH = 3
        const val MAX_USERNAME_LENGTH = 20
        const val MIN_PASSWORD_LENGTH = 8

        private val EMAIL_REGEX =
            Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}
