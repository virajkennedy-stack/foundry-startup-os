package com.example.data

/**
 * Sealed class representing the current state of a user's session in Foundry.
 */
sealed class AuthSessionState {
    /** Session restoration or auth operation in progress. */
    object Loading : AuthSessionState()

    /** User is authenticated with a valid profile loaded. */
    data class Authenticated(val user: UserEntity) : AuthSessionState()

    /** No active user session exists. */
    object Unauthenticated : AuthSessionState()

    /** An error occurred during auth verification, login, or registration. */
    data class Error(val message: String, val cause: Throwable? = null) : AuthSessionState()
}
