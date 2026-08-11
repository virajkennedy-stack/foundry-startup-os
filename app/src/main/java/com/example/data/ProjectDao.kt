package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE ownerId = :ownerId ORDER BY updatedAt DESC")
    fun getProjectsForUser(ownerId: String): Flow<List<ProjectEntity>>

    @Query("SELECT COUNT(*) FROM projects WHERE ownerId = :ownerId")
    suspend fun getProjectCountForUser(ownerId: String): Int

    @Query("SELECT * FROM projects WHERE projectId = :projectId AND ownerId = :ownerId")
    suspend fun getProjectById(projectId: String, ownerId: String): ProjectEntity?

    @Query("SELECT * FROM projects WHERE projectId = :projectId AND ownerId = :ownerId")
    fun observeProjectById(projectId: String, ownerId: String): Flow<ProjectEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjects(projects: List<ProjectEntity>)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE projectId = :projectId AND ownerId = :ownerId")
    suspend fun deleteProject(projectId: String, ownerId: String)

    @Query("DELETE FROM projects WHERE ownerId = :ownerId")
    suspend fun deleteAllProjectsForUser(ownerId: String)
}
