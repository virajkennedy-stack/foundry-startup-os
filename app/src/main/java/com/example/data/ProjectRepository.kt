package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.util.UUID

class ProjectRepository(
    private val projectDao: ProjectDao,
    private val firestoreService: FirestoreService
) {
    fun getProjectsForUser(ownerId: String): Flow<List<ProjectEntity>> {
        if (ownerId.isBlank()) return flowOf(emptyList())
        return projectDao.getProjectsForUser(ownerId)
    }

    fun observeProjectById(ownerId: String, projectId: String): Flow<ProjectEntity?> {
        if (ownerId.isBlank() || projectId.isBlank()) return flowOf(null)
        return projectDao.observeProjectById(projectId, ownerId)
    }

    suspend fun getProjectById(ownerId: String, projectId: String): Result<ProjectEntity?> {
        if (ownerId.isBlank() || projectId.isBlank()) {
            return Result.failure(IllegalArgumentException("Owner ID and Project ID must not be blank"))
        }
        return try {
            val localProject = projectDao.getProjectById(projectId, ownerId)
            if (localProject != null) {
                Result.success(localProject)
            } else {
                val remoteProject = firestoreService.fetchProjectById(ownerId, projectId)
                if (remoteProject != null && remoteProject.ownerId == ownerId) {
                    projectDao.insertProject(remoteProject)
                    Result.success(remoteProject)
                } else {
                    Result.success(null)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createProject(
        ownerId: String,
        title: String,
        description: String = "",
        originalIdea: String = "",
        problem: String = "",
        goal: String = "",
        status: String = "ACTIVE",
        userPlan: UserPlan? = null,
        usageRepository: UsageRepository? = null
    ): Result<ProjectEntity> {
        if (ownerId.isBlank()) {
            return Result.failure(IllegalStateException("User is not authenticated"))
        }
        if (title.isBlank()) {
            return Result.failure(IllegalArgumentException("Project title cannot be empty"))
        }

        // Limit Enforcement Check
        if (userPlan != null) {
            val existingProjects = projectDao.getProjectCountForUser(ownerId)
            val limitInfo = EntitlementManager.getUsageLimit(userPlan, FeatureId.PROJECT_CREATION, existingProjects)
            if (!limitInfo.isUnlimited && limitInfo.remainingUsage <= 0) {
                return Result.failure(IllegalStateException("Project limit reached (${limitInfo.maxLimit} max on Free Tier). Upgrade to Pro for unlimited projects."))
            }
        }

        val now = System.currentTimeMillis()
        val project = ProjectEntity(
            projectId = UUID.randomUUID().toString(),
            ownerId = ownerId,
            title = title.trim(),
            description = description.trim(),
            originalIdea = originalIdea.trim(),
            problem = problem.trim(),
            goal = goal.trim(),
            status = status,
            createdAt = now,
            updatedAt = now
        )

        return try {
            projectDao.insertProject(project)
            firestoreService.saveProject(project)
            usageRepository?.recordSuccessfulUsage(ownerId, FeatureId.PROJECT_CREATION)
            Result.success(project)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProject(ownerId: String, project: ProjectEntity): Result<Unit> {
        if (ownerId.isBlank() || project.ownerId != ownerId) {
            return Result.failure(SecurityException("Unauthorized operation on project"))
        }
        if (project.title.isBlank()) {
            return Result.failure(IllegalArgumentException("Project title cannot be empty"))
        }

        val updated = project.copy(updatedAt = System.currentTimeMillis())
        return try {
            projectDao.updateProject(updated)
            firestoreService.saveProject(updated)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProject(ownerId: String, projectId: String): Result<Unit> {
        if (ownerId.isBlank() || projectId.isBlank()) {
            return Result.failure(IllegalArgumentException("Invalid parameters for deletion"))
        }

        return try {
            projectDao.deleteProject(projectId, ownerId)
            firestoreService.deleteProject(ownerId, projectId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncProjectsFromFirestore(ownerId: String): Result<List<ProjectEntity>> {
        if (ownerId.isBlank()) return Result.success(emptyList())

        return try {
            val remoteProjects = firestoreService.fetchProjectsForUser(ownerId)
            val filtered = remoteProjects.filter { it.ownerId == ownerId }
            if (filtered.isNotEmpty()) {
                projectDao.insertProjects(filtered)
            }
            Result.success(filtered)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
