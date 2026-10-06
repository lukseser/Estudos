package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.QuestionEntity
import com.example.data.local.entity.SubjectEntity
import java.io.InputStream

@Composable
fun QuestionsScreen(
    questions: List<QuestionEntity>,
    subjects: List<SubjectEntity>,
    selectedSubjectFilter: Long?,
    revealedQuestionIds: Set<Long>,
    onFilterSubject: (Long?) -> Unit,
    onToggleReveal: (Long) -> Unit,
    onAddQuestion: (subjectId: Long, subjectName: String, prompt: String, optA: String, optB: String, optC: String, optD: String, correct: String, exp: String) -> Unit,
    onDeleteQuestion: (QuestionEntity) -> Unit,
    onDownloadTemplate: (Context) -> Unit,
    onImportCsv: (InputStream) -> Unit
) {
    val context = LocalContext.current
    var showCreateForm by remember { mutableStateOf(false) }

    // Campos do formulário de criação
    var prompt by remember { mutableStateOf("") }
    var optA by remember { mutableStateOf("") }
    var optB by remember { mutableStateOf("") }
    var optC by remember { mutableStateOf("") }
    var optD by remember { mutableStateOf("") }
    var correctAnswer by remember { mutableStateOf("A") }
    var selectedSubjectId by remember { mutableStateOf(subjects.firstOrNull()?.id ?: 0L) }
    var explanation by remember { mutableStateOf("") }

    // Launcher para importar arquivo CSV
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

    // Filtragem de questões
    val filteredQuestions = if (selectedSubjectFilter == null) {
        questions
    } else {
        val targetSubject = subjects.find { it.id == selectedSubjectFilter }
        questions.filter { it.subjectId == selectedSubjectFilter || (targetSubject != null && it.subjectName.equals(targetSubject.name, ignoreCase = true)) }
    }

    // Agrupamento por disciplina
    val groupedQuestions = filteredQuestions.groupBy { it.subjectName.ifBlank { "Geral" } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("questions_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // CABEÇALHO COM CONTROLES CSV E BOTÃO ADICIONAR
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Banco de Questões",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${filteredQuestions.size} questões cadastradas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { showCreateForm = !showCreateForm }
                            .testTag("btn_toggle_create_question")
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
                                text = if (showCreateForm) "Fechar" else "Nova Questão",
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
                            .testTag("btn_download_questions_csv")
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
                            .testTag("btn_import_questions_csv")
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

                // FILTROS POR DISCIPLINA (Horizontal Chips)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedSubjectFilter == null,
                            onClick = { onFilterSubject(null) },
                            label = { Text("Todas (${questions.size})") },
                            modifier = Modifier.testTag("filter_all_questions")
                        )
                    }
                    items(subjects) { subject ->
                        val count = questions.count { it.subjectId == subject.id || it.subjectName.equals(subject.name, ignoreCase = true) }
                        FilterChip(
                            selected = selectedSubjectFilter == subject.id,
                            onClick = { onFilterSubject(subject.id) },
                            label = { Text("${subject.name} ($count)") },
                            modifier = Modifier.testTag("filter_question_sub_${subject.id}")
                        )
                    }
                }
            }
        }

        // FORMULÁRIO DE CRIAÇÃO (CARD RETRÁTIL)
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
                            text = "Cadastrar Questão com Gabarito",
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
                                    .testTag("question_form_subject_select")
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

                        // Enunciado
                        OutlinedTextField(
                            value = prompt,
                            onValueChange = { prompt = it },
                            label = { Text("Enunciado da Questão") },
                            modifier = Modifier.fillMaxWidth().testTag("q_input_prompt"),
                            minLines = 2,
                            shape = RoundedCornerShape(8.dp)
                        )

                        // Alternativas A, B, C, D
                        OutlinedTextField(
                            value = optA,
                            onValueChange = { optA = it },
                            label = { Text("Alternativa A") },
                            modifier = Modifier.fillMaxWidth().testTag("q_input_opt_a"),
                            shape = RoundedCornerShape(8.dp)
                        )

                        OutlinedTextField(
                            value = optB,
                            onValueChange = { optB = it },
                            label = { Text("Alternativa B") },
                            modifier = Modifier.fillMaxWidth().testTag("q_input_opt_b"),
                            shape = RoundedCornerShape(8.dp)
                        )

                        OutlinedTextField(
                            value = optC,
                            onValueChange = { optC = it },
                            label = { Text("Alternativa C") },
                            modifier = Modifier.fillMaxWidth().testTag("q_input_opt_c"),
                            shape = RoundedCornerShape(8.dp)
                        )

                        OutlinedTextField(
                            value = optD,
                            onValueChange = { optD = it },
                            label = { Text("Alternativa D") },
                            modifier = Modifier.fillMaxWidth().testTag("q_input_opt_d"),
                            shape = RoundedCornerShape(8.dp)
                        )

                        // Seletor de Gabarito (A, B, C, D)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Gabarito Correto:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.width(10.dp))
                            listOf("A", "B", "C", "D").forEach { letter ->
                                Surface(
                                    shape = CircleShape,
                                    color = if (correctAnswer == letter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .size(36.dp)
                                        .clickable { correctAnswer = letter }
                                        .testTag("gabarito_select_$letter")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = letter,
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = if (correctAnswer == letter) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        // Explicação / Justificativa
                        OutlinedTextField(
                            value = explanation,
                            onValueChange = { explanation = it },
                            label = { Text("Comentário / Explicação da Resposta (Opcional)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            shape = RoundedCornerShape(8.dp)
                        )

                        // Botão Salvar
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = prompt.isNotBlank() && optA.isNotBlank() && optB.isNotBlank()) {
                                    val subName = subjects.find { it.id == selectedSubjectId }?.name ?: "Geral"
                                    onAddQuestion(selectedSubjectId, subName, prompt, optA, optB, optC, optD, correctAnswer, explanation)
                                    prompt = ""
                                    optA = ""
                                    optB = ""
                                    optC = ""
                                    optD = ""
                                    explanation = ""
                                    showCreateForm = false
                                }
                                .testTag("btn_save_new_question")
                        ) {
                            Text(
                                text = "Salvar Questão no Banco",
                                modifier = Modifier.padding(vertical = 12.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }

        // LISTA DE QUESTÕES (MODO OCULTO / FLASHCARD STYLE)
        if (groupedQuestions.isEmpty()) {
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
                        Icon(Icons.Default.HelpOutline, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Nenhuma questão encontrada", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Adicione uma questão manualmente ou importe um arquivo CSV.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        } else {
            groupedQuestions.forEach { (subjectTitle, questionsInSub) ->
                item {
                    Text(
                        text = "📚 $subjectTitle",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    )
                }

                items(questionsInSub, key = { it.id }) { question ->
                    val isRevealed = revealedQuestionIds.contains(question.id)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("question_card_${question.id}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Enunciado
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = question.prompt,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { onDeleteQuestion(question) },
                                    modifier = Modifier.size(28.dp).testTag("delete_question_${question.id}")
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Excluir questão",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Alternativas
                            OptionRow(letter = "A", text = question.optionA, isCorrect = question.correctAnswer == "A", isRevealed = isRevealed)
                            OptionRow(letter = "B", text = question.optionB, isCorrect = question.correctAnswer == "B", isRevealed = isRevealed)
                            if (question.optionC.isNotBlank()) {
                                OptionRow(letter = "C", text = question.optionC, isCorrect = question.correctAnswer == "C", isRevealed = isRevealed)
                            }
                            if (question.optionD.isNotBlank()) {
                                OptionRow(letter = "D", text = question.optionD, isCorrect = question.correctAnswer == "D", isRevealed = isRevealed)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // BOTÃO "VER RESPOSTA" (MODO OCULTO)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isRevealed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleReveal(question.id) }
                                    .testTag("reveal_answer_btn_${question.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = if (isRevealed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isRevealed) "Ocultar Resposta" else "Ver Resposta (Gabarito)",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isRevealed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // EXPLICAÇÃO REVELADA
                            AnimatedVisibility(visible = isRevealed) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF0FDF4))
                                        .padding(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Gabarito: Alternativa ${question.correctAnswer}",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF15803D)
                                        )
                                    }
                                    if (question.explanation.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = question.explanation,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF166534)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionRow(letter: String, text: String, isCorrect: Boolean, isRevealed: Boolean) {
    val highlightCorrect = isRevealed && isCorrect

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (highlightCorrect) Color(0xFFDCFCE7) else Color.Transparent)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = if (highlightCorrect) Color(0xFF16A34A) else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = letter,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (highlightCorrect) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (highlightCorrect) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (highlightCorrect) Color(0xFF166534) else MaterialTheme.colorScheme.onSurface
        )
    }
}
