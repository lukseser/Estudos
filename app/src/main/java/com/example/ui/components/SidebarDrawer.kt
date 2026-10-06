package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ModuleEntity
import com.example.data.local.entity.SemesterEntity
import com.example.data.local.entity.SubjectEntity
import com.example.ui.viewmodel.AppScreen

import com.example.ui.theme.ThemeMode
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7

@Composable
fun SidebarDrawer(
    currentScreen: AppScreen,
    semesters: List<SemesterEntity>,
    modules: List<ModuleEntity>,
    subjects: List<SubjectEntity>,
    selectedSemesterId: Long?,
    expandedModuleIds: Set<Long>,
    themeMode: ThemeMode,
    onToggleTheme: () -> Unit,
    onSelectScreen: (AppScreen) -> Unit,
    onSelectSemester: (Long) -> Unit,
    onToggleModule: (Long) -> Unit,
    onOpenSubject: (Long, String) -> Unit,
    onCloseDrawer: () -> Unit
) {
    var semesterDropdownExpanded by remember { mutableStateOf(false) }

    // Determina o semestre ativo (o selecionado ou o primeiro)
    val activeSemester = semesters.find { it.id == selectedSemesterId }
        ?: semesters.firstOrNull()

    // Filtra módulos pertencentes ao semestre ativo
    val currentModules = modules.filter { it.semesterId == activeSemester?.id }

    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(320.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Cabeçalho da Sidebar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = "Ícone do Planner",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Estudos do Sergio",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Medicina UFPA",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Links de Acesso Rápido Principais
            Text(
                text = "FERRAMENTAS",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            DrawerNavButton(
                icon = Icons.Default.Dashboard,
                label = "Dashboard",
                isSelected = currentScreen is AppScreen.Dashboard,
                testTag = "nav_dashboard_button",
                onClick = {
                    onSelectScreen(AppScreen.Dashboard)
                    onCloseDrawer()
                }
            )

            DrawerNavButton(
                icon = Icons.Default.CalendarMonth,
                label = "Calendário & Planner",
                isSelected = currentScreen is AppScreen.Calendar,
                testTag = "nav_calendar_button",
                onClick = {
                    onSelectScreen(AppScreen.Calendar)
                    onCloseDrawer()
                }
            )

            DrawerNavButton(
                icon = Icons.Default.HelpOutline,
                label = "Banco de Questões",
                isSelected = currentScreen is AppScreen.Questions,
                testTag = "nav_questions_button",
                onClick = {
                    onSelectScreen(AppScreen.Questions)
                    onCloseDrawer()
                }
            )

            DrawerNavButton(
                icon = Icons.Default.Style,
                label = "Flashcards (Anki)",
                isSelected = currentScreen is AppScreen.Flashcards,
                testTag = "nav_flashcards_button",
                onClick = {
                    onSelectScreen(AppScreen.Flashcards)
                    onCloseDrawer()
                }
            )

            DrawerNavButton(
                icon = Icons.Default.School,
                label = "Gestão Curricular",
                isSelected = currentScreen is AppScreen.Curriculum,
                testTag = "nav_curriculum_button",
                onClick = {
                    onSelectScreen(AppScreen.Curriculum)
                    onCloseDrawer()
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Seletor de Semestre (Menu Acordeão / Dropdown)
            Text(
                text = "CURRÍCULO & DISCIPLINAS",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { semesterDropdownExpanded = true }
                        .testTag("semester_dropdown_selector")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = activeSemester?.name ?: "Selecione o Semestre",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = "Expandir Semestres",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                DropdownMenu(
                    expanded = semesterDropdownExpanded,
                    onDismissRequest = { semesterDropdownExpanded = false }
                ) {
                    semesters.forEach { semester ->
                        DropdownMenuItem(
                            text = { Text(semester.name) },
                            onClick = {
                                onSelectSemester(semester.id)
                                semesterDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Lista de Módulos (Menu Acordeão)
            if (currentModules.isEmpty()) {
                Text(
                    text = "Nenhum módulo cadastrado neste semestre.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                currentModules.forEach { module ->
                    val isExpanded = expandedModuleIds.contains(module.id)
                    val moduleSubjects = subjects.filter { it.moduleId == module.id }

                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        // Linha do Módulo (Clicável para expandir)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isExpanded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleModule(module.id) }
                                .testTag("module_item_${module.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = module.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isExpanded) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Disciplinas Aninhadas (Expandidas)
                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 24.dp, top = 2.dp, bottom = 4.dp)
                            ) {
                                if (moduleSubjects.isEmpty()) {
                                    Text(
                                        text = "Nenhuma disciplina neste módulo.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                } else {
                                    moduleSubjects.forEach { subject ->
                                        val isCurrentSubject = (currentScreen is AppScreen.SubjectEditor && currentScreen.subjectId == subject.id) ||
                                                (currentScreen is AppScreen.SubjectLibrary && currentScreen.subjectId == subject.id)

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isCurrentSubject) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    onOpenSubject(subject.id, subject.name)
                                                    onCloseDrawer()
                                                }
                                                .testTag("subject_nav_${subject.id}")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                                    contentDescription = null,
                                                    tint = if (isCurrentSubject) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = subject.name,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = if (isCurrentSubject) FontWeight.Bold else FontWeight.Normal
                                                    ),
                                                    color = if (isCurrentSubject) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Seletor de Tema (Claro / Escuro)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleTheme() }
                    .testTag("drawer_theme_toggle_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (themeMode == ThemeMode.LIGHT) Icons.Default.Brightness7 else Icons.Default.Brightness4,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (themeMode == ThemeMode.LIGHT) "Tema Claro Ativo" else "Tema Escuro Ativo",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "Trocar",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DrawerNavButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        icon = { Icon(imageVector = icon, contentDescription = label) },
        label = { Text(text = label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
        selected = isSelected,
        onClick = onClick,
        modifier = Modifier
            .padding(vertical = 2.dp)
            .testTag(testTag),
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}
