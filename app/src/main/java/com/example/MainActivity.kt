package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Surface
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.AppDatabase
import com.example.data.repository.StudyRepository
import com.example.ui.components.SidebarDrawer
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.CurriculumManagerScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FlashcardsScreen
import com.example.ui.screens.QuestionsScreen
import com.example.ui.screens.SubjectEditorScreen
import com.example.ui.screens.SubjectLibraryScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ThemeMode
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.StudyViewModel
import com.example.ui.viewmodel.StudyViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: StudyViewModel

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(this)
        val repository = StudyRepository(
            curriculumDao = database.curriculumDao(),
            taskDao = database.taskDao(),
            calendarDao = database.calendarDao(),
            studyDao = database.studyDao()
        )
        val factory = StudyViewModelFactory(application, repository)
        viewModel = ViewModelProvider(this, factory)[StudyViewModel::class.java]

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

            MyApplicationTheme(themeMode = themeMode) {
                val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
                val semesters by viewModel.allSemesters.collectAsStateWithLifecycle()
                val modules by viewModel.allModules.collectAsStateWithLifecycle()
                val subjects by viewModel.allSubjects.collectAsStateWithLifecycle()
                val selectedSemesterId by viewModel.selectedSemesterId.collectAsStateWithLifecycle()
                val expandedModuleIds by viewModel.expandedModuleIds.collectAsStateWithLifecycle()

                // Estatísticas e Tarefas
                val notesCount by viewModel.notesCount.collectAsStateWithLifecycle()
                val questionsCount by viewModel.questionsCount.collectAsStateWithLifecycle()
                val flashcardsCount by viewModel.flashcardsCount.collectAsStateWithLifecycle()
                val tasksWithBadges by viewModel.tasksWithBadges.collectAsStateWithLifecycle()

                // Calendário
                val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
                val currentYearMonth by viewModel.currentYearMonth.collectAsStateWithLifecycle()
                val allEvents by viewModel.allEvents.collectAsStateWithLifecycle()

                // Caderno / Editor
                val activeSubjectId by viewModel.activeSubjectId.collectAsStateWithLifecycle()
                val activeSubjectName by viewModel.activeSubjectName.collectAsStateWithLifecycle()
                val editorMode by viewModel.editorMode.collectAsStateWithLifecycle()
                val slideViewerLayout by viewModel.slideViewerLayout.collectAsStateWithLifecycle()
                val noteTitle by viewModel.noteTitle.collectAsStateWithLifecycle()
                val freeTextContent by viewModel.freeTextContent.collectAsStateWithLifecycle()
                val slideBlocks by viewModel.slideBlocks.collectAsStateWithLifecycle()
                val activeNoteId by viewModel.activeNoteId.collectAsStateWithLifecycle()
                val savedNotes by viewModel.savedNotesForSubject.collectAsStateWithLifecycle()
                val allNotes by viewModel.allNotes.collectAsStateWithLifecycle()
                val isReadOnly by viewModel.isReadOnly.collectAsStateWithLifecycle()
                val isAutoSaving by viewModel.isAutoSaving.collectAsStateWithLifecycle()
                val firstSlideBlocks by viewModel.firstSlideBlocks.collectAsStateWithLifecycle()
                val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

                // Questões
                val allQuestions by viewModel.allQuestions.collectAsStateWithLifecycle()
                val questionFilter by viewModel.questionSubjectFilter.collectAsStateWithLifecycle()
                val revealedQuestions by viewModel.revealedQuestions.collectAsStateWithLifecycle()

                // Flashcards
                val allFlashcards by viewModel.allFlashcards.collectAsStateWithLifecycle()
                val flashcardFilter by viewModel.flashcardSubjectFilter.collectAsStateWithLifecycle()
                val flippedFlashcards by viewModel.flippedFlashcards.collectAsStateWithLifecycle()

                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                val snackbarHostState = remember { SnackbarHostState() }

                // Ouvir mensagens de feedback
                LaunchedEffect(Unit) {
                    viewModel.userMessage.collectLatest { msg ->
                        snackbarHostState.showSnackbar(msg)
                    }
                }

                // Ouvir compartilhamento de arquivos (PDF e CSV)
                LaunchedEffect(Unit) {
                    viewModel.shareFileUri.collectLatest { uri ->
                        shareFile(uri)
                    }
                }

                var isLibrarySearchOpen by remember { mutableStateOf(false) }
                var librarySearchQuery by remember { mutableStateOf("") }

                // Reset busca ao alternar telas
                LaunchedEffect(currentScreen) {
                    isLibrarySearchOpen = false
                    librarySearchQuery = ""
                }

                // Controle do botão Voltar (BackHandler)
                BackHandler(enabled = drawerState.isOpen || (currentScreen is AppScreen.SubjectLibrary && isLibrarySearchOpen) || currentScreen !is AppScreen.Dashboard) {
                    if (drawerState.isOpen) {
                        scope.launch { drawerState.close() }
                    } else if (currentScreen is AppScreen.SubjectLibrary && isLibrarySearchOpen) {
                        isLibrarySearchOpen = false
                        librarySearchQuery = ""
                    } else {
                        when (val screen = currentScreen) {
                            is AppScreen.SubjectEditor -> {
                                viewModel.openSubject(screen.subjectId, screen.subjectName)
                            }
                            else -> {
                                viewModel.switchView(AppScreen.Dashboard)
                            }
                        }
                    }
                }

                // Título dinâmico do cabeçalho
                val screenTitle = when (val screen = currentScreen) {
                    is AppScreen.Dashboard -> "Dashboard de Produtividade"
                    is AppScreen.Calendar -> "Calendário & Planner"
                    is AppScreen.Questions -> "Banco de Questões"
                    is AppScreen.Flashcards -> "Flashcards (Anki)"
                    is AppScreen.Curriculum -> "Gestão Curricular"
                    is AppScreen.SubjectLibrary -> "Caderno: ${screen.subjectName}"
                    is AppScreen.SubjectEditor -> "Caderno: ${screen.subjectName}"
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        SidebarDrawer(
                            currentScreen = currentScreen,
                            semesters = semesters,
                            modules = modules,
                            subjects = subjects,
                            selectedSemesterId = selectedSemesterId,
                            expandedModuleIds = expandedModuleIds,
                            themeMode = themeMode,
                            onToggleTheme = { viewModel.toggleThemeMode() },
                            onSelectScreen = { screen -> viewModel.switchView(screen) },
                            onSelectSemester = { semId -> viewModel.selectSemester(semId) },
                            onToggleModule = { modId -> viewModel.toggleModuleAccordion(modId) },
                            onOpenSubject = { subId, subName -> viewModel.openSubject(subId, subName) },
                            onCloseDrawer = { scope.launch { drawerState.close() } }
                        )
                    }
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            if (currentScreen !is AppScreen.SubjectEditor) {
                                TopAppBar(
                                    title = {
                                        if (currentScreen is AppScreen.SubjectLibrary) {
                                            // Remoção do cabeçalho na tela de notas do caderno; exibe campo de busca expansível se ativo
                                            if (isLibrarySearchOpen) {
                                                Surface(
                                                    shape = RoundedCornerShape(20.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(40.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .padding(horizontal = 12.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Search,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        BasicTextField(
                                                            value = librarySearchQuery,
                                                            onValueChange = { librarySearchQuery = it },
                                                            singleLine = true,
                                                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            ),
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .testTag("appbar_search_input"),
                                                            decorationBox = { innerTextField ->
                                                                if (librarySearchQuery.isEmpty()) {
                                                                    Text(
                                                                        text = "Pesquisar...",
                                                                        style = MaterialTheme.typography.bodyMedium,
                                                                        color = MaterialTheme.colorScheme.outline
                                                                    )
                                                                }
                                                                innerTextField()
                                                            }
                                                        )
                                                        if (librarySearchQuery.isNotEmpty()) {
                                                            IconButton(
                                                                onClick = { librarySearchQuery = "" },
                                                                modifier = Modifier.size(24.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Close,
                                                                    contentDescription = "Limpar",
                                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            } else {
                                                // Cabeçalho removido conforme solicitado para manter visual limpo e minimalista
                                            }
                                        } else {
                                            Text(
                                                text = screenTitle,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                maxLines = 1
                                            )
                                        }
                                    },
                                    navigationIcon = {
                                        IconButton(
                                            onClick = {
                                                if (currentScreen is AppScreen.SubjectLibrary && isLibrarySearchOpen) {
                                                    isLibrarySearchOpen = false
                                                    librarySearchQuery = ""
                                                } else {
                                                    scope.launch { drawerState.open() }
                                                }
                                            },
                                            modifier = Modifier.testTag("drawer_hamburger_btn")
                                        ) {
                                            Icon(
                                                imageVector = if (currentScreen is AppScreen.SubjectLibrary && isLibrarySearchOpen) {
                                                    Icons.AutoMirrored.Filled.ArrowBack
                                                } else {
                                                    Icons.Default.Menu
                                                },
                                                contentDescription = if (currentScreen is AppScreen.SubjectLibrary && isLibrarySearchOpen) {
                                                    "Voltar"
                                                } else {
                                                    "Abrir menu lateral"
                                                }
                                            )
                                        }
                                    },
                                    actions = {
                                        // Função de pesquisar (lupa) ao lado do ícone de tema na página do caderno
                                        if (currentScreen is AppScreen.SubjectLibrary) {
                                            IconButton(
                                                onClick = {
                                                    isLibrarySearchOpen = !isLibrarySearchOpen
                                                    if (!isLibrarySearchOpen) {
                                                        librarySearchQuery = ""
                                                    }
                                                },
                                                modifier = Modifier.testTag("appbar_search_btn")
                                            ) {
                                                Icon(
                                                    imageVector = if (isLibrarySearchOpen) Icons.Default.Close else Icons.Default.Search,
                                                    contentDescription = if (isLibrarySearchOpen) "Fechar busca" else "Pesquisar",
                                                    tint = if (isLibrarySearchOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }

                                        // Botão de alternar Modo Claro / Modo Escuro
                                        IconButton(
                                            onClick = { viewModel.toggleThemeMode() },
                                            modifier = Modifier.testTag("appbar_theme_toggle_btn")
                                        ) {
                                            Icon(
                                                imageVector = if (themeMode == ThemeMode.LIGHT) Icons.Default.Brightness4 else Icons.Default.Brightness7,
                                                contentDescription = if (themeMode == ThemeMode.LIGHT) "Ativar Modo Escuro" else "Ativar Modo Claro",
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        },
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (val screen = currentScreen) {
                                is AppScreen.Dashboard -> {
                                    DashboardScreen(
                                        notesCount = notesCount,
                                        questionsCount = questionsCount,
                                        flashcardsCount = flashcardsCount,
                                        tasksWithBadges = tasksWithBadges,
                                        onAddTask = { title, date -> viewModel.addTask(title, date) },
                                        onToggleTask = { task -> viewModel.toggleTask(task) },
                                        onDeleteTask = { task -> viewModel.deleteTask(task) }
                                    )
                                }

                                is AppScreen.Calendar -> {
                                    CalendarScreen(
                                        currentYearMonth = currentYearMonth,
                                        selectedDate = selectedDate,
                                        todayString = viewModel.todayString,
                                        tasks = tasksWithBadges.map { it.task },
                                        events = allEvents,
                                        onSelectDate = { date -> viewModel.selectCalendarDate(date) },
                                        onPrevMonth = { viewModel.prevMonth() },
                                        onNextMonth = { viewModel.nextMonth() },
                                        onToggleTask = { task -> viewModel.toggleTask(task) },
                                        onDeleteEvent = { event -> viewModel.deleteCalendarEvent(event) },
                                        onAddEvent = { title, date, time, desc ->
                                            viewModel.addCalendarEvent(title, date, time, desc)
                                        }
                                    )
                                }

                                is AppScreen.SubjectLibrary -> {
                                    SubjectLibraryScreen(
                                        subjectId = screen.subjectId,
                                        subjectName = screen.subjectName,
                                        notes = savedNotes,
                                        firstSlideBlocks = firstSlideBlocks,
                                        searchQuery = librarySearchQuery,
                                        onNewNote = { viewModel.startNewNote(screen.subjectId, screen.subjectName) },
                                        onOpenNoteReadOnly = { note -> viewModel.openNoteForViewing(note, screen.subjectName) },
                                        onEditNoteDirectly = { note -> viewModel.openNoteForEditing(note, screen.subjectName) },
                                        onDeleteNote = { note -> viewModel.deleteNote(note) },
                                        onExportPdf = { note, subName -> viewModel.exportNoteById(this@MainActivity, note, subName) },
                                        onBackToDashboard = { viewModel.switchView(AppScreen.Dashboard) }
                                    )
                                }

                                is AppScreen.SubjectEditor -> {
                                    SubjectEditorScreen(
                                        subjectId = screen.subjectId,
                                        subjectName = screen.subjectName,
                                        editorMode = editorMode,
                                        slideViewerLayout = slideViewerLayout,
                                        noteTitle = noteTitle,
                                        freeTextContent = freeTextContent,
                                        slideBlocks = slideBlocks,
                                        activeNoteId = activeNoteId,
                                        savedNotes = savedNotes,
                                        allNotes = allNotes,
                                        subjects = subjects,
                                        isLoading = isLoading,
                                        isReadOnly = isReadOnly || screen.isReadOnly,
                                        onToggleReadOnly = { readOnly -> viewModel.setReadOnly(readOnly) },
                                        onBackToLibrary = { viewModel.openSubject(screen.subjectId, screen.subjectName) },
                                        onSetEditorMode = { mode -> viewModel.setEditorMode(mode) },
                                        onSetSlideViewerLayout = { layout -> viewModel.setSlideViewerLayout(layout) },
                                        onUpdateNoteTitle = { title -> viewModel.updateNoteTitle(title) },
                                        onUpdateFreeTextContent = { text -> viewModel.updateFreeTextContent(text) },
                                        onAddManualSlideBlock = { path -> viewModel.addManualSlideBlock(path) },
                                        onUpdateSlideBlockNotes = { idx, notes -> viewModel.updateSlideBlockNotes(idx, notes) },
                                        onUpdateSlideBlockImage = { idx, path -> viewModel.updateSlideBlockImage(idx, path) },
                                        onMoveSlideBlockUp = { idx -> viewModel.moveSlideBlockUp(idx) },
                                        onMoveSlideBlockDown = { idx -> viewModel.moveSlideBlockDown(idx) },
                                        onRemoveSlideBlock = { idx -> viewModel.removeSlideBlock(idx) },
                                        onExtractPdf = { ctx, uri -> viewModel.extractPdfAndCreateBlocks(ctx, uri) },
                                        onSaveNote = { viewModel.saveCurrentNote() },
                                        onAutoSave = { viewModel.autoSaveCurrentNote() },
                                        isAutoSaving = isAutoSaving,
                                        onLoadNote = { note -> viewModel.openNoteForEditing(note, screen.subjectName) },
                                        onDeleteNote = { note -> viewModel.deleteNote(note) },
                                        onNewNote = { viewModel.startNewNote(screen.subjectId, screen.subjectName) },
                                        onExportPdf = { ctx -> viewModel.exportCurrentNotePdf(ctx) },
                                        onExportSpecificPdf = { note, subName -> viewModel.exportNoteById(this@MainActivity, note, subName) }
                                    )
                                }

                                is AppScreen.Questions -> {
                                    QuestionsScreen(
                                        questions = allQuestions,
                                        subjects = subjects,
                                        selectedSubjectFilter = questionFilter,
                                        revealedQuestionIds = revealedQuestions,
                                        onFilterSubject = { id -> viewModel.setQuestionSubjectFilter(id) },
                                        onToggleReveal = { id -> viewModel.toggleRevealQuestion(id) },
                                        onAddQuestion = { subId, subName, p, a, b, c, d, corr, exp ->
                                            viewModel.addQuestion(subId, subName, p, a, b, c, d, corr, exp)
                                        },
                                        onDeleteQuestion = { q -> viewModel.deleteQuestion(q) },
                                        onDownloadTemplate = { ctx -> viewModel.downloadQuestionsTemplate(ctx) },
                                        onImportCsv = { stream -> viewModel.importQuestionsFromCsv(stream) }
                                    )
                                }

                                is AppScreen.Flashcards -> {
                                    FlashcardsScreen(
                                        flashcards = allFlashcards,
                                        subjects = subjects,
                                        selectedSubjectFilter = flashcardFilter,
                                        flippedCardIds = flippedFlashcards,
                                        onFilterSubject = { id -> viewModel.setFlashcardSubjectFilter(id) },
                                        onToggleFlip = { id -> viewModel.toggleFlashcardFlip(id) },
                                        onAddFlashcard = { subId, subName, front, back ->
                                            viewModel.addFlashcard(subId, subName, front, back)
                                        },
                                        onDeleteFlashcard = { fc -> viewModel.deleteFlashcard(fc) },
                                        onDownloadTemplate = { ctx -> viewModel.downloadFlashcardsTemplate(ctx) },
                                        onImportCsv = { stream -> viewModel.importFlashcardsFromCsv(stream) }
                                    )
                                }

                                is AppScreen.Curriculum -> {
                                    CurriculumManagerScreen(
                                        semesters = semesters,
                                        modules = modules,
                                        subjects = subjects,
                                        selectedSemesterId = selectedSemesterId,
                                        onSelectSemester = { id -> viewModel.selectSemester(id) },
                                        onAddSemester = { name -> viewModel.addSemester(name) },
                                        onRenameSemester = { sem, newName -> viewModel.renameSemester(sem, newName) },
                                        onDeleteSemester = { sem -> viewModel.deleteSemester(sem) },
                                        onAddModule = { semId, name -> viewModel.addModule(semId, name) },
                                        onDeleteModule = { mod -> viewModel.deleteModule(mod) },
                                        onAddSubject = { modId, name -> viewModel.addSubject(modId, name) },
                                        onDeleteSubject = { sub -> viewModel.deleteSubject(sub) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun shareFile(uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = contentResolver.getType(uri) ?: "*/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Compartilhar arquivo"))
    }
}
