package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CalendarEventEntity
import com.example.data.local.entity.FlashcardEntity
import com.example.data.local.entity.ModuleEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.QuestionEntity
import com.example.data.local.entity.SemesterEntity
import com.example.data.local.entity.SlideBlockEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.repository.StudyRepository
import com.example.data.util.CsvHelper
import com.example.data.util.PdfHelper
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.text.SimpleDateFormat
import java.time.YearMonth
import java.util.Collections
import java.util.Date
import java.util.Locale

sealed class AppScreen {
    object Dashboard : AppScreen()
    object Calendar : AppScreen()
    object Questions : AppScreen()
    object Flashcards : AppScreen()
    object Curriculum : AppScreen()
    data class SubjectLibrary(val subjectId: Long, val subjectName: String) : AppScreen()
    data class SubjectEditor(val subjectId: Long, val subjectName: String, val isReadOnly: Boolean = false) : AppScreen()
}

data class TaskWithBadge(
    val task: TaskEntity,
    val badgeType: BadgeType,
    val badgeText: String
)

enum class BadgeType {
    OVERDUE, // Vermelho "Atrasado"
    TODAY,   // Azul "Hoje"
    FUTURE,  // Cinza com data
    NO_DATE, // Cinza "Sem Data"
    COMPLETED // Cinza / Riscado
}

class StudyViewModel(
    application: Application,
    private val repository: StudyRepository
) : AndroidViewModel(application) {

    // Tela atual (Roteamento Interno SPA)
    private val _currentScreen = MutableStateFlow<AppScreen>(AppScreen.Dashboard)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Mensagens de feedback (Snackbars / Toasts)
    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    // Compartilhamento de arquivos (PDF / CSV)
    private val _shareFileUri = MutableSharedFlow<Uri>()
    val shareFileUri: SharedFlow<Uri> = _shareFileUri.asSharedFlow()

    // Status de carregamento (ex: extração de PDF)
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Data de hoje formatada (YYYY-MM-DD)
    val todayString: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // ==========================================
    // 1. GESTÃO CURRICULAR & DRAWER ACCORDION
    // ==========================================
    val allSemesters: StateFlow<List<SemesterEntity>> = repository.allSemesters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allModules: StateFlow<List<ModuleEntity>> = repository.allModules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSubjects: StateFlow<List<SubjectEntity>> = repository.allSubjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Semestre selecionado no Drawer e Gestor
    private val _selectedSemesterId = MutableStateFlow<Long?>(null)
    val selectedSemesterId: StateFlow<Long?> = _selectedSemesterId.asStateFlow()

    // Módulos expandidos no Accordion
    private val _expandedModuleIds = MutableStateFlow<Set<Long>>(emptySet())
    val expandedModuleIds: StateFlow<Set<Long>> = _expandedModuleIds.asStateFlow()

    // ==========================================
    // 2. ESTATÍSTICAS EM TEMPO REAL
    // ==========================================
    val notesCount: StateFlow<Int> = repository.notesCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val questionsCount: StateFlow<Int> = repository.questionsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val flashcardsCount: StateFlow<Int> = repository.flashcardsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ==========================================
    // 3. TAREFAS / DASHBOARD COM ORDENAÇÃO AVANÇADA
    // ==========================================
    val tasksWithBadges: StateFlow<List<TaskWithBadge>> = repository.allTasks
        .combine(_currentScreen) { tasks, _ ->
            val today = todayString

            // Mapeia para TaskWithBadge
            val mapped = tasks.map { task ->
                val (badgeType, badgeText) = when {
                    task.isCompleted -> BadgeType.COMPLETED to "Concluída"
                    task.dueDate == null -> BadgeType.NO_DATE to "Sem Data"
                    task.dueDate < today -> BadgeType.OVERDUE to "Atrasada"
                    task.dueDate == today -> BadgeType.TODAY to "Hoje"
                    else -> {
                        // Formatar DD/MM
                        val parts = task.dueDate.split("-")
                        val formatted = if (parts.size == 3) "${parts[2]}/${parts[1]}" else task.dueDate
                        BadgeType.FUTURE to formatted
                    }
                }
                TaskWithBadge(task, badgeType, badgeText)
            }

            // Ordenação Rigorosa:
            // 1. Pendentes no topo, Concluídas no final
            // 2. Nas pendentes: Atrasadas > Hoje > Futuras > Sem Data
            mapped.sortedWith(
                compareBy<TaskWithBadge> { it.task.isCompleted } // false (0) first, true (1) last
                    .thenBy {
                        when (it.badgeType) {
                            BadgeType.OVERDUE -> 1
                            BadgeType.TODAY -> 2
                            BadgeType.FUTURE -> 3
                            BadgeType.NO_DATE -> 4
                            BadgeType.COMPLETED -> 5
                        }
                    }
                    .thenBy { it.task.dueDate ?: "9999-99-99" }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ==========================================
    // 4. CALENDÁRIO DINÂMICO
    // ==========================================
    private val _selectedDate = MutableStateFlow(todayString)
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _currentYearMonth = MutableStateFlow(YearMonth.now())
    val currentYearMonth: StateFlow<YearMonth> = _currentYearMonth.asStateFlow()

    val allEvents: StateFlow<List<CalendarEventEntity>> = repository.allEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ==========================================
    // 5. CADERNO DE ANOTAÇÕES & EDITOR RICO
    // ==========================================
    private val _activeSubjectId = MutableStateFlow<Long>(0)
    val activeSubjectId: StateFlow<Long> = _activeSubjectId.asStateFlow()

    private val _activeSubjectName = MutableStateFlow("")
    val activeSubjectName: StateFlow<String> = _activeSubjectName.asStateFlow()

    private val _editorMode = MutableStateFlow("FREE") // "FREE" ou "SLIDES"
    val editorMode: StateFlow<String> = _editorMode.asStateFlow()

    private val _slideViewerLayout = MutableStateFlow("SPLIT") // "SPLIT", "LINEAR", "TEXT_ONLY"
    val slideViewerLayout: StateFlow<String> = _slideViewerLayout.asStateFlow()

    private val _noteTitle = MutableStateFlow("")
    val noteTitle: StateFlow<String> = _noteTitle.asStateFlow()

    private val _freeTextContent = MutableStateFlow("")
    val freeTextContent: StateFlow<String> = _freeTextContent.asStateFlow()

    private val _noteDrawingData = MutableStateFlow("")
    val noteDrawingData: StateFlow<String> = _noteDrawingData.asStateFlow()

    private val _slideBlocks = MutableStateFlow<List<SlideBlockEntity>>(emptyList())
    val slideBlocks: StateFlow<List<SlideBlockEntity>> = _slideBlocks.asStateFlow()

    private val _activeNoteId = MutableStateFlow<Long>(0)
    val activeNoteId: StateFlow<Long> = _activeNoteId.asStateFlow()

    // Anotações salvas da disciplina ativa
    val savedNotesForSubject: StateFlow<List<NoteEntity>> = _activeSubjectId
        .combine(repository.allNotes) { subjectId, allNotes ->
            allNotes.filter { it.subjectId == subjectId }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Todas as anotações para a Biblioteca Geral
    val allNotes: StateFlow<List<NoteEntity>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Modo de Leitura apenas (Read-Only) vs Modo Edição
    private val _isReadOnly = MutableStateFlow(false)
    val isReadOnly: StateFlow<Boolean> = _isReadOnly.asStateFlow()

    // Status de Salvamento Automático
    private val _isAutoSaving = MutableStateFlow(false)
    val isAutoSaving: StateFlow<Boolean> = _isAutoSaving.asStateFlow()

    // Primeiros blocos de slides para pré-visualização na biblioteca (Thumbnail estilo Samsung Notes)
    val firstSlideBlocks: StateFlow<Map<Long, SlideBlockEntity>> = repository.allFirstSlideBlocks
        .map { list -> list.associateBy { it.noteId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Modo / Tema da Aplicação (Claro por padrão)
    private val _themeMode = MutableStateFlow<ThemeMode>(ThemeMode.LIGHT)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    fun toggleThemeMode() {
        _themeMode.value = if (_themeMode.value == ThemeMode.LIGHT) ThemeMode.DARK else ThemeMode.LIGHT
    }

    // ==========================================
    // 6. BANCO DE QUESTÕES
    // ==========================================
    val allQuestions: StateFlow<List<QuestionEntity>> = repository.allQuestions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _questionSubjectFilter = MutableStateFlow<Long?>(null)
    val questionSubjectFilter: StateFlow<Long?> = _questionSubjectFilter.asStateFlow()

    private val _revealedQuestions = MutableStateFlow<Set<Long>>(emptySet())
    val revealedQuestions: StateFlow<Set<Long>> = _revealedQuestions.asStateFlow()

    // ==========================================
    // 7. FLASHCARDS TIPO ANKI
    // ==========================================
    val allFlashcards: StateFlow<List<FlashcardEntity>> = repository.allFlashcards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _flashcardSubjectFilter = MutableStateFlow<Long?>(null)
    val flashcardSubjectFilter: StateFlow<Long?> = _flashcardSubjectFilter.asStateFlow()

    private val _flippedFlashcards = MutableStateFlow<Set<Long>>(emptySet())
    val flippedFlashcards: StateFlow<Set<Long>> = _flippedFlashcards.asStateFlow()

    init {
        viewModelScope.launch {
            repository.checkAndSeedInitialData()
        }
    }

    // ====================
    // NAVEGAÇÃO & UI
    // ====================
    fun switchView(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun openSubject(subjectId: Long, subjectName: String) {
        _activeSubjectId.value = subjectId
        _activeSubjectName.value = subjectName
        _currentScreen.value = AppScreen.SubjectLibrary(subjectId, subjectName)
    }

    fun openNoteForViewing(note: NoteEntity, subjectName: String) {
        _activeSubjectId.value = note.subjectId
        _activeSubjectName.value = subjectName
        _activeNoteId.value = note.id
        _noteTitle.value = note.title
        _editorMode.value = note.mode
        _freeTextContent.value = note.freeTextContent
        _noteDrawingData.value = note.drawingData
        _isReadOnly.value = true
        viewModelScope.launch {
            if (note.mode == "SLIDES") {
                _slideBlocks.value = repository.getSlideBlocksSync(note.id)
            } else {
                _slideBlocks.value = emptyList()
            }
            _currentScreen.value = AppScreen.SubjectEditor(note.subjectId, subjectName, isReadOnly = true)
        }
    }

    fun openNoteForEditing(note: NoteEntity, subjectName: String) {
        _activeSubjectId.value = note.subjectId
        _activeSubjectName.value = subjectName
        _activeNoteId.value = note.id
        _noteTitle.value = note.title
        _editorMode.value = note.mode
        _freeTextContent.value = note.freeTextContent
        _noteDrawingData.value = note.drawingData
        _isReadOnly.value = false
        viewModelScope.launch {
            if (note.mode == "SLIDES") {
                _slideBlocks.value = repository.getSlideBlocksSync(note.id)
            } else {
                _slideBlocks.value = emptyList()
            }
            _currentScreen.value = AppScreen.SubjectEditor(note.subjectId, subjectName, isReadOnly = false)
        }
    }

    fun startNewNote(subjectId: Long, subjectName: String) {
        _activeSubjectId.value = subjectId
        _activeSubjectName.value = subjectName
        _activeNoteId.value = 0
        _noteTitle.value = "Nova Anotação de ${subjectName.ifBlank { "Estudo" }}"
        _freeTextContent.value = ""
        _noteDrawingData.value = ""
        _slideBlocks.value = emptyList()
        _editorMode.value = "FREE"
        _isReadOnly.value = false
        _currentScreen.value = AppScreen.SubjectEditor(subjectId, subjectName, isReadOnly = false)
    }

    fun setReadOnly(readOnly: Boolean) {
        _isReadOnly.value = readOnly
        val current = _currentScreen.value
        if (current is AppScreen.SubjectEditor) {
            _currentScreen.value = current.copy(isReadOnly = readOnly)
        }
    }

    fun selectSemester(semesterId: Long) {
        _selectedSemesterId.value = semesterId
    }

    fun toggleModuleAccordion(moduleId: Long) {
        val current = _expandedModuleIds.value
        _expandedModuleIds.value = if (current.contains(moduleId)) {
            current - moduleId
        } else {
            current + moduleId
        }
    }

    // ====================
    // AÇÕES DE TAREFAS
    // ====================
    fun addTask(title: String, dueDate: String?) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.insertTask(title.trim(), dueDate)
            _userMessage.emit("Tarefa criada!")
        }
    }

    fun toggleTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.toggleTaskCompleted(task)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
            _userMessage.emit("Tarefa removida")
        }
    }

    // ====================
    // AÇÕES DE CALENDÁRIO
    // ====================
    fun selectCalendarDate(date: String) {
        _selectedDate.value = date
    }

    fun nextMonth() {
        _currentYearMonth.value = _currentYearMonth.value.plusMonths(1)
    }

    fun prevMonth() {
        _currentYearMonth.value = _currentYearMonth.value.minusMonths(1)
    }

    fun addCalendarEvent(title: String, eventDate: String, time: String?, description: String?) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.insertEvent(title.trim(), eventDate, time?.trim(), description?.trim())
            _userMessage.emit("Evento agendado com sucesso!")
        }
    }

    fun deleteCalendarEvent(event: CalendarEventEntity) {
        viewModelScope.launch {
            repository.deleteEvent(event)
            _userMessage.emit("Evento removido")
        }
    }

    // ====================
    // AÇÕES DO CADERNO / SLIDES
    // ====================
    fun setEditorMode(mode: String) {
        _editorMode.value = mode
    }

    fun setSlideViewerLayout(layout: String) {
        _slideViewerLayout.value = layout
    }

    fun updateNoteTitle(title: String) {
        _noteTitle.value = title
    }

    fun updateFreeTextContent(content: String) {
        _freeTextContent.value = content
    }

    fun updateNoteDrawingData(data: String) {
        _noteDrawingData.value = data
    }

    fun updateSlideBlockDrawingData(index: Int, data: String) {
        val current = _slideBlocks.value.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(drawingData = data)
            _slideBlocks.value = current
        }
    }

    fun addManualSlideBlock(imagePath: String? = null) {
        val current = _slideBlocks.value.toMutableList()
        val newBlock = SlideBlockEntity(
            noteId = _activeNoteId.value,
            pageIndex = current.size,
            imagePath = imagePath,
            notesContent = ""
        )
        current.add(newBlock)
        _slideBlocks.value = current
    }

    fun updateSlideBlockNotes(index: Int, notes: String) {
        val current = _slideBlocks.value.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(notesContent = notes)
            _slideBlocks.value = current
        }
    }

    fun updateSlideBlockImage(index: Int, imagePath: String) {
        val current = _slideBlocks.value.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(imagePath = imagePath)
            _slideBlocks.value = current
        }
    }

    fun moveSlideBlockUp(index: Int) {
        if (index > 0) {
            val current = _slideBlocks.value.toMutableList()
            Collections.swap(current, index, index - 1)
            _slideBlocks.value = current
        }
    }

    fun moveSlideBlockDown(index: Int) {
        val current = _slideBlocks.value.toMutableList()
        if (index < current.size - 1) {
            Collections.swap(current, index, index + 1)
            _slideBlocks.value = current
        }
    }

    fun removeSlideBlock(index: Int) {
        val current = _slideBlocks.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _slideBlocks.value = current
        }
    }

    fun extractPdfAndCreateBlocks(context: Context, pdfUri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            val slidePaths = withContext(Dispatchers.IO) {
                PdfHelper.extractPdfSlides(context, pdfUri)
            }
            _isLoading.value = false

            if (slidePaths.isNotEmpty()) {
                val newBlocks = slidePaths.mapIndexed { idx, path ->
                    SlideBlockEntity(
                        noteId = _activeNoteId.value,
                        pageIndex = idx,
                        imagePath = path,
                        notesContent = ""
                    )
                }
                _slideBlocks.value = newBlocks
                _editorMode.value = "SLIDES"
                _userMessage.emit("${slidePaths.size} slides importados do PDF!")
            } else {
                _userMessage.emit("Não foi possível extrair slides do PDF selecionado.")
            }
        }
    }

    fun saveCurrentNote() {
        val title = _noteTitle.value.ifBlank { "Sem Título" }
        viewModelScope.launch {
            val savedId = repository.saveNoteWithSlides(
                noteId = _activeNoteId.value,
                subjectId = _activeSubjectId.value,
                title = title,
                mode = _editorMode.value,
                freeTextContent = _freeTextContent.value,
                drawingData = _noteDrawingData.value,
                blocks = _slideBlocks.value
            )
            _activeNoteId.value = savedId
            _userMessage.emit("Anotação salva com sucesso!")
        }
    }

    fun autoSaveCurrentNote() {
        val title = _noteTitle.value.ifBlank { "Sem Título" }
        viewModelScope.launch {
            _isAutoSaving.value = true
            try {
                val savedId = repository.saveNoteWithSlides(
                    noteId = _activeNoteId.value,
                    subjectId = _activeSubjectId.value,
                    title = title,
                    mode = _editorMode.value,
                    freeTextContent = _freeTextContent.value,
                    drawingData = _noteDrawingData.value,
                    blocks = _slideBlocks.value
                )
                _activeNoteId.value = savedId
            } finally {
                _isAutoSaving.value = false
            }
        }
    }

    fun loadNoteForEditing(note: NoteEntity) {
        viewModelScope.launch {
            _activeNoteId.value = note.id
            _noteTitle.value = note.title
            _editorMode.value = note.mode
            _freeTextContent.value = note.freeTextContent
            _noteDrawingData.value = note.drawingData

            if (note.mode == "SLIDES") {
                val blocks = repository.getSlideBlocksSync(note.id)
                _slideBlocks.value = blocks
            } else {
                _slideBlocks.value = emptyList()
            }
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
            if (_activeNoteId.value == note.id) {
                _activeNoteId.value = 0
                _noteTitle.value = "Nova Anotação"
                _freeTextContent.value = ""
                _slideBlocks.value = emptyList()
            }
            _userMessage.emit("Anotação excluída")
        }
    }

    fun newNote(subjectId: Long = _activeSubjectId.value, subjectName: String = _activeSubjectName.value) {
        _activeNoteId.value = 0
        _noteTitle.value = "Nova Anotação de ${subjectName.ifBlank { "Estudo" }}"
        _freeTextContent.value = ""
        _slideBlocks.value = emptyList()
        _editorMode.value = "FREE"
    }

    fun exportNoteById(context: Context, note: NoteEntity, subjectName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val blocks = if (note.mode == "SLIDES") {
                withContext(Dispatchers.IO) { repository.getSlideBlocksSync(note.id) }
            } else emptyList()

            val uri = withContext(Dispatchers.IO) {
                PdfHelper.exportNoteToPdf(
                    context = context,
                    title = note.title.ifBlank { "Resumo de Estudo" },
                    subjectName = subjectName,
                    mode = note.mode,
                    freeTextContent = note.freeTextContent,
                    drawingData = note.drawingData,
                    slideBlocks = blocks
                )
            }
            _isLoading.value = false
            if (uri != null) {
                _shareFileUri.emit(uri)
                _userMessage.emit("PDF de '${note.title}' gerado com sucesso!")
            } else {
                _userMessage.emit("Erro ao exportar PDF.")
            }
        }
    }

    fun exportCurrentNotePdf(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            val uri = withContext(Dispatchers.IO) {
                PdfHelper.exportNoteToPdf(
                    context = context,
                    title = _noteTitle.value.ifBlank { "Resumo de Estudo" },
                    subjectName = _activeSubjectName.value.ifBlank { "Geral" },
                    mode = _editorMode.value,
                    freeTextContent = _freeTextContent.value,
                    drawingData = _noteDrawingData.value,
                    slideBlocks = _slideBlocks.value
                )
            }
            _isLoading.value = false
            if (uri != null) {
                _shareFileUri.emit(uri)
                _userMessage.emit("PDF gerado com sucesso!")
            } else {
                _userMessage.emit("Erro ao exportar PDF.")
            }
        }
    }

    // ====================
    // AÇÕES DE QUESTÕES
    // ====================
    fun setQuestionSubjectFilter(subjectId: Long?) {
        _questionSubjectFilter.value = subjectId
    }

    fun toggleRevealQuestion(questionId: Long) {
        val current = _revealedQuestions.value
        _revealedQuestions.value = if (current.contains(questionId)) {
            current - questionId
        } else {
            current + questionId
        }
    }

    fun addQuestion(
        subjectId: Long,
        subjectName: String,
        prompt: String,
        optA: String,
        optB: String,
        optC: String,
        optD: String,
        correct: String,
        explanation: String
    ) {
        if (prompt.isBlank() || optA.isBlank() || optB.isBlank()) return
        viewModelScope.launch {
            repository.insertQuestion(
                QuestionEntity(
                    subjectId = subjectId,
                    subjectName = subjectName,
                    prompt = prompt.trim(),
                    optionA = optA.trim(),
                    optionB = optB.trim(),
                    optionC = optC.trim(),
                    optionD = optD.trim(),
                    correctAnswer = correct,
                    explanation = explanation.trim()
                )
            )
            _userMessage.emit("Questão adicionada!")
        }
    }

    fun deleteQuestion(question: QuestionEntity) {
        viewModelScope.launch {
            repository.deleteQuestion(question)
            _userMessage.emit("Questão excluída")
        }
    }

    fun downloadQuestionsTemplate(context: Context) {
        viewModelScope.launch {
            val uri = CsvHelper.createTemplateFile(context, "modelo_questoes.csv", CsvHelper.SAMPLE_QUESTIONS_CSV)
            if (uri != null) {
                _shareFileUri.emit(uri)
            } else {
                _userMessage.emit("Erro ao gerar modelo CSV.")
            }
        }
    }

    fun importQuestionsFromCsv(inputStream: InputStream) {
        viewModelScope.launch {
            val parsed = withContext(Dispatchers.IO) {
                CsvHelper.parseQuestionsCsv(inputStream)
            }
            if (parsed.isNotEmpty()) {
                repository.insertQuestions(parsed)
                _userMessage.emit("${parsed.size} questões importadas com sucesso!")
            } else {
                _userMessage.emit("Nenhuma questão válida encontrada no arquivo CSV.")
            }
        }
    }

    // ====================
    // AÇÕES DE FLASHCARDS
    // ====================
    fun setFlashcardSubjectFilter(subjectId: Long?) {
        _flashcardSubjectFilter.value = subjectId
    }

    fun toggleFlashcardFlip(cardId: Long) {
        val current = _flippedFlashcards.value
        _flippedFlashcards.value = if (current.contains(cardId)) {
            current - cardId
        } else {
            current + cardId
        }
    }

    fun addFlashcard(subjectId: Long, subjectName: String, front: String, back: String) {
        if (front.isBlank() || back.isBlank()) return
        viewModelScope.launch {
            repository.insertFlashcard(
                FlashcardEntity(
                    subjectId = subjectId,
                    subjectName = subjectName,
                    front = front.trim(),
                    back = back.trim()
                )
            )
            _userMessage.emit("Flashcard criado!")
        }
    }

    fun deleteFlashcard(flashcard: FlashcardEntity) {
        viewModelScope.launch {
            repository.deleteFlashcard(flashcard)
            _userMessage.emit("Flashcard removido")
        }
    }

    fun downloadFlashcardsTemplate(context: Context) {
        viewModelScope.launch {
            val uri = CsvHelper.createTemplateFile(context, "modelo_flashcards.csv", CsvHelper.SAMPLE_FLASHCARDS_CSV)
            if (uri != null) {
                _shareFileUri.emit(uri)
            } else {
                _userMessage.emit("Erro ao gerar modelo de Flashcards.")
            }
        }
    }

    fun importFlashcardsFromCsv(inputStream: InputStream) {
        viewModelScope.launch {
            val parsed = withContext(Dispatchers.IO) {
                CsvHelper.parseFlashcardsCsv(inputStream)
            }
            if (parsed.isNotEmpty()) {
                repository.insertFlashcards(parsed)
                _userMessage.emit("${parsed.size} flashcards importados com sucesso!")
            } else {
                _userMessage.emit("Nenhum flashcard válido encontrado no arquivo CSV.")
            }
        }
    }

    // ====================
    // AÇÕES DE GESTÃO CURRICULAR
    // ====================
    fun addSemester(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = repository.insertSemester(name.trim())
            _selectedSemesterId.value = id
            _userMessage.emit("Semestre adicionado!")
        }
    }

    fun renameSemester(semester: SemesterEntity, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            repository.updateSemester(semester.copy(name = newName.trim()))
            _userMessage.emit("Semestre renomeado")
        }
    }

    fun deleteSemester(semester: SemesterEntity) {
        viewModelScope.launch {
            val count = repository.getSemesterCount()
            if (count <= 1) {
                _userMessage.emit("Operação bloqueada: o sistema precisa ter pelo menos 1 semestre ativo.")
                return@launch
            }
            repository.deleteSemester(semester)
            _userMessage.emit("Semestre e seus módulos excluídos com sucesso")
        }
    }

    fun addModule(semesterId: Long, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = repository.insertModule(semesterId, name.trim())
            _expandedModuleIds.value = _expandedModuleIds.value + id
            _userMessage.emit("Módulo criado!")
        }
    }

    fun deleteModule(module: ModuleEntity) {
        viewModelScope.launch {
            repository.deleteModule(module)
            _userMessage.emit("Módulo e suas disciplinas excluídos")
        }
    }

    fun addSubject(moduleId: Long, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.insertSubject(moduleId, name.trim())
            _userMessage.emit("Disciplina adicionada!")
        }
    }

    fun deleteSubject(subject: SubjectEntity) {
        viewModelScope.launch {
            repository.deleteSubject(subject)
            _userMessage.emit("Disciplina excluída")
        }
    }
}

class StudyViewModelFactory(
    private val application: Application,
    private val repository: StudyRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StudyViewModel::class.java)) {
            return StudyViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
