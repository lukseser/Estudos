package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.FlashcardEntity
import com.example.data.local.entity.SubjectEntity
import java.io.InputStream

@Composable
fun FlashcardsScreen(
    flashcards: List<FlashcardEntity>,
    subjects: List<SubjectEntity>,
    selectedSubjectFilter: Long?,
    flippedCardIds: Set<Long>,
    onFilterSubject: (Long?) -> Unit,
    onToggleFlip: (Long) -> Unit,
    onAddFlashcard: (subjectId: Long, subjectName: String, front: String, back: String) -> Unit,
    onDeleteFlashcard: (FlashcardEntity) -> Unit,
    onDownloadTemplate: (Context) -> Unit,
    onImportCsv: (InputStream) -> Unit
) {
    val context = LocalContext.current
    var showCreateForm by remember { mutableStateOf(false) }

    var frontText by remember { mutableStateOf("") }
    var backText by remember { mutableStateOf("") }
    var selectedSubjectId by remember { mutableStateOf(subjects.firstOrNull()?.id ?: 0L) }

    val csvPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.let { stream ->
                    onImportCsv(stream)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val filteredFlashcards = if (selectedSubjectFilter == null) {
        flashcards
    } else {
        val targetSubject = subjects.find { it.id == selectedSubjectFilter }
        flashcards.filter { it.subjectId == selectedSubjectFilter || (targetSubject != null && it.subjectName.equals(targetSubject.name, ignoreCase = true)) }
    }

    val groupedFlashcards = filteredFlashcards.groupBy { it.subjectName.ifBlank { "Geral" } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("flashcards_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // CABEÇALHO E CONTROLES CSV
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Flashcards (Anki)",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Repetição espaçada com giro 3D",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { showCreateForm = !showCreateForm }
                            .testTag("btn_toggle_create_flashcard")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (showCreateForm) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (showCreateForm) "Fechar" else "Novo Card",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // BOTÕES CSV: Baixar Modelo & Importar CSV
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onDownloadTemplate(context) }
                            .testTag("btn_download_flashcards_csv")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Baixar Modelo CSV", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold))
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { csvPickerLauncher.launch("*/*") }
                            .testTag("btn_import_flashcards_csv")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Importar CSV", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // FILTROS POR DISCIPLINA
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedSubjectFilter == null,
                            onClick = { onFilterSubject(null) },
                            label = { Text("Todos (${flashcards.size})") },
                            modifier = Modifier.testTag("filter_all_flashcards")
                        )
                    }
                    items(subjects) { subject ->
                        val count = flashcards.count { it.subjectId == subject.id || it.subjectName.equals(subject.name, ignoreCase = true) }
                        FilterChip(
                            selected = selectedSubjectFilter == subject.id,
                            onClick = { onFilterSubject(subject.id) },
                            label = { Text("${subject.name} ($count)") },
                            modifier = Modifier.testTag("filter_flashcard_sub_${subject.id}")
                        )
                    }
                }
            }
        }

        // FORMULÁRIO DE CRIAÇÃO SIMPLES (FRENTE / VERSO)
        if (showCreateForm) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Criar Novo Flashcard",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Seleção de Disciplina
                        var subDropdownExpanded by remember { mutableStateOf(false) }
                        val activeSubName = subjects.find { it.id == selectedSubjectId }?.name ?: "Selecione a Disciplina"

                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { subDropdownExpanded = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Disciplina: $activeSubName", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                    Icon(Icons.Default.FilterList, contentDescription = null)
                                }
                            }

                            DropdownMenu(
                                expanded = subDropdownExpanded,
                                onDismissRequest = { subDropdownExpanded = false }
                            ) {
                                subjects.forEach { sub ->
                                    DropdownMenuItem(
                                        text = { Text(sub.name) },
                                        onClick = {
                                            selectedSubjectId = sub.id
                                            subDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Frente (Pergunta/Conceito)
                        OutlinedTextField(
                            value = frontText,
                            onValueChange = { frontText = it },
                            label = { Text("Frente (Pergunta ou Conceito)") },
                            modifier = Modifier.fillMaxWidth().testTag("fc_input_front"),
                            minLines = 2,
                            shape = RoundedCornerShape(8.dp)
                        )

                        // Verso (Resposta/Definição)
                        OutlinedTextField(
                            value = backText,
                            onValueChange = { backText = it },
                            label = { Text("Verso (Resposta ou Definição)") },
                            modifier = Modifier.fillMaxWidth().testTag("fc_input_back"),
                            minLines = 3,
                            shape = RoundedCornerShape(8.dp)
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = frontText.isNotBlank() && backText.isNotBlank()) {
                                    val subName = subjects.find { it.id == selectedSubjectId }?.name ?: "Geral"
                                    onAddFlashcard(selectedSubjectId, subName, frontText, backText)
                                    frontText = ""
                                    backText = ""
                                    showCreateForm = false
                                }
                                .testTag("btn_save_flashcard")
                        ) {
                            Text(
                                text = "Salvar Flashcard",
                                modifier = Modifier.padding(vertical = 12.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }

        // CARDS COM GIRO 3D (ANIMAÇÃO EM EIXO Y)
        if (groupedFlashcards.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Style, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Nenhum flashcard disponível", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Adicione cards ou importe pelo modelo CSV.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        } else {
            groupedFlashcards.forEach { (subjectTitle, cardsInSub) ->
                item {
                    Text(
                        text = "🗂 $subjectTitle",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    )
                }

                items(cardsInSub, key = { it.id }) { card ->
                    val isFlipped = flippedCardIds.contains(card.id)

                    FlipFlashcardItem(
                        flashcard = card,
                        isFlipped = isFlipped,
                        onFlip = { onToggleFlip(card.id) },
                        onDelete = { onDeleteFlashcard(card) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FlipFlashcardItem(
    flashcard: FlashcardEntity,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    onDelete: () -> Unit
) {
    // ENGENHARIA VISUAL 3D: Animação de rotação em 180 graus com perspectiva
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "flashcard_flip_animation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 170.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 14f * density // Perspectiva 3D
            }
            .clickable { onFlip() }
            .testTag("flashcard_item_${flashcard.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (rotation <= 90f) MaterialTheme.colorScheme.surface else Color(0xFFF0FDF4)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (rotation <= 90f) MaterialTheme.colorScheme.outlineVariant else Color(0xFF86EFAC)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Botão de Exclusão no canto superior direito (com parada de propagação)
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(30.dp)
                    .testTag("delete_flashcard_${flashcard.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Excluir flashcard",
                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }

            if (rotation <= 90f) {
                // FRENTE (PERGUNTA / CONCEITO)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 32.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "FRENTE",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = flashcard.subjectName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = flashcard.front,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Toque no card para girar e ver a resposta",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                // VERSO (RESPOSTA / DEFINIÇÃO) - Inverter rotação em 180f para não espelhar o texto
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationY = 180f }
                        .padding(end = 32.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFDCFCE7)
                        ) {
                            Text(
                                text = "VERSO (RESPOSTA)",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF15803D)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = flashcard.back,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Normal,
                            fontSize = 16.sp
                        ),
                        color = Color(0xFF14532D)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color(0xFF15803D),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Toque para voltar à pergunta",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF15803D)
                        )
                    }
                }
            }
        }
    }
}
