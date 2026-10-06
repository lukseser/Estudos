package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.CalendarDao
import com.example.data.local.dao.CurriculumDao
import com.example.data.local.dao.StudyDao
import com.example.data.local.dao.TaskDao
import com.example.data.local.entity.CalendarEventEntity
import com.example.data.local.entity.FlashcardEntity
import com.example.data.local.entity.ModuleEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.QuestionEntity
import com.example.data.local.entity.SemesterEntity
import com.example.data.local.entity.SlideBlockEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TaskEntity

@Database(
    entities = [
        SemesterEntity::class,
        ModuleEntity::class,
        SubjectEntity::class,
        TaskEntity::class,
        CalendarEventEntity::class,
        NoteEntity::class,
        SlideBlockEntity::class,
        QuestionEntity::class,
        FlashcardEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun curriculumDao(): CurriculumDao
    abstract fun taskDao(): TaskDao
    abstract fun calendarDao(): CalendarDao
    abstract fun studyDao(): StudyDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "study_planner_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
