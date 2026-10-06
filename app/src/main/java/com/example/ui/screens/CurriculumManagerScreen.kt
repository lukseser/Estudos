package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ModuleEntity
import com.example.data.local.entity.SemesterEntity
import com.example.data.local.entity.SubjectEntity

@Composable
fun CurriculumManagerScreen(
    semesters: List<SemesterEntity>,
    modules: List<ModuleEntity>,
    subjects: List<SubjectEntity>,
    selectedSemesterId: Long?,
    onSelectSemester: (Long) -> Unit,
    onAddSemester: (String) -> Unit,
    onRenameSemester: (SemesterEntity, String) -> Unit,
    onDeleteSemester: (SemesterEntity) -> Unit,
    onAddModule: (semesterId: Long, name: String) -> Unit,
    onDeleteModule: (ModuleEntity) -> Unit,
    onAddSubject: (moduleId: Long, name: String) -> Unit,
    onDeleteSubject: (SubjectEntity) -> Unit
) {
    val activeSemester = semesters.find { it.id == selectedSemesterId }
        ?: semesters.firstOrNull()

    val currentModules = modules.filter { it.semesterId == activeSemester?.id }

    // Diálogos de controle de semestre
    var showAddSemesterDialog by remember { mutableStateOf(false) }
    var showRenameSemesterDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    var newSemesterName by remember { mutableStateOf("") }
    var renameSemesterName by remember { mutableStateOf("") }

    // Input para novo módulo
    var newModuleName by remember { mutableStateOf("") }

    // Diálogo Adicionar Semestre
    if (showAddSemesterDialog) {
        AlertDialog(
            onDismissRequest = { showAddSemesterDialog = false },
            title = { Text("Novo Semestre", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newSemesterName,
                    onValueChange = { newSemesterName = it },
                    label = { Text("Nome do Semestre (Ex: 3º Semestre)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newSemesterName.isNotBlank()) {
                            onAddSemester(newSemesterName)
                            newSemesterName = ""
                            showAddSemesterDialog = false
                        }
                    }
                ) { Text("Adicionar") }
            },
            dismissButton = {
                TextButton(onClick = { showAddSemesterDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Diálogo Renomear Semestre
    if (showRenameSemesterDialog && activeSemester != null) {
        AlertDialog(
            onDismissRequest = { showRenameSemesterDialog = false },
            title = { Text("Renomear Semestre", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameSemesterName,
                    onValueChange = { renameSemesterName = it },
                    label = { Text("Novo nome") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameSemesterName.isNotBlank()) {
                            onRenameSemester(activeSemester, renameSemesterName)
                            showRenameSemesterDialog = false
                        }
                    }
                ) { Text("Salvar") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameSemesterDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Diálogo de Confirmação de Exclusão de Semestre (Cascata)
    if (showDeleteConfirmDialog && activeSemester != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Excluir Semestre?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Atenção: Ao excluir o semestre '${activeSemester.name}', todos os módulos e disciplinas cadastrados dentro dele serão excluídos em cascata permanentemente."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteSemester(activeSemester)
                        showDeleteConfirmDialog = false
                    }
                ) {
                    Text("Excluir Definitivamente", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancelar") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("curriculum_manager_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // CABEÇALHO DO GESTOR CURRICULAR COM SEMESTRES
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Estrutura Curricular",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Botão Adicionar Novo Semestre
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable {
                                    newSemesterName = ""
                                    showAddSemesterDialog = true
                                }
                                .testTag("btn_add_semester")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Semestre +",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Seletor do Semestre Atual + Ações (Renomear / Excluir)
                    if (activeSemester != null) {
                        var semDropdownExpanded by remember { mutableStateOf(false) }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { semDropdownExpanded = true }
                                    ) {
                                        Text(
                                            text = activeSemester.name,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("(Trocar)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                    }

                                    DropdownMenu(
                                        expanded = semDropdownExpanded,
                                        onDismissRequest = { semDropdownExpanded = false }
                                    ) {
                                        semesters.forEach { sem ->
                                            DropdownMenuItem(
                                                text = { Text(sem.name) },
                                                onClick = {
                                                    onSelectSemester(sem.id)
                                                    semDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // Renomear semestre
                                    IconButton(
                                        onClick = {
                                            renameSemesterName = activeSemester.name
                                            showRenameSemesterDialog = true
                                        },
                                        modifier = Modifier.size(32.dp).testTag("btn_rename_semester")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Renomear", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                    }

                                    // Excluir semestre (com bloqueio e alerta em cascata)
                                    IconButton(
                                        onClick = { showDeleteConfirmDialog = true },
                                        modifier = Modifier.size(32.dp).testTag("btn_delete_semester")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // INPUT PARA CRIAR "CAIXAS" DE MÓDULOS
        if (activeSemester != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newModuleName,
                            onValueChange = { newModuleName = it },
                            placeholder = { Text("Nome do novo Módulo (Ex: Sistema Nervoso)") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_new_module"),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable(enabled = newModuleName.isNotBlank()) {
                                    onAddModule(activeSemester.id, newModuleName)
                                    newModuleName = ""
                                }
                                .testTag("btn_save_module")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Criar Módulo", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }
            }
        }

        // LISTA DE "CAIXAS" DE MÓDULOS E SUAS DISCIPLINAS
        if (currentModules.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhum módulo cadastrado neste semestre.\nCrie um módulo acima para começar!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            items(currentModules, key = { it.id }) { module ->
                val moduleSubjects = subjects.filter { it.moduleId == module.id }

                ModuleBoxItem(
                    module = module,
                    subjects = moduleSubjects,
                    onDeleteModule = { onDeleteModule(module) },
                    onAddSubject = { name -> onAddSubject(module.id, name) },
                    onDeleteSubject = onDeleteSubject
                )
            }
        }
    }
}

@Composable
private fun ModuleBoxItem(
    module: ModuleEntity,
    subjects: List<SubjectEntity>,
    onDeleteModule: () -> Unit,
    onAddSubject: (String) -> Unit,
    onDeleteSubject: (SubjectEntity) -> Unit
) {
    var newSubjectName by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("module_box_${module.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Cabeçalho da Caixa de Módulo
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = module.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onDeleteModule,
                    modifier = Modifier.size(30.dp).testTag("delete_module_${module.id}")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Excluir Módulo", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mini-input lateral para criar disciplinas específicas
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newSubjectName,
                    onValueChange = { newSubjectName = it },
                    placeholder = { Text("Nova disciplina (Ex: Neuroanatomia)") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_subject_for_module_${module.id}"),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .clickable(enabled = newSubjectName.isNotBlank()) {
                            onAddSubject(newSubjectName)
                            newSubjectName = ""
                        }
                        .testTag("btn_add_subject_${module.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "Adicionar",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Lista de Disciplinas dentro deste Módulo
            if (subjects.isEmpty()) {
                Text(
                    text = "Nenhuma disciplina neste módulo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    subjects.forEach { subject ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = subject.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                IconButton(
                                    onClick = { onDeleteSubject(subject) },
                                    modifier = Modifier.size(24.dp).testTag("delete_subject_${subject.id}")
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Excluir disciplina",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                        modifier = Modifier.size(14.dp)
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
