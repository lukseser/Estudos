package com.example.data.repository

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
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class StudyRepository(
    private val curriculumDao: CurriculumDao,
    private val taskDao: TaskDao,
    private val calendarDao: CalendarDao,
    private val studyDao: StudyDao
) {
    // Curriculum
    val allSemesters: Flow<List<SemesterEntity>> = curriculumDao.getAllSemesters()
    val allModules: Flow<List<ModuleEntity>> = curriculumDao.getAllModules()
    val allSubjects: Flow<List<SubjectEntity>> = curriculumDao.getAllSubjects()

    fun getModulesForSemester(semesterId: Long): Flow<List<ModuleEntity>> =
        curriculumDao.getModulesForSemester(semesterId)

    fun getSubjectsForModule(moduleId: Long): Flow<List<SubjectEntity>> =
        curriculumDao.getSubjectsForModule(moduleId)

    suspend fun insertSemester(name: String): Long =
        curriculumDao.insertSemester(SemesterEntity(name = name))

    suspend fun updateSemester(semester: SemesterEntity) =
        curriculumDao.updateSemester(semester)

    suspend fun deleteSemester(semester: SemesterEntity) {
        // Cascade delete modules and subjects
        val modules = curriculumDao.getAllModules() // We can delete modules for this semester
        curriculumDao.deleteModulesBySemester(semester.id)
        curriculumDao.deleteSemester(semester)
    }

    suspend fun insertModule(semesterId: Long, name: String): Long =
        curriculumDao.insertModule(ModuleEntity(semesterId = semesterId, name = name))

    suspend fun deleteModule(module: ModuleEntity) {
        curriculumDao.deleteSubjectsByModule(module.id)
        curriculumDao.deleteModule(module)
    }

    suspend fun insertSubject(moduleId: Long, name: String): Long =
        curriculumDao.insertSubject(SubjectEntity(moduleId = moduleId, name = name))

    suspend fun deleteSubject(subject: SubjectEntity) =
        curriculumDao.deleteSubject(subject)

    suspend fun getSemesterCount(): Int = curriculumDao.getSemesterCount()

    // Tasks
    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()

    fun getTasksForDate(date: String): Flow<List<TaskEntity>> =
        taskDao.getTasksForDate(date)

    suspend fun insertTask(title: String, dueDate: String?): Long =
        taskDao.insertTask(TaskEntity(title = title, dueDate = dueDate))

    suspend fun updateTask(task: TaskEntity) =
        taskDao.updateTask(task)

    suspend fun toggleTaskCompleted(task: TaskEntity) {
        taskDao.updateTask(task.copy(isCompleted = !task.isCompleted))
    }

    suspend fun deleteTask(task: TaskEntity) =
        taskDao.deleteTask(task)

    // Calendar
    val allEvents: Flow<List<CalendarEventEntity>> = calendarDao.getAllEvents()

    fun getEventsForDate(date: String): Flow<List<CalendarEventEntity>> =
        calendarDao.getEventsForDate(date)

    suspend fun insertEvent(title: String, eventDate: String, time: String? = null, description: String? = null): Long =
        calendarDao.insertEvent(CalendarEventEntity(title = title, eventDate = eventDate, time = time, description = description))

    suspend fun deleteEvent(event: CalendarEventEntity) =
        calendarDao.deleteEvent(event)

    // Notes
    val allNotes: Flow<List<NoteEntity>> = studyDao.getAllNotes()

    fun getNotesForSubject(subjectId: Long): Flow<List<NoteEntity>> =
        studyDao.getNotesForSubject(subjectId)

    suspend fun getNoteById(id: Long): NoteEntity? =
        studyDao.getNoteById(id)

    suspend fun saveNoteWithSlides(
        noteId: Long,
        subjectId: Long,
        title: String,
        mode: String,
        freeTextContent: String,
        drawingData: String = "",
        blocks: List<SlideBlockEntity>
    ): Long {
        val note = if (noteId > 0) {
            val existing = studyDao.getNoteById(noteId)
            val updated = (existing ?: NoteEntity(id = noteId, subjectId = subjectId, title = title)).copy(
                subjectId = subjectId,
                title = title,
                mode = mode,
                freeTextContent = freeTextContent,
                drawingData = drawingData,
                updatedAt = System.currentTimeMillis()
            )
            studyDao.updateNote(updated)
            noteId
        } else {
            val newNote = NoteEntity(
                subjectId = subjectId,
                title = title,
                mode = mode,
                freeTextContent = freeTextContent,
                drawingData = drawingData,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            studyDao.insertNote(newNote)
        }

        if (mode == "SLIDES") {
            studyDao.deleteSlideBlocksForNote(note)
            val mappedBlocks = blocks.mapIndexed { index, block ->
                block.copy(id = 0, noteId = note, pageIndex = index)
            }
            studyDao.insertSlideBlocks(mappedBlocks)
        }
        return note
    }

    fun getSlideBlocks(noteId: Long): Flow<List<SlideBlockEntity>> =
        studyDao.getSlideBlocks(noteId)

    val allFirstSlideBlocks: Flow<List<SlideBlockEntity>> = studyDao.getAllFirstSlideBlocks()

    suspend fun getSlideBlocksSync(noteId: Long): List<SlideBlockEntity> =
        studyDao.getSlideBlocksSync(noteId)

    suspend fun deleteNote(note: NoteEntity) {
        studyDao.deleteSlideBlocksForNote(note.id)
        studyDao.deleteNote(note)
    }

    // Questions
    val allQuestions: Flow<List<QuestionEntity>> = studyDao.getAllQuestions()

    fun getQuestionsForSubject(subjectId: Long): Flow<List<QuestionEntity>> =
        studyDao.getQuestionsForSubject(subjectId)

    suspend fun insertQuestion(question: QuestionEntity): Long =
        studyDao.insertQuestion(question)

    suspend fun insertQuestions(questions: List<QuestionEntity>) =
        studyDao.insertQuestions(questions)

    suspend fun deleteQuestion(question: QuestionEntity) =
        studyDao.deleteQuestion(question)

    // Flashcards
    val allFlashcards: Flow<List<FlashcardEntity>> = studyDao.getAllFlashcards()

    fun getFlashcardsForSubject(subjectId: Long): Flow<List<FlashcardEntity>> =
        studyDao.getFlashcardsForSubject(subjectId)

    suspend fun insertFlashcard(flashcard: FlashcardEntity): Long =
        studyDao.insertFlashcard(flashcard)

    suspend fun insertFlashcards(flashcards: List<FlashcardEntity>) =
        studyDao.insertFlashcards(flashcards)

    suspend fun deleteFlashcard(flashcard: FlashcardEntity) =
        studyDao.deleteFlashcard(flashcard)

    // Stats
    val notesCount: Flow<Int> = studyDao.getNotesCount()
    val questionsCount: Flow<Int> = studyDao.getQuestionsCount()
    val flashcardsCount: Flow<Int> = studyDao.getFlashcardsCount()

    // Prepopulate initial data if empty
    suspend fun checkAndSeedInitialData() {
        if (curriculumDao.getSemesterCount() == 0) {
            val sem1Id = curriculumDao.insertSemester(SemesterEntity(name = "1º Semestre", orderIndex = 1))
            val sem2Id = curriculumDao.insertSemester(SemesterEntity(name = "2º Semestre", orderIndex = 2))

            // Modulos Semestre 1
            val modBioId = curriculumDao.insertModule(ModuleEntity(semesterId = sem1Id, name = "Fundamentos Biológicos", orderIndex = 1))
            val modCardioId = curriculumDao.insertModule(ModuleEntity(semesterId = sem1Id, name = "Sistema Cardiovascular", orderIndex = 2))

            // Disciplinas Semestre 1
            val subAnatId = curriculumDao.insertSubject(SubjectEntity(moduleId = modBioId, name = "Anatomia Humana", orderIndex = 1))
            val subFisioId = curriculumDao.insertSubject(SubjectEntity(moduleId = modBioId, name = "Fisiologia Geral", orderIndex = 2))
            val subBioqId = curriculumDao.insertSubject(SubjectEntity(moduleId = modBioId, name = "Bioquímica", orderIndex = 3))
            val subCardioSubId = curriculumDao.insertSubject(SubjectEntity(moduleId = modCardioId, name = "Cardiologia Básica", orderIndex = 1))
            val subEcgId = curriculumDao.insertSubject(SubjectEntity(moduleId = modCardioId, name = "Eletrocardiograma (ECG)", orderIndex = 2))

            // Modulos Semestre 2
            val modNervId = curriculumDao.insertModule(ModuleEntity(semesterId = sem2Id, name = "Sistema Nervoso", orderIndex = 1))
            val subNeuroId = curriculumDao.insertSubject(SubjectEntity(moduleId = modNervId, name = "Neuroanatomia", orderIndex = 1))
            val subFarmacoId = curriculumDao.insertSubject(SubjectEntity(moduleId = modNervId, name = "Farmacologia", orderIndex = 2))

            // Seed sample Tasks
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val cal = Calendar.getInstance()
            val todayStr = dateFormat.format(cal.time)

            cal.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayStr = dateFormat.format(cal.time)

            cal.add(Calendar.DAY_OF_YEAR, 4)
            val futureStr = dateFormat.format(cal.time)

            taskDao.insertTask(TaskEntity(title = "Revisar vascularização cerebral", dueDate = yesterdayStr, isCompleted = false))
            taskDao.insertTask(TaskEntity(title = "Resolver 20 questões de Fisiologia", dueDate = todayStr, isCompleted = false))
            taskDao.insertTask(TaskEntity(title = "Preparar resumo de ECG com slides", dueDate = futureStr, isCompleted = false))
            taskDao.insertTask(TaskEntity(title = "Comprar caderno de mapas conceituais", dueDate = null, isCompleted = true))

            // Seed sample Event
            calendarDao.insertEvent(
                CalendarEventEntity(
                    title = "Prova de Anatomia: Aparelho Locomotor",
                    eventDate = todayStr,
                    time = "14:00",
                    description = "Sala 304 - Levar prancheta e jaleco"
                )
            )

            // Seed sample Questions
            studyDao.insertQuestions(
                listOf(
                    QuestionEntity(
                        subjectId = subAnatId,
                        subjectName = "Anatomia Humana",
                        prompt = "Qual estrutura óssea do crânio abriga a glândula hipófise?",
                        optionA = "Lâmina cribriforme do etmoide",
                        optionB = "Sela túrcica do osso esfenoide",
                        optionC = "Forame magno do occipital",
                        optionD = "Meato acústico interno do temporal",
                        correctAnswer = "B",
                        explanation = "A sela túrcica (fossa hipofisial) está localizada no corpo do osso esfenoide e aloja a glândula hipófise."
                    ),
                    QuestionEntity(
                        subjectId = subFisioId,
                        subjectName = "Fisiologia Geral",
                        prompt = "Durante o potencial de ação cardíaco nas células ventriculares, a fase de platô (fase 2) é sustentada principalmente por:",
                        optionA = "Influxo rápido de íons Sódio (Na+)",
                        optionB = "Efluxo maciço de íons Cloreto (Cl-)",
                        optionC = "Influxo de Cálcio (Ca2+) por canais do tipo L e efluxo de Potássio (K+)",
                        optionD = "Bomba de Sódio-Potássio ATPase em hiperatividade",
                        correctAnswer = "C",
                        explanation = "A fase de platô ocorre pelo equilíbrio entre a entrada lenta de Ca2+ (canais L) e a saída de K+."
                    )
                )
            )

            // Seed sample Flashcards
            studyDao.insertFlashcards(
                listOf(
                    FlashcardEntity(
                        subjectId = subAnatId,
                        subjectName = "Anatomia Humana",
                        front = "Quais são os 4 músculos que formam o manguito rotador?",
                        back = "1. Supraespinhal\n2. Infraespinhal\n3. Redondo Menor\n4. Subescapular\n(Mnemônico: SITS)"
                    ),
                    FlashcardEntity(
                        subjectId = subBioqId,
                        subjectName = "Bioquímica",
                        front = "Qual a enzima marcapasso (principal ponto de regulação) da via glicolítica?",
                        back = "Fosfofrutoquinase-1 (PFK-1), catalisa a conversão de Frutose-6-fosfato em Frutose-1,6-bisfosfato com gasto de ATP."
                    ),
                    FlashcardEntity(
                        subjectId = subCardioSubId,
                        subjectName = "Cardiologia Básica",
                        front = "O que representa a Onda P no traçado do Eletrocardiograma normal?",
                        back = "A despolarização atrial (ativação elétrica dos átrios direito e esquerdo antes da contração mecânica)."
                    )
                )
            )

            // Seed sample Note
            val noteId = studyDao.insertNote(
                NoteEntity(
                    subjectId = subAnatId,
                    title = "Introdução à Anatomia e Eixos Anatômicos",
                    mode = "FREE",
                    freeTextContent = "Eixos e Planos Anatômicos:\n• Plano Sagital Mediano: divide o corpo em metades direita e esquerda.\n• Plano Coronal (Frontal): divide em porções anterior e posterior.\n• Plano Transversal (Horizontal): divide em porções cranial e caudal.\n\nTermos de Posição:\n- Medial vs Lateral\n- Proximal vs Distal (nos membros)\n- Ventral vs Dorsal"
                )
            )
        }
    }
}
