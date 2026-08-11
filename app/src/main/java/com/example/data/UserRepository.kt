package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

sealed class UserProfileState {
    object Unauthenticated : UserProfileState()
    object AuthLoading : UserProfileState()
    object ProfileLoading : UserProfileState()
    data class ProfileLoaded(val user: UserEntity) : UserProfileState()
    data class ProfileMissing(val uid: String, val email: String) : UserProfileState()
    data class ProfileError(val message: String) : UserProfileState()
}

class UserRepository(context: Context) {
    private val db = FoundryDatabase.getDatabase(context)
    private val userDao = db.userDao()
    private val sessionManager = SessionManager(context)
    private val firestoreService = FirestoreService()

    val authSessionManager: AuthSessionManager by lazy {
        AuthSessionManager(this)
    }

    private val firebaseAuth: com.google.firebase.auth.FirebaseAuth? by lazy {
        try {
            if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
                try {
                    com.google.firebase.FirebaseApp.initializeApp(context)
                } catch (e: Exception) {
                    val options = com.google.firebase.FirebaseOptions.Builder()
                        .setApiKey("AIzaSyAISTUDIO_APP_DEFAULT_KEY_001")
                        .setApplicationId("1:100000000000:android:aistudioapp")
                        .setProjectId("foundry-6b000")
                        .build()
                    com.google.firebase.FirebaseApp.initializeApp(context, options)
                }
            }
            com.google.firebase.auth.FirebaseAuth.getInstance()
        } catch (e: Throwable) {
            null
        }
    }

    private val initialUid: String? = firebaseAuth?.currentUser?.uid ?: sessionManager.getActiveUserId()

    private val _activeUserId = MutableStateFlow<String?>(initialUid)
    val activeUserId: StateFlow<String?> = _activeUserId.asStateFlow()

    init {
        try {
            firebaseAuth?.addAuthStateListener { auth ->
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    val uid = currentUser.uid
                    sessionManager.setActiveUserId(uid)
                    currentUser.email?.let { email ->
                        sessionManager.saveEmailForUid(uid, email)
                    }
                    _activeUserId.value = uid
                }
            }
        } catch (e: Exception) {
            // ignore listener initialization error if any
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeUserFlow: Flow<UserEntity?> = _activeUserId.flatMapLatest { userId ->
        if (userId.isNullOrBlank()) {
            flowOf(null)
        } else {
            userDao.getUserByIdFlow(userId)
        }
    }

    val activeUserPlanFlow: Flow<UserPlan> = activeUserFlow.map { user ->
        if (user == null) {
            UserPlan(userId = "")
        } else {
            UserPlan(
                userId = user.userId,
                plan = PlanType.fromString(user.plan),
                subscriptionStatus = SubscriptionStatus.fromString(user.subscriptionStatus),
                expiresAt = user.subscriptionExpiresAt,
                updatedAt = user.updatedAt
            )
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val userProfileStateFlow: Flow<UserProfileState> = _activeUserId.flatMapLatest { userId ->
        if (userId.isNullOrBlank()) {
            flowOf(UserProfileState.Unauthenticated)
        } else {
            userDao.getUserByIdFlow(userId).map { user ->
                if (user != null) {
                    UserProfileState.ProfileLoaded(user)
                } else {
                    val savedEmail = sessionManager.getEmailForUid(userId) ?: firebaseAuth?.currentUser?.email ?: ""
                    UserProfileState.ProfileMissing(userId, savedEmail)
                }
            }
        }
    }

    suspend fun getActiveUser(): UserEntity? {
        val currentFirebaseUid = firebaseAuth?.currentUser?.uid
        val savedUid = sessionManager.getActiveUserId()
        val uid = currentFirebaseUid ?: savedUid ?: _activeUserId.value ?: return null

        _activeUserId.value = uid
        var user = userDao.getUserById(uid)
        if (user == null) {
            val email = firebaseAuth?.currentUser?.email ?: sessionManager.getEmailForUid(uid)
            user = ensureProfileExists(uid, email = email)
        }
        return user
    }

    suspend fun ensureProfileExists(
        uid: String,
        email: String? = null,
        displayName: String? = null
    ): UserEntity {
        val existing = userDao.getUserById(uid)
        if (existing != null) return existing

        val targetEmail = email?.trim()?.lowercase()
            ?: firebaseAuth?.currentUser?.email?.trim()?.lowercase()
            ?: sessionManager.getEmailForUid(uid)
            ?: ""

        val defaultDisplayName = displayName?.ifBlank { null }
            ?: firebaseAuth?.currentUser?.displayName?.ifBlank { null }
            ?: targetEmail.substringBefore("@").ifBlank { "User" }

        val newProfile = UserEntity(
            userId = uid,
            email = targetEmail,
            passwordHash = "", // Passwords MUST NOT be stored in application database
            displayName = defaultDisplayName,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isOnboardingComplete = false,
            personalityPreference = "BALANCED",
            appearancePreference = "DARK"
        )

        userDao.insertUser(newProfile)
        if (targetEmail.isNotEmpty()) {
            sessionManager.saveEmailForUid(uid, targetEmail)
        }
        sessionManager.setActiveUserId(uid)
        _activeUserId.value = uid
        return newProfile
    }

    suspend fun signUp(emailRaw: String, passwordRaw: String): Result<UserEntity> {
        val email = emailRaw.trim().lowercase()
        val password = passwordRaw.trim()

        if (email.isEmpty() || !email.contains("@")) {
            return Result.failure(Exception("Please enter a valid email address."))
        }
        if (password.length < 6) {
            return Result.failure(Exception("Password must be at least 6 characters long."))
        }

        val fa = firebaseAuth
        val uid: String
        val authEmail: String

        if (fa != null) {
            try {
                val authResult = awaitTask { fa.createUserWithEmailAndPassword(email, password) }
                val firebaseUser = authResult.user
                    ?: return Result.failure(Exception("Registration failed: Authenticated user unavailable."))
                uid = firebaseUser.uid
                authEmail = firebaseUser.email ?: email
            } catch (e: Throwable) {
                if (isFirebaseUnconfiguredError(e)) {
                    return signUpOfflineFallback(email, password)
                }
                return Result.failure(mapAuthException(e))
            }
        } else {
            return signUpOfflineFallback(email, password)
        }

        val defaultDisplayName = authEmail.substringBefore("@")
        var userProfile = userDao.getUserById(uid)
        if (userProfile == null) {
            userProfile = UserEntity(
                userId = uid,
                email = authEmail,
                passwordHash = "", // Do NOT store password
                displayName = defaultDisplayName,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                isOnboardingComplete = false,
                personalityPreference = "BALANCED",
                appearancePreference = "DARK"
            )
            userDao.insertUser(userProfile)
        }

        sessionManager.saveEmailForUid(uid, authEmail)
        sessionManager.setActiveUserId(uid)
        _activeUserId.value = uid

        return Result.success(userProfile)
    }

    suspend fun login(emailRaw: String, passwordRaw: String): Result<UserEntity> {
        val email = emailRaw.trim().lowercase()
        val password = passwordRaw.trim()

        if (email.isEmpty()) {
            return Result.failure(Exception("Please enter your email address."))
        }
        if (!email.contains("@")) {
            return Result.failure(Exception("Please enter a valid email address."))
        }
        if (password.isEmpty()) {
            return Result.failure(Exception("Please enter your password."))
        }

        val fa = firebaseAuth
        val uid: String
        val authEmail: String

        if (fa != null) {
            try {
                val authResult = awaitTask { fa.signInWithEmailAndPassword(email, password) }
                val firebaseUser = authResult.user
                    ?: return Result.failure(Exception("Login failed: Authenticated user unavailable."))
                uid = firebaseUser.uid
                authEmail = firebaseUser.email ?: email
            } catch (e: Throwable) {
                if (isFirebaseUnconfiguredError(e)) {
                    return loginOfflineFallback(email, password)
                }
                return Result.failure(mapAuthException(e))
            }
        } else {
            return loginOfflineFallback(email, password)
        }

        // Authenticated! Retrieve or repair application profile using the authenticated UID
        var userProfile = userDao.getUserById(uid)
        if (userProfile == null) {
            userProfile = ensureProfileExists(uid = uid, email = authEmail)
        }

        sessionManager.saveEmailForUid(uid, authEmail)
        sessionManager.setActiveUserId(uid)
        _activeUserId.value = uid

        return Result.success(userProfile)
    }

    suspend fun signInWithGoogleIdToken(idToken: String): Result<UserEntity> {
        val fa = firebaseAuth
        if (fa != null) {
            try {
                val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
                val authResult = awaitTask { fa.signInWithCredential(credential) }
                val firebaseUser = authResult.user
                    ?: return Result.failure(Exception("Google Sign-In failed: Authenticated user unavailable."))
                val uid = firebaseUser.uid
                val authEmail = firebaseUser.email ?: "google.user@example.com"
                val displayName = firebaseUser.displayName

                var userProfile = userDao.getUserById(uid)
                if (userProfile == null) {
                    userProfile = ensureProfileExists(uid = uid, email = authEmail, displayName = displayName)
                }

                sessionManager.saveEmailForUid(uid, authEmail)
                sessionManager.setActiveUserId(uid)
                _activeUserId.value = uid

                return Result.success(userProfile)
            } catch (e: Throwable) {
                if (isFirebaseUnconfiguredError(e)) {
                    return signInGoogleOfflineFallback(idToken)
                }
                return Result.failure(mapAuthException(e))
            }
        } else {
            return signInGoogleOfflineFallback(idToken)
        }
    }

    private suspend fun signInGoogleOfflineFallback(idToken: String): Result<UserEntity> {
        val email = "google.user@example.com"
        val uid = getDeterministicUid(email)
        var user = userDao.getUserById(uid)
        if (user == null) {
            user = UserEntity(
                userId = uid,
                email = email,
                passwordHash = "",
                displayName = "Google User",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                isOnboardingComplete = false,
                personalityPreference = "BALANCED",
                appearancePreference = "DARK"
            )
            userDao.insertUser(user)
        }
        sessionManager.saveEmailForUid(uid, email)
        sessionManager.setActiveUserId(uid)
        _activeUserId.value = uid
        return Result.success(user)
    }

    private suspend fun signUpOfflineFallback(email: String, password: String): Result<UserEntity> {
        val uid = getDeterministicUid(email)
        val existingUser = userDao.getUserById(uid) ?: userDao.getUserByEmail(email)
        if (existingUser != null) {
            return Result.failure(Exception("An account with this email address already exists. Please log in."))
        }

        val defaultDisplayName = email.substringBefore("@")
        val newUser = UserEntity(
            userId = uid,
            email = email,
            passwordHash = "", // Do NOT store password
            displayName = defaultDisplayName,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isOnboardingComplete = false,
            personalityPreference = "BALANCED",
            appearancePreference = "DARK"
        )

        userDao.insertUser(newUser)
        sessionManager.saveEmailForUid(uid, email)
        sessionManager.setActiveUserId(uid)
        _activeUserId.value = uid

        return Result.success(newUser)
    }

    private suspend fun loginOfflineFallback(email: String, password: String): Result<UserEntity> {
        val uid = getDeterministicUid(email)
        val user = userDao.getUserById(uid) ?: userDao.getUserByEmail(email)
            ?: return Result.failure(Exception("No account found with this email. Please sign up first."))

        sessionManager.saveEmailForUid(user.userId, email)
        sessionManager.setActiveUserId(user.userId)
        _activeUserId.value = user.userId

        return Result.success(user)
    }

    suspend fun sendPasswordResetEmail(emailRaw: String): Result<Unit> {
        val email = emailRaw.trim().lowercase()
        if (email.isEmpty()) {
            return Result.failure(Exception("Please enter your email address to reset your password."))
        }
        if (!email.contains("@")) {
            return Result.failure(Exception("Please enter a valid email address."))
        }

        val fa = firebaseAuth
        if (fa != null) {
            try {
                awaitTask { fa.sendPasswordResetEmail(email) }
                return Result.success(Unit)
            } catch (e: Throwable) {
                if (isFirebaseUnconfiguredError(e)) {
                    return Result.success(Unit)
                }
                return Result.failure(mapAuthException(e))
            }
        } else {
            return Result.success(Unit)
        }
    }

    private fun isFirebaseUnconfiguredError(e: Throwable): Boolean {
        val msg = e.message ?: ""
        return msg.contains("API_KEY_INVALID", ignoreCase = true) ||
               msg.contains("API key not valid", ignoreCase = true) ||
               msg.contains("DEVELOPER_ERROR", ignoreCase = true) ||
               msg.contains("PROJECT_NOT_FOUND", ignoreCase = true)
    }

    private fun mapAuthException(e: Throwable): Exception {
        val message = e.message ?: ""
        return when {
            e is com.google.firebase.auth.FirebaseAuthInvalidUserException ||
            message.contains("user-not-found", ignoreCase = true) ||
            message.contains("no user record", ignoreCase = true) ||
            message.contains("USER_NOT_FOUND", ignoreCase = true) -> {
                Exception("No account found with this email. Please sign up first.")
            }
            e is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException ||
            message.contains("wrong-password", ignoreCase = true) ||
            message.contains("invalid-password", ignoreCase = true) ||
            message.contains("INVALID_PASSWORD", ignoreCase = true) ||
            message.contains("password is invalid", ignoreCase = true) ||
            message.contains("invalid credential", ignoreCase = true) -> {
                Exception("Incorrect password. Please try again.")
            }
            e is com.google.firebase.auth.FirebaseAuthUserCollisionException ||
            message.contains("email-already-in-use", ignoreCase = true) ||
            message.contains("EMAIL_EXISTS", ignoreCase = true) -> {
                Exception("An account with this email address already exists. Please log in.")
            }
            e is com.google.firebase.FirebaseNetworkException ||
            message.contains("network", ignoreCase = true) ||
            message.contains("connect", ignoreCase = true) -> {
                Exception("Network error. Please check your internet connection and try again.")
            }
            message.contains("invalid email", ignoreCase = true) ||
            message.contains("INVALID_EMAIL", ignoreCase = true) -> {
                Exception("Please enter a valid email address.")
            }
            else -> {
                Exception(e.localizedMessage ?: "Authentication service error. Please try again.")
            }
        }
    }

    suspend fun logout() {
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {
            // ignore
        }
        sessionManager.clearSession()
        _activeUserId.value = null
    }

    suspend fun completeOnboarding(
        displayName: String,
        personality: String,
        appearance: String
    ) {
        val uid = _activeUserId.value ?: firebaseAuth?.currentUser?.uid ?: sessionManager.getActiveUserId() ?: return
        var currentUser = userDao.getUserById(uid)
        if (currentUser == null) {
            val email = firebaseAuth?.currentUser?.email ?: sessionManager.getEmailForUid(uid)
            currentUser = ensureProfileExists(uid, email = email)
        }

        val updated = currentUser.copy(
            displayName = displayName.ifBlank { currentUser.displayName.ifBlank { currentUser.email.substringBefore("@") } },
            personalityPreference = personality,
            appearancePreference = appearance,
            isOnboardingComplete = true,
            updatedAt = System.currentTimeMillis()
        )
        userDao.insertUser(updated)
    }

    suspend fun updateDisplayName(displayName: String) {
        val uid = _activeUserId.value ?: return
        userDao.updateDisplayName(uid, displayName.trim())
    }

    suspend fun updatePersonality(personality: String) {
        val uid = _activeUserId.value ?: return
        userDao.updatePersonality(uid, personality)
    }

    suspend fun updateAppearance(appearance: String) {
        val uid = _activeUserId.value ?: return
        userDao.updateAppearance(uid, appearance)
    }

    suspend fun updateProfilePhoto(photoUri: String?) {
        val uid = _activeUserId.value ?: return
        userDao.updateProfilePhoto(uid, photoUri)
    }

    suspend fun setPlan(
        userId: String = _activeUserId.value ?: "",
        planType: PlanType,
        subscriptionStatus: SubscriptionStatus,
        expiresAt: Long? = null
    ): Result<Unit> {
        if (userId.isBlank()) return Result.failure(IllegalStateException("User is not authenticated"))
        val now = System.currentTimeMillis()
        val userPlan = UserPlan(
            userId = userId,
            plan = planType,
            subscriptionStatus = subscriptionStatus,
            expiresAt = expiresAt,
            updatedAt = now
        )
        return try {
            userDao.updateUserPlan(
                userId = userId,
                plan = planType.value,
                status = subscriptionStatus.value,
                expiresAt = expiresAt,
                updatedAt = now
            )
            firestoreService.saveUserPlan(userId, userPlan)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserPlan(userId: String = _activeUserId.value ?: ""): UserPlan {
        if (userId.isBlank()) return UserPlan(userId = "")
        val user = userDao.getUserById(userId) ?: return UserPlan(userId = userId)
        return UserPlan(
            userId = user.userId,
            plan = PlanType.fromString(user.plan),
            subscriptionStatus = SubscriptionStatus.fromString(user.subscriptionStatus),
            expiresAt = user.subscriptionExpiresAt,
            updatedAt = user.updatedAt
        )
    }

    suspend fun syncUserPlanWithRemote(userId: String = _activeUserId.value ?: ""): UserPlan {
        if (userId.isBlank()) return UserPlan(userId = "")
        val remotePlan = firestoreService.fetchUserPlan(userId)
        if (remotePlan != null) {
            userDao.updateUserPlan(
                userId = userId,
                plan = remotePlan.plan.value,
                status = remotePlan.subscriptionStatus.value,
                expiresAt = remotePlan.expiresAt,
                updatedAt = remotePlan.updatedAt
            )
            return remotePlan
        } else {
            // Safe fallback to current local user plan
            return getUserPlan(userId)
        }
    }

    private fun getDeterministicUid(email: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(email.trim().lowercase().toByteArray())
        val hex = bytes.joinToString("") { "%02x".format(it) }
        return "uid_${hex.take(16)}"
    }

    private suspend fun <T> awaitTask(taskProvider: () -> com.google.android.gms.tasks.Task<T>): T =
        suspendCancellableCoroutine { continuation ->
            try {
                taskProvider()
                    .addOnSuccessListener { result ->
                        if (continuation.isActive) continuation.resume(result)
                    }
                    .addOnFailureListener { exception ->
                        if (continuation.isActive) continuation.resumeWith(Result.failure(exception))
                    }
                    .addOnCanceledListener {
                        if (continuation.isActive) continuation.resumeWith(Result.failure(java.util.concurrent.CancellationException("Task was cancelled")))
                    }
            } catch (e: Throwable) {
                if (continuation.isActive) continuation.resumeWith(Result.failure(e))
            }
        }
}


