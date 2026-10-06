package com.example.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.SlideBlockEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SubjectLibraryScreen(
    subjectId: Long,
    subjectName: String,
    notes: List<NoteEntity>,
    firstSlideBlocks: Map<Long, SlideBlockEntity>,
    searchQuery: String = "",
    onNewNote: () -> Unit,
    onOpenNoteReadOnly: (NoteEntity) -> Unit,
    onEditNoteDirectly: (NoteEntity) -> Unit,
    onDeleteNote: (NoteEntity) -> Unit,
    onExportPdf: (NoteEntity, String) -> Unit,
    onBackToDashboard: () -> Unit = {}
) {
    var selectedFilterMode by remember { mutableStateOf("ALL") } // "ALL", "FREE", "SLIDES"
    var noteToDelete by remember { mutableStateOf<NoteEntity?>(null) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Diálogo de confirmação de exclusão
    if (noteToDelete != null) {
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("Excluir Anotação?", fontWeight = FontWeight.Bold) },
            text = { Text("Deseja realmente excluir '${noteToDelete?.title}' deste caderno?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        noteToDelete?.let { onDeleteNote(it) }
                        noteToDelete = null
                    }
                ) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) { Text("Cancelar") }
            }
        )
    }

    // Filtragem de notas com base na busca
    val effectiveQuery = searchQuery.trim()
    val filteredNotes = notes.filter { note ->
        val matchesSearch = effectiveQuery.isEmpty() ||
                note.title.contains(effectiveQuery, ignoreCase = true) ||
                note.freeTextContent.contains(effectiveQuery, ignoreCase = true)
        val matchesMode = when (selectedFilterMode) {
            "FREE" -> note.mode == "FREE"
            "SLIDES" -> note.mode == "SLIDES"
            else -> true
        }
        matchesSearch && matchesMode
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (isLandscape) 20.dp else 14.dp)
            .testTag("subject_library_screen")
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // FILTROS CHIP MINIMALISTAS
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedFilterMode == "ALL",
                    onClick = { selectedFilterMode = "ALL" },
                    label = { Text("Todas (${notes.size})") }
                )
            }
            item {
                val freeCount = notes.count { it.mode == "FREE" }
                FilterChip(
                    selected = selectedFilterMode == "FREE",
                    onClick = { selectedFilterMode = "FREE" },
                    label = { Text("Resumos ($freeCount)") }
                )
            }
            item {
                val slidesCount = notes.count { it.mode == "SLIDES" }
                FilterChip(
                    selected = selectedFilterMode == "SLIDES",
                    onClick = { selectedFilterMode = "SLIDES" },
                    label = { Text("Slides ($slidesCount)") }
                )
            }
        }

        if (effectiveQuery.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${filteredNotes.size} nota(s)",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // GRADE DE NOTAS (Estilo Samsung Notes com o card "+" de criar novas notas)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = if (isLandscape) 175.dp else 155.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("notes_samsung_grid"),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // ITEM 1: QUADRADO COM SÍMBOLO + PARA FAZER NOVAS NOTAS (leva ao atual caderno)
            item(key = "create_new_note_square_card") {
                CreateNewNoteSquareCard(
                    subjectName = subjectName,
                    onClick = onNewNote
                )
            }

            // ITENS 2..N: CARDS DAS NOTAS FEITAS COM PRÉ-VISUALIZAÇÃO DA PRIMEIRA PÁGINA E TÍTULO LOGO EMBAIXO
            items(filteredNotes, key = { it.id }) { note ->
                val firstSlide = firstSlideBlocks[note.id]
                SamsungNoteCard(
                    note = note,
                    firstSlideBlock = firstSlide,
                    subjectName = subjectName,
                    onClick = { onOpenNoteReadOnly(note) },
                    onEdit = { onEditNoteDirectly(note) },
                    onExportPdf = { onExportPdf(note, subjectName) },
                    onDelete = { noteToDelete = note }
                )
            }
        }
    }
}

/**
 * Quadrado com o símbolo + (semelhante ao Notes da Samsung) para criar novas notas
 */
@Composable
private fun CreateNewNoteSquareCard(
    subjectName: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .clickable(onClick = onClick)
            .testTag("btn_create_new_note_square"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.5.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Criar nova nota",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Nova Nota",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Toque para escrever",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Card de nota feito no estilo Samsung Notes:
 * - Pré-visualização da primeira página (slide 1 ou página de papel com anotações)
 * - Título logo embaixo
 * - Toque abre no modo de visualização apenas
 */
@Composable
private fun SamsungNoteCard(
    note: NoteEntity,
    firstSlideBlock: SlideBlockEntity?,
    subjectName: String,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onExportPdf: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val formattedDate = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(note.updatedAt))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .clickable(onClick = onClick)
            .testTag("note_card_${note.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. ÁREA SUPERIOR: PRÉ-VISUALIZAÇÃO DA PRIMEIRA PÁGINA (Samsung Notes thumbnail)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.35f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(
                        if (note.mode == "SLIDES") Color(0xFF1E293B) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    )
            ) {
                if (note.mode == "SLIDES") {
                    // Pré-visualização do Primeiro Slide
                    val imagePath = firstSlideBlock?.imagePath
                    if (!imagePath.isNullOrBlank() && File(imagePath).exists()) {
                        AsyncImage(
                            model = File(imagePath),
                            contentDescription = "Primeira página de ${note.title}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Slide sem imagem / placeholder elegante
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Slide 1",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            if (!firstSlideBlock?.notesContent.isNullOrBlank()) {
                                Text(
                                    text = firstSlideBlock?.notesContent?.replace("\n", " ") ?: "",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                    color = Color.LightGray,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Badge de Slides no canto superior
                    Surface(
                        shape = RoundedCornerShape(bottomEnd = 8.dp),
                        color = Color(0xFF7C3AED).copy(alpha = 0.9f),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = "🖼 Slides",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            ),
                            color = Color.White
                        )
                    }
                } else {
                    // MODO FREE: Folha de Papel Lined / Document Thumbnail realista
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF2563EB).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "📝 Resumo",
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    ),
                                    color = Color(0xFF1D4ED8)
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                modifier = Modifier.size(13.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Linhas de texto simulando folha de anotações
                        if (note.freeTextContent.isNotBlank()) {
                            Text(
                                text = note.freeTextContent.trim(),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                maxLines = 5,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = "Sem conteúdo de texto...",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 1.dp
            )

            // 2. ÁREA INFERIOR: TÍTULO LOGO EMBAIXO (como no Samsung Notes)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.95f)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // TÍTULO DA NOTA
                Text(
                    text = note.title.ifBlank { "Sem Título" },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("note_card_title_${note.id}")
                )

                // RODAPÉ: DATA E MENU DE AÇÕES RÁPIDAS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(24.dp).testTag("note_menu_btn_${note.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Opções da nota",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Visualizar") },
                                leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Editar") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Exportar PDF") },
                                leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onExportPdf()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Excluir", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
