package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [UserEntity::class, ChatSessionEntity::class, ChatMessageEntity::class, ProjectEntity::class, UsageEntity::class, ExecutionPlanInterviewEntity::class, CustomPersonaEntity::class],
    version = 7,
    exportSchema = false
)
abstract class FoundryDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun chatDao(): ChatDao
    abstract fun projectDao(): ProjectDao
    abstract fun usageDao(): UsageDao
    abstract fun interviewDao(): InterviewDao
    abstract fun customPersonaDao(): CustomPersonaDao

    companion object {
        @Volatile
        private var INSTANCE: FoundryDatabase? = null

        fun getDatabase(context: Context): FoundryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FoundryDatabase::class.java,
                    "foundry_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
