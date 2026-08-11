package com.example.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Central State Manager for User Sessions in Foundry.
 * Manages session lifecycle, tracks authentication states (Loading, Authenticated, Unauthenticated, Error),
 * and handles login/registration/logout operations with crash resilience and safety guarantees.
 */
class AuthSessionManager(
    private val userRepository: UserRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _sessionState = MutableStateFlow<AuthSessionState>(AuthSessionState.Loading)
    val sessionState: StateFlow<AuthSessionState> = _sessionState.asStateFlow()

    init {
        observeUserSession()
    }

    private fun observeUserSession() {
        scope.launch {
            _sessionState.value = AuthSessionState.Loading
            userRepository.activeUserFlow
                .catch { e ->
                    _sessionState.value = AuthSessionState.Error(
                        message = e.localizedMessage ?: "Failed to resolve active user session",
                        cause = e
                    )
                }
                .collect { user ->
                    if (user != null) {
                        _sessionState.value = AuthSessionState.Authenticated(user)
                    } else {
                        val activeUid = userRepository.activeUserId.value
                        if (activeUid.isNullOrBlank()) {
                            _sessionState.value = AuthSessionState.Unauthenticated
                        } else {
                            _sessionState.value = AuthSessionState.Loading
                        }
                    }
                }
        }
    }

    /**
     * Authenticate an existing user with email and password safely.
     */
    suspend fun login(emailRaw: String, passwordRaw: String): Result<UserEntity> {
        _sessionState.value = AuthSessionState.Loading
        return try {
            val result = userRepository.login(emailRaw, passwordRaw)
            if (result.isSuccess) {
                val user = result.getOrThrow()
                _sessionState.value = AuthSessionState.Authenticated(user)
            } else {
                val exception = result.exceptionOrNull() ?: Exception("Login failed. Please check credentials.")
                val friendlyMessage = exception.localizedMessage ?: "Login failed. Please try again."
                _sessionState.value = AuthSessionState.Error(
                    message = friendlyMessage,
                    cause = exception
                )
            }
            result
        } catch (e: Throwable) {
            val friendlyMsg = e.localizedMessage ?: "An unexpected error occurred during login."
            _sessionState.value = AuthSessionState.Error(
                message = friendlyMsg,
                cause = e
            )
            Result.failure(e)
        }
    }

    /**
     * Register a new user account with email and password safely.
     */
    suspend fun signUp(emailRaw: String, passwordRaw: String): Result<UserEntity> {
        _sessionState.value = AuthSessionState.Loading
        return try {
            val result = userRepository.signUp(emailRaw, passwordRaw)
            if (result.isSuccess) {
                val user = result.getOrThrow()
                _sessionState.value = AuthSessionState.Authenticated(user)
            } else {
                val exception = result.exceptionOrNull() ?: Exception("Registration failed.")
                val friendlyMessage = exception.localizedMessage ?: "Registration failed. Please try again."
                _sessionState.value = AuthSessionState.Error(
                    message = friendlyMessage,
                    cause = exception
                )
            }
            result
        } catch (e: Throwable) {
            val friendlyMsg = e.localizedMessage ?: "An unexpected error occurred during registration."
            _sessionState.value = AuthSessionState.Error(
                message = friendlyMsg,
                cause = e
            )
            Result.failure(e)
        }
    }

    /**
     * End active session and transition to Unauthenticated state safely.
     */
    suspend fun logout() {
        _sessionState.value = AuthSessionState.Loading
        try {
            userRepository.logout()
        } catch (e: Throwable) {
            // Ignore error during sign out to ensure unauthenticated state is reached regardless
        } finally {
            _sessionState.value = AuthSessionState.Unauthenticated
        }
    }

    /**
     * Send password reset email safely.
     */
    suspend fun sendPasswordResetEmail(emailRaw: String): Result<Unit> {
        return try {
            userRepository.sendPasswordResetEmail(emailRaw)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    /**
     * Reset or clear an active error state.
     */
    fun clearError() {
        if (_sessionState.value is AuthSessionState.Error) {
            val currentUid = userRepository.activeUserId.value
            if (currentUid.isNullOrBlank()) {
                _sessionState.value = AuthSessionState.Unauthenticated
            } else {
                _sessionState.value = AuthSessionState.Loading
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AuthSessionManager? = null

        fun getInstance(context: Context, userRepository: UserRepository): AuthSessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthSessionManager(userRepository).also { INSTANCE = it }
            }
        }
    }
}
