package com.example.ui.screens

import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.ViewHeadline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.SlideBlockEntity
import com.example.data.local.entity.SubjectEntity
import com.example.ui.components.DirectTextAnnotator
import com.example.ui.components.HandwritingCanvas
import com.example.ui.components.PenToolbarRow
import com.example.ui.components.RuledNotebookBackground
import com.example.ui.components.SavedNotesLibraryDialog
import com.example.ui.components.UnifiedNoteDocument
import com.example.ui.components.WysiwygToolbar
import com.example.ui.components.rememberAnnotatorController
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.delay
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File
import java.io.FileOutputStream

@Composable
fun SubjectEditorScreen(
    subjectId: Long,
    subjectName: String,
    editorMode: String,
    slideViewerLayout: String,
    noteTitle: String,
    freeTextContent: String,
    noteDrawingData: String = "",
    slideBlocks: List<SlideBlockEntity>,
    activeNoteId: Long,
    savedNotes: List<NoteEntity>,
    allNotes: List<NoteEntity>,
    subjects: List<SubjectEntity>,
    isLoading: Boolean,
    isReadOnly: Boolean = false,
    onToggleReadOnly: (Boolean) -> Unit = {},
    onBackToLibrary: () -> Unit = {},
    onSetEditorMode: (String) -> Unit,
    onSetSlideViewerLayout: (String) -> Unit,
    onUpdateNoteTitle: (String) -> Unit,
    onUpdateFreeTextContent: (String) -> Unit,
    onUpdateNoteDrawingData: (String) -> Unit = {},
    onAddManualSlideBlock: (String?) -> Unit,
    onUpdateSlideBlockNotes: (Int, String) -> Unit,
    onUpdateSlideBlockDrawingData: (Int, String) -> Unit = { _, _ -> },
    onUpdateSlideBlockImage: (Int, String) -> Unit,
    onMoveSlideBlockUp: (Int) -> Unit,
    onMoveSlideBlockDown: (Int) -> Unit,
    onRemoveSlideBlock: (Int) -> Unit,
    onExtractPdf: (Context, Uri) -> Unit,
    onSaveNote: () -> Unit,
    onAutoSave: () -> Unit = {},
    isAutoSaving: Boolean = false,
    onLoadNote: (NoteEntity) -> Unit,
    onDeleteNote: (NoteEntity) -> Unit,
    onNewNote: () -> Unit,
    onExportPdf: (Context) -> Unit,
    onExportSpecificPdf: (NoteEntity, String) -> Unit
) {
    val context = LocalContext.current
    var showLibraryDialog by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Modo de caneta / riscar diretamente sobre o texto do resumo livre
    var isFreePenActive by remember { mutableStateOf(false) }
    val freePenController = rememberAnnotatorController()
    var isHeaderHidden by remember { mutableStateOf(false) }
    var isAnyBlockDrawing by remember { mutableStateOf(false) }
    var isLocalAutoSaving by remember { mutableStateOf(false) }

    // Salvamento automático transparente com debounce de 1 segundo
    LaunchedEffect(freeTextContent, noteDrawingData, noteTitle, slideBlocks) {
        if (!isReadOnly && (freeTextContent.isNotBlank() || noteDrawingData.isNotBlank() || slideBlocks.isNotEmpty() || noteTitle.isNotBlank())) {
            delay(1000)
            isLocalAutoSaving = true
            onAutoSave()
            delay(500)
            isLocalAutoSaving = false
        }
    }

    // Launcher para selecionar arquivo PDF da aula
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onExtractPdf(context, uri)
        }
    }

    // Modal Gerenciador de Notas Salvas
    if (showLibraryDialog) {
        SavedNotesLibraryDialog(
            notes = if (allNotes.isNotEmpty()) allNotes else savedNotes,
            subjects = subjects,
            currentSubjectId = subjectId,
            onDismiss = { showLibraryDialog = false },
            onSelectNote = { note ->
                onLoadNote(note)
                showLibraryDialog = false
            },
            onDeleteNote = onDeleteNote,
            onExportPdf = onExportSpecificPdf,
            onNewNote = {
                onNewNote()
                showLibraryDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (isLandscape) 20.dp else 12.dp)
            .testTag("subject_editor_screen")
    ) {
        // BARRA SUPERIOR MINIMALISTA ESTILO SAMSUNG NOTES (COM OPÇÃO DE OCULTAR / EXPANDIR)
        if (isHeaderHidden) {
            // MODO OCULTO: Pílula flutuante ultrafina ocupando espaço zero (28dp de altura)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                tonalElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .testTag("compact_floating_header_pill")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBackToLibrary,
                            modifier = Modifier.size(24.dp).testTag("btn_back_to_subject_library")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar",
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = noteTitle.ifBlank { "Sem Título" },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.widthIn(max = 200.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (editorMode == "FREE") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = if (editorMode == "FREE") "Resumo" else "Slides",
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = if (editorMode == "FREE") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Indicador de Salvamento Automático
                        val isSaving = isAutoSaving || isLocalAutoSaving
                        AnimatedContent(
                            targetState = isSaving,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "auto_save_pill"
                        ) { saving ->
                            if (saving) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                                    CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.5.dp, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Salvando", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(11.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Salvo", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        if (!isReadOnly) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable { onSaveNote() }
                                    .testTag("save_note_btn_compact")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Salvar", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp), color = Color.White)
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable { onToggleReadOnly(false) }
                                    .testTag("btn_edit_note_compact")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Editar", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp), color = Color.White)
                                }
                            }
                        }

                        IconButton(
                            onClick = { isHeaderHidden = false },
                            modifier = Modifier.size(26.dp).testTag("btn_expand_header")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FullscreenExit,
                                contentDescription = "Expandir",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        } else {
            // MODO VISÍVEL: Barra única, compacta e moderna (altura 40dp)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                tonalElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .testTag("samsung_notes_header")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Esquerda: Botão Voltar + Título da Nota
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1.3f)
                    ) {
                        IconButton(
                            onClick = onBackToLibrary,
                            modifier = Modifier.size(30.dp).testTag("btn_back_to_subject_library")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar",
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        if (isReadOnly) {
                            Column {
                                Text(
                                    text = noteTitle.ifBlank { "Sem Título" },
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = subjectName,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.outline,
                                    maxLines = 1
                                )
                            }
                        } else {
                            OutlinedTextField(
                                value = noteTitle,
                                onValueChange = onUpdateNoteTitle,
                                placeholder = { Text("Título da aula...", fontSize = 12.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                                    .testTag("note_title_input"),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Centro: Seletor Cápsula Samsung One UI (Resumo vs Slides)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isFreeSelected = editorMode == "FREE"
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isFreeSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                            tonalElevation = if (isFreeSelected) 1.dp else 0.dp,
                            modifier = Modifier
                                .clickable { onSetEditorMode("FREE") }
                                .testTag("tab_free_mode")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = if (isFreeSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Resumo",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isFreeSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    ),
                                    color = if (isFreeSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        val isSlidesSelected = editorMode == "SLIDES"
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSlidesSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                            tonalElevation = if (isSlidesSelected) 1.dp else 0.dp,
                            modifier = Modifier
                                .clickable { onSetEditorMode("SLIDES") }
                                .testTag("tab_slides_mode")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    tint = if (isSlidesSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Slides",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSlidesSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    ),
                                    color = if (isSlidesSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Direita: Indicador de Auto-save + Ações
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Indicador de Salvamento Automático
                        val isSaving = isAutoSaving || isLocalAutoSaving
                        AnimatedContent(
                            targetState = isSaving,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "auto_save_header"
                        ) { saving ->
                            if (saving) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                                    CircularProgressIndicator(modifier = Modifier.size(11.dp), strokeWidth = 1.5.dp, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Salvando", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Salvo", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        if (isReadOnly) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable { onToggleReadOnly(false) }
                                    .testTag("btn_edit_note")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Editar", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp), color = Color.White)
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable { onSaveNote() }
                                    .testTag("save_note_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Salvar", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp), color = Color.White)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clickable {
                                        onSaveNote()
                                        onToggleReadOnly(true)
                                    }
                                    .testTag("btn_finish_editing")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Concluir", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp))
                                }
                            }
                        }

                        // Botão Exportar PDF
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFDC2626).copy(alpha = 0.9f),
                            modifier = Modifier
                                .clickable { onExportPdf(context) }
                                .testTag("export_pdf_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("PDF", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp), color = Color.White)
                            }
                        }

                        // Botão Ocultar Barra (Modo Tela Cheia Imersivo Samsung)
                        IconButton(
                            onClick = { isHeaderHidden = true },
                            modifier = Modifier.size(30.dp).testTag("btn_hide_header")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "Ocultar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Indicador de Carregamento (extração de PDF)
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Processando slides em alta resolução...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // CONTEÚDO BASEADO NO MODO ATIVO (RESUMO LIVRE OU SLIDES)
        if (editorMode == "FREE") {
            // MODO 1: RESUMO LIVRE COM ANOTAÇÃO DIRETA SOBRE O TEXTO (S-PEN / CANETA)
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                    // BARRA SUPERIOR DO RESUMO: Formatação WYSIWYG e Caneta com microanimações
                    AnimatedContent(
                        targetState = isFreePenActive,
                        transitionSpec = {
                            fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                                    fadeOut(spring(stiffness = Spring.StiffnessMediumLow))
                        },
                        label = "free_toolbar_animation"
                    ) { penActive ->
                        if (penActive) {
                            PenToolbarRow(
                                controller = freePenController,
                                onClosePen = { isFreePenActive = false }
                            )
                        } else if (!isReadOnly) {
                            WysiwygToolbar(
                                onApplyFormat = { prefix, suffix ->
                                    onUpdateFreeTextContent(freeTextContent + "$prefix$suffix")
                                },
                                onApplyColor = { colorName ->
                                    val tag = when (colorName) {
                                        "azul" -> "🔵 "
                                        "vermelho" -> "🔴 "
                                        "verde" -> "🟢 "
                                        else -> ""
                                    }
                                    onUpdateFreeTextContent(freeTextContent + tag)
                                },
                                onApplyHighlight = { markName ->
                                    val mark = when (markName) {
                                        "amarelo" -> " [MARCA:AMARELO] "
                                        "verde" -> " [MARCA:VERDE] "
                                        "rosa" -> " [MARCA:ROSA] "
                                        else -> ""
                                    }
                                    onUpdateFreeTextContent(freeTextContent + mark)
                                },
                                onClearFormat = {
                                    val clean = freeTextContent
                                        .replace("**", "")
                                        .replace("*", "")
                                        .replace("<u>", "")
                                        .replace("</u>", "")
                                        .replace("[MARCA:AMARELO]", "")
                                        .replace("[MARCA:VERDE]", "")
                                        .replace("[MARCA:ROSA]", "")
                                    onUpdateFreeTextContent(clean)
                                },
                                onInsertList = { isNumbered ->
                                    val listPrefix = if (isNumbered) "\n1. " else "\n• "
                                    onUpdateFreeTextContent(freeTextContent + listPrefix)
                                },
                                isPenActive = false,
                                onTogglePen = { isFreePenActive = true }
                            )
                        } else {
                            // Em modo leitura, botão para ativar anotação com S-Pen
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .clickable { isFreePenActive = true }
                                        .testTag("free_read_only_pen_toggle")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Gesture,
                                            contentDescription = "Caneta",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Caneta",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // ANOTAÇÃO DIRETA UNIFICADA: Texto e traços rolam em sincronia absoluta
                    val freeScrollState = rememberScrollState()
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(freeScrollState, enabled = !freePenController.isCurrentlyDrawing)
                    ) {
                        DirectTextAnnotator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 720.dp)
                                .testTag("free_text_annotator"),
                            drawingData = noteDrawingData,
                            isReadOnly = isReadOnly,
                            isPenActive = isFreePenActive,
                            controller = freePenController,
                            minCanvasHeight = 720.dp,
                            onDrawingChanged = onUpdateNoteDrawingData
                        ) { isPenMode ->
                            UnifiedNoteDocument(
                                modifier = Modifier.fillMaxWidth(),
                                text = freeTextContent,
                                onTextChanged = onUpdateFreeTextContent,
                                isPenMode = isPenMode,
                                isReadOnly = isReadOnly,
                                minHeight = 720.dp,
                                placeholder = "Anotações da aula..."
                            )
                        }
                    }
                }
            }
        } else {
            // MODO 2: AULA COM SLIDES (CADA BLOCO TEM OPÇÃO DE CANETA/RISCAR COM S-PEN)
            Column(modifier = Modifier.fillMaxSize()) {
                // Barra de Controles dos Slides: Importar PDF + Adicionar Bloco (Apenas em Edição) + Layout
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (!isReadOnly) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Importar PDF
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier
                                    .clickable { pdfPickerLauncher.launch("application/pdf") }
                                    .testTag("btn_import_pdf_slides")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "PDF",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Adicionar Bloco Manual
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clickable { onAddManualSlideBlock(null) }
                                    .testTag("btn_add_manual_slide_block")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Novo",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "${slideBlocks.size} slides",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Visualizador Multilayout: Split, Linear, Só Texto
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        LayoutModeIconButton(
                            icon = Icons.Default.ViewColumn,
                            tooltip = "Lado a Lado (Split)",
                            isSelected = slideViewerLayout == "SPLIT",
                            testTag = "layout_split_btn",
                            onClick = { onSetSlideViewerLayout("SPLIT") }
                        )

                        LayoutModeIconButton(
                            icon = Icons.Default.ViewAgenda,
                            tooltip = "Texto Corrido (Linear)",
                            isSelected = slideViewerLayout == "LINEAR",
                            testTag = "layout_linear_btn",
                            onClick = { onSetSlideViewerLayout("LINEAR") }
                        )

                        LayoutModeIconButton(
                            icon = Icons.Default.ViewHeadline,
                            tooltip = "Só Texto",
                            isSelected = slideViewerLayout == "TEXT_ONLY",
                            testTag = "layout_text_only_btn",
                            onClick = { onSetSlideViewerLayout("TEXT_ONLY") }
                        )
                    }
                }

                // Lista de Blocos de Slides
                if (slideBlocks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Nenhum slide adicionado nesta aula",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isReadOnly) "Toque em 'Editar' para extrair um PDF ou adicionar slides." else "Clique em 'Extrair PDF' para carregar a aula ou 'Novo Slide +' para criar blocos.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 16.dp),
                        userScrollEnabled = !isAnyBlockDrawing,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        itemsIndexed(slideBlocks) { index, block ->
                            SlideBlockItem(
                                index = index,
                                totalCount = slideBlocks.size,
                                block = block,
                                layout = slideViewerLayout,
                                isLandscape = isLandscape,
                                isReadOnly = isReadOnly,
                                onUpdateNotes = { onUpdateSlideBlockNotes(index, it) },
                                onUpdateDrawing = { onUpdateSlideBlockDrawingData(index, it) },
                                onUpdateImage = { onUpdateSlideBlockImage(index, it) },
                                onMoveUp = { onMoveSlideBlockUp(index) },
                                onMoveDown = { onMoveSlideBlockDown(index) },
                                onRemove = { onRemoveSlideBlock(index) },
                                onDrawingStateChange = { isDrawing -> isAnyBlockDrawing = isDrawing }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LayoutModeIconButton(
    icon: ImageVector,
    tooltip: String,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = tooltip,
            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SlideBlockItem(
    index: Int,
    totalCount: Int,
    block: SlideBlockEntity,
    layout: String, // "SPLIT", "LINEAR", "TEXT_ONLY"
    isLandscape: Boolean,
    isReadOnly: Boolean,
    onUpdateNotes: (String) -> Unit,
    onUpdateDrawing: (String) -> Unit,
    onUpdateImage: (String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onDrawingStateChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    var isBlockPenActive by remember { mutableStateOf(false) }
    val blockPenController = rememberAnnotatorController()
    var showFullscreenSlide by remember { mutableStateOf(false) }

    // Notificar a lista pai quando a caneta estiver desenhando ativamente
    androidx.compose.runtime.LaunchedEffect(blockPenController.isCurrentlyDrawing) {
        onDrawingStateChange(blockPenController.isCurrentlyDrawing)
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val input = context.contentResolver.openInputStream(uri)
                val destFile = File(context.filesDir, "block_img_${System.currentTimeMillis()}.jpg")
                FileOutputStream(destFile).use { out ->
                    input?.copyTo(out)
                }
                onUpdateImage(destFile.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Modal de Slide em Tela Cheia / Zoom
    if (showFullscreenSlide && !block.imagePath.isNullOrBlank() && File(block.imagePath).exists()) {
        Dialog(
            onDismissRequest = { showFullscreenSlide = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.94f))
            ) {
                AsyncImage(
                    model = File(block.imagePath),
                    contentDescription = "Slide ${index + 1} em Tela Cheia",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )

                // Barra superior com título e botão fechar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.65f)
                    ) {
                        Text(
                            text = "Slide ${index + 1} de $totalCount (Visualização Completa)",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    IconButton(
                        onClick = { showFullscreenSlide = false },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar tela cheia",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }

    // Funções de formatação atreladas diretamente a ESTE bloco
    fun applyFormatToThisBlock(prefix: String, suffix: String) {
        onUpdateNotes(block.notesContent + "$prefix$suffix")
    }

    fun applyColorToThisBlock(colorName: String) {
        val tag = when (colorName) {
            "azul" -> "🔵 "
            "vermelho" -> "🔴 "
            "verde" -> "🟢 "
            else -> ""
        }
        onUpdateNotes(block.notesContent + tag)
    }

    fun applyHighlightToThisBlock(highlightName: String) {
        val mark = when (highlightName) {
            "amarelo" -> " [MARCA:AMARELO] "
            "verde" -> " [MARCA:VERDE] "
            "rosa" -> " [MARCA:ROSA] "
            else -> ""
        }
        onUpdateNotes(block.notesContent + mark)
    }

    fun insertListToThisBlock(isNumbered: Boolean) {
        val listPrefix = if (isNumbered) "\n1. " else "\n• "
        onUpdateNotes(block.notesContent + listPrefix)
    }

    fun clearFormatThisBlock() {
        val clean = block.notesContent
            .replace("**", "")
            .replace("*", "")
            .replace("<u>", "")
            .replace("</u>", "")
            .replace("[MARCA:AMARELO]", "")
            .replace("[MARCA:VERDE]", "")
            .replace("[MARCA:ROSA]", "")
        onUpdateNotes(clean)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("slide_block_$index"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Barra de cabeçalho do Bloco: Slide # e Controles
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Slide ${index + 1}/$totalCount",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isBlockPenActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isBlockPenActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .clickable { isBlockPenActive = !isBlockPenActive }
                            .testTag("toggle_pen_block_$index")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Gesture,
                                contentDescription = "Caneta S-Pen",
                                tint = if (isBlockPenActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            if (block.drawingData.isNotBlank() && !isBlockPenActive) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    }
                }

                if (!isReadOnly) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = onMoveUp,
                            enabled = index > 0,
                            modifier = Modifier.size(30.dp).testTag("move_up_block_$index")
                        ) {
                            Icon(
                                Icons.Default.ArrowUpward,
                                contentDescription = "Mover para cima",
                                tint = if (index > 0) MaterialTheme.colorScheme.onSurfaceVariant else Color.LightGray,
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        IconButton(
                            onClick = onMoveDown,
                            enabled = index < totalCount - 1,
                            modifier = Modifier.size(30.dp).testTag("move_down_block_$index")
                        ) {
                            Icon(
                                Icons.Default.ArrowDownward,
                                contentDescription = "Mover para baixo",
                                tint = if (index < totalCount - 1) MaterialTheme.colorScheme.onSurfaceVariant else Color.LightGray,
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        IconButton(
                            onClick = onRemove,
                            modifier = Modifier.size(30.dp).testTag("remove_block_$index")
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Excluir bloco",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // RENDERIZAÇÃO CONFORME O LAYOUT SELECIONADO:
            val effectiveLayout = if (isLandscape && layout != "TEXT_ONLY") "SPLIT" else layout

            when (effectiveLayout) {
                "SPLIT" -> {
                    // Layout Lado a Lado (Split): Imagem grande à esquerda, anotação/texto à direita
                    val splitHeight = if (isLandscape) 520.dp else 440.dp

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(splitHeight),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 1. Coluna do Slide (Mais ampla para visualização clara de detalhes médicos)
                        Box(
                            modifier = Modifier
                                .weight(1.25f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                                .clickable {
                                    if (!block.imagePath.isNullOrBlank() && File(block.imagePath).exists()) {
                                        showFullscreenSlide = true
                                    } else if (!isReadOnly) {
                                        imagePickerLauncher.launch("image/*")
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!block.imagePath.isNullOrBlank() && File(block.imagePath).exists()) {
                                AsyncImage(
                                    model = File(block.imagePath),
                                    contentDescription = "Slide ${index + 1}",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Botão de Tela Cheia / Zoom no canto superior do Slide
                                IconButton(
                                    onClick = { showFullscreenSlide = true },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.55f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fullscreen,
                                        contentDescription = "Ver slide em tela cheia",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(40.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isReadOnly) "Sem imagem para este slide" else "Toque para escolher imagem do slide",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }

                        // 2. Coluna das Anotações com Caneta S-Pen / Texto
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            AnimatedContent(
                                targetState = isBlockPenActive,
                                transitionSpec = {
                                    fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                                            fadeOut(spring(stiffness = Spring.StiffnessMediumLow))
                                },
                                label = "split_pen_toolbar_$index"
                            ) { penActive ->
                                if (penActive) {
                                    PenToolbarRow(
                                        controller = blockPenController,
                                        onClosePen = { isBlockPenActive = false }
                                    )
                                } else if (!isReadOnly) {
                                    WysiwygToolbar(
                                        onApplyFormat = { p, s -> applyFormatToThisBlock(p, s) },
                                        onApplyColor = { c -> applyColorToThisBlock(c) },
                                        onApplyHighlight = { h -> applyHighlightToThisBlock(h) },
                                        onClearFormat = { clearFormatThisBlock() },
                                        onInsertList = { num -> insertListToThisBlock(num) },
                                        isPenActive = false,
                                        onTogglePen = { isBlockPenActive = true }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))

                            val splitScrollState = rememberScrollState()
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(splitScrollState, enabled = !blockPenController.isCurrentlyDrawing)
                            ) {
                                DirectTextAnnotator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = splitHeight - 60.dp),
                                    drawingData = block.drawingData,
                                    isReadOnly = isReadOnly,
                                    isPenActive = isBlockPenActive,
                                    controller = blockPenController,
                                    minCanvasHeight = splitHeight - 60.dp,
                                    onDrawingChanged = onUpdateDrawing
                                ) { isPenMode ->
                                    UnifiedNoteDocument(
                                        modifier = Modifier.fillMaxWidth(),
                                        text = block.notesContent,
                                        onTextChanged = onUpdateNotes,
                                        isPenMode = isPenMode,
                                        isReadOnly = isReadOnly,
                                        minHeight = splitHeight - 60.dp,
                                        placeholder = "Anotações do Slide ${index + 1}..."
                                    )
                                }
                            }
                        }
                    }
                }

                "LINEAR" -> {
                    // Layout Texto Corrido (Linear): Imagem no topo grande (440dp), texto com anotação embaixo
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(440.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                                .clickable {
                                    if (!block.imagePath.isNullOrBlank() && File(block.imagePath).exists()) {
                                        showFullscreenSlide = true
                                    } else if (!isReadOnly) {
                                        imagePickerLauncher.launch("image/*")
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!block.imagePath.isNullOrBlank() && File(block.imagePath).exists()) {
                                AsyncImage(
                                    model = File(block.imagePath),
                                    contentDescription = "Slide ${index + 1}",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )

                                IconButton(
                                    onClick = { showFullscreenSlide = true },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.55f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fullscreen,
                                        contentDescription = "Ver slide em tela cheia",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(40.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isReadOnly) "Sem imagem para este slide" else "Toque para escolher imagem do slide",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        AnimatedContent(
                            targetState = isBlockPenActive,
                            transitionSpec = {
                                fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                                        fadeOut(spring(stiffness = Spring.StiffnessMediumLow))
                            },
                            label = "linear_pen_toolbar_$index"
                        ) { penActive ->
                            if (penActive) {
                                PenToolbarRow(
                                    controller = blockPenController,
                                    onClosePen = { isBlockPenActive = false }
                                )
                            } else if (!isReadOnly) {
                                WysiwygToolbar(
                                    onApplyFormat = { p, s -> applyFormatToThisBlock(p, s) },
                                    onApplyColor = { c -> applyColorToThisBlock(c) },
                                    onApplyHighlight = { h -> applyHighlightToThisBlock(h) },
                                    onClearFormat = { clearFormatThisBlock() },
                                    onInsertList = { num -> insertListToThisBlock(num) },
                                    isPenActive = false,
                                    onTogglePen = { isBlockPenActive = true }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))

                        DirectTextAnnotator(
                            modifier = Modifier.fillMaxWidth(),
                            drawingData = block.drawingData,
                            isReadOnly = isReadOnly,
                            isPenActive = isBlockPenActive,
                            controller = blockPenController,
                            minCanvasHeight = 360.dp,
                            onDrawingChanged = onUpdateDrawing
                        ) { isPenMode ->
                            UnifiedNoteDocument(
                                modifier = Modifier.fillMaxWidth(),
                                text = block.notesContent,
                                onTextChanged = onUpdateNotes,
                                isPenMode = isPenMode,
                                isReadOnly = isReadOnly,
                                minHeight = 360.dp,
                                placeholder = "Anotações do Slide ${index + 1}..."
                            )
                        }
                    }
                }

                "TEXT_ONLY" -> {
                    // Só Texto com Anotação Ampla
                    Column(modifier = Modifier.fillMaxWidth()) {
                        AnimatedContent(
                            targetState = isBlockPenActive,
                            transitionSpec = {
                                fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                                        fadeOut(spring(stiffness = Spring.StiffnessMediumLow))
                            },
                            label = "text_only_pen_toolbar_$index"
                        ) { penActive ->
                            if (penActive) {
                                PenToolbarRow(
                                    controller = blockPenController,
                                    onClosePen = { isBlockPenActive = false }
                                )
                            } else if (!isReadOnly) {
                                WysiwygToolbar(
                                    onApplyFormat = { p, s -> applyFormatToThisBlock(p, s) },
                                    onApplyColor = { c -> applyColorToThisBlock(c) },
                                    onApplyHighlight = { h -> applyHighlightToThisBlock(h) },
                                    onClearFormat = { clearFormatThisBlock() },
                                    onInsertList = { num -> insertListToThisBlock(num) },
                                    isPenActive = false,
                                    onTogglePen = { isBlockPenActive = true }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))

                        DirectTextAnnotator(
                            modifier = Modifier.fillMaxWidth(),
                            drawingData = block.drawingData,
                            isReadOnly = isReadOnly,
                            isPenActive = isBlockPenActive,
                            controller = blockPenController,
                            minCanvasHeight = 420.dp,
                            onDrawingChanged = onUpdateDrawing
                        ) { isPenMode ->
                            UnifiedNoteDocument(
                                modifier = Modifier.fillMaxWidth(),
                                text = block.notesContent,
                                onTextChanged = onUpdateNotes,
                                isPenMode = isPenMode,
                                isReadOnly = isReadOnly,
                                minHeight = 420.dp,
                                placeholder = "Anotações do Slide ${index + 1}..."
                            )
                        }
                    }
                }
            }
        }
    }
}
