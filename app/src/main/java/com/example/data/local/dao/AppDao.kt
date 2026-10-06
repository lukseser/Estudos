package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CalendarEventEntity
import com.example.data.local.entity.FlashcardEntity
import com.example.data.local.entity.ModuleEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.QuestionEntity
import com.example.data.local.entity.SemesterEntity
import com.example.data.local.entity.SlideBlockEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CurriculumDao {
    @Query("SELECT * FROM semesters ORDER BY orderIndex ASC, id ASC")
    fun getAllSemesters(): Flow<List<SemesterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSemester(semester: SemesterEntity): Long

    @Update
    suspend fun updateSemester(semester: SemesterEntity)

    @Delete
    suspend fun deleteSemester(semester: SemesterEntity)

    @Query("SELECT * FROM modules ORDER BY orderIndex ASC, id ASC")
    fun getAllModules(): Flow<List<ModuleEntity>>

    @Query("SELECT * FROM modules WHERE semesterId = :semesterId ORDER BY orderIndex ASC, id ASC")
    fun getModulesForSemester(semesterId: Long): Flow<List<ModuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModule(module: ModuleEntity): Long

    @Delete
    suspend fun deleteModule(module: ModuleEntity)

    @Query("DELETE FROM modules WHERE semesterId = :semesterId")
    suspend fun deleteModulesBySemester(semesterId: Long)

    @Query("SELECT * FROM subjects ORDER BY orderIndex ASC, id ASC")
    fun getAllSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE moduleId = :moduleId ORDER BY orderIndex ASC, id ASC")
    fun getSubjectsForModule(moduleId: Long): Flow<List<SubjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity): Long

    @Delete
    suspend fun deleteSubject(subject: SubjectEntity)

    @Query("DELETE FROM subjects WHERE moduleId = :moduleId")
    suspend fun deleteSubjectsByModule(moduleId: Long)

    @Query("SELECT COUNT(*) FROM semesters")
    suspend fun getSemesterCount(): Int
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, dueDate ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dueDate = :date")
    fun getTasksForDate(date: String): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)
}

@Dao
interface CalendarDao {
    @Query("SELECT * FROM calendar_events ORDER BY eventDate ASC, time ASC")
    fun getAllEvents(): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events WHERE eventDate = :date ORDER BY time ASC")
    fun getEventsForDate(date: String): Flow<List<CalendarEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: CalendarEventEntity): Long

    @Delete
    suspend fun deleteEvent(event: CalendarEventEntity)
}

@Dao
interface StudyDao {
    // Anotações
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE subjectId = :subjectId ORDER BY updatedAt DESC")
    fun getNotesForSubject(subjectId: Long): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    // Slide blocks
    @Query("SELECT * FROM slide_blocks WHERE noteId = :noteId ORDER BY pageIndex ASC, id ASC")
    fun getSlideBlocks(noteId: Long): Flow<List<SlideBlockEntity>>

    @Query("SELECT * FROM slide_blocks WHERE noteId = :noteId ORDER BY pageIndex ASC, id ASC")
    suspend fun getSlideBlocksSync(noteId: Long): List<SlideBlockEntity>

    @Query("SELECT * FROM slide_blocks WHERE pageIndex = 0")
    fun getAllFirstSlideBlocks(): Flow<List<SlideBlockEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlideBlock(block: SlideBlockEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlideBlocks(blocks: List<SlideBlockEntity>)

    @Query("DELETE FROM slide_blocks WHERE noteId = :noteId")
    suspend fun deleteSlideBlocksForNote(noteId: Long)

    @Delete
    suspend fun deleteSlideBlock(block: SlideBlockEntity)

    // Questões
    @Query("SELECT * FROM questions ORDER BY createdAt DESC")
    fun getAllQuestions(): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE subjectId = :subjectId ORDER BY createdAt DESC")
    fun getQuestionsForSubject(subjectId: Long): Flow<List<QuestionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: QuestionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Delete
    suspend fun deleteQuestion(question: QuestionEntity)

    // Flashcards
    @Query("SELECT * FROM flashcards ORDER BY createdAt DESC")
    fun getAllFlashcards(): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE subjectId = :subjectId ORDER BY createdAt DESC")
    fun getFlashcardsForSubject(subjectId: Long): Flow<List<FlashcardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(flashcard: FlashcardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcards(flashcards: List<FlashcardEntity>)

    @Delete
    suspend fun deleteFlashcard(flashcard: FlashcardEntity)

    // Estatísticas em tempo real
    @Query("SELECT COUNT(*) FROM notes")
    fun getNotesCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM questions")
    fun getQuestionsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM flashcards")
    fun getFlashcardsCount(): Flow<Int>
}
