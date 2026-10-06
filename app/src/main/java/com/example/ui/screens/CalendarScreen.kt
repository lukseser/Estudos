package com.example.ui.screens

import android.content.res.Configuration
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CalendarEventEntity
import com.example.data.local.entity.TaskEntity
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(
    currentYearMonth: YearMonth,
    selectedDate: String,
    todayString: String,
    tasks: List<TaskEntity>,
    events: List<CalendarEventEntity>,
    onSelectDate: (String) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToggleTask: (TaskEntity) -> Unit,
    onDeleteEvent: (CalendarEventEntity) -> Unit,
    onAddEvent: (title: String, eventDate: String, time: String?, description: String?) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var newEventTitle by remember { mutableStateOf("") }
    var newEventTime by remember { mutableStateOf("") }
    var newEventDesc by remember { mutableStateOf("") }
    var showAddEventForm by remember { mutableStateOf(false) }

    // Itens da data selecionada
    val tasksOnSelectedDate = tasks.filter { it.dueDate == selectedDate }
    val eventsOnSelectedDate = events.filter { it.eventDate == selectedDate }

    val locale = Locale("pt", "BR")
    val monthTitle = currentYearMonth.month.getDisplayName(TextStyle.FULL, locale)
        .replaceFirstChar { it.uppercase() } + " de ${currentYearMonth.year}"

    val parsedSelectedDate = try {
        LocalDate.parse(selectedDate)
    } catch (e: Exception) {
        LocalDate.now()
    }
    val formattedSelectedDate = "${parsedSelectedDate.dayOfMonth} de ${
        parsedSelectedDate.month.getDisplayName(TextStyle.FULL, locale)
    }"
    val isSelectedToday = selectedDate == todayString

    if (isLandscape) {
        // LAYOUT HORIZONTAL OTIMIZADO:
        // Lado Esquerdo: Grade do Mês
        // Lado Direito: Painel de Detalhes do Dia
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("calendar_screen_landscape"),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Lado Esquerdo: Calendário
            Card(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    MonthHeader(
                        monthTitle = monthTitle,
                        onPrev = onPrevMonth,
                        onNext = onNextMonth
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    WeekdaysRow()
                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(4.dp))

                    CalendarGrid(
                        yearMonth = currentYearMonth,
                        selectedDate = selectedDate,
                        todayString = todayString,
                        tasks = tasks,
                        events = events,
                        onSelectDate = onSelectDate
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendItem(color = Color(0xFFF97316), label = "Pendentes")
                        LegendItem(color = Color(0xFF94A3B8), label = "Concluídas")
                        LegendItem(color = Color(0xFF2563EB), label = "Eventos")
                    }
                }
            }

            // Lado Direito: Detalhes do Dia Selecionado
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .testTag("day_details_panel_landscape"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    DayDetailsContent(
                        formattedSelectedDate = formattedSelectedDate,
                        isSelectedToday = isSelectedToday,
                        tasksOnSelectedDate = tasksOnSelectedDate,
                        eventsOnSelectedDate = eventsOnSelectedDate,
                        selectedDate = selectedDate,
                        showAddEventForm = showAddEventForm,
                        newEventTitle = newEventTitle,
                        newEventTime = newEventTime,
                        newEventDesc = newEventDesc,
                        onTitleChange = { newEventTitle = it },
                        onTimeChange = { newEventTime = it },
                        onDescChange = { newEventDesc = it },
                        onShowAddForm = { showAddEventForm = true },
                        onCancelAddForm = { showAddEventForm = false },
                        onToggleTask = onToggleTask,
                        onDeleteEvent = onDeleteEvent,
                        onAddEvent = {
                            onAddEvent(newEventTitle, selectedDate, newEventTime, newEventDesc)
                            newEventTitle = ""
                            newEventTime = ""
                            newEventDesc = ""
                            showAddEventForm = false
                        }
                    )
                }
            }
        }
    } else {
        // LAYOUT VERTICAL PADRÃO
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("calendar_screen"),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        MonthHeader(
                            monthTitle = monthTitle,
                            onPrev = onPrevMonth,
                            onNext = onNextMonth
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        WeekdaysRow()
                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(6.dp))

                        CalendarGrid(
                            yearMonth = currentYearMonth,
                            selectedDate = selectedDate,
                            todayString = todayString,
                            tasks = tasks,
                            events = events,
                            onSelectDate = onSelectDate
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LegendItem(color = Color(0xFFF97316), label = "Tarefas Pendentes")
                            LegendItem(color = Color(0xFF94A3B8), label = "Tarefas Concluídas")
                            LegendItem(color = Color(0xFF2563EB), label = "Eventos")
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("day_details_panel"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        DayDetailsContent(
                            formattedSelectedDate = formattedSelectedDate,
                            isSelectedToday = isSelectedToday,
                            tasksOnSelectedDate = tasksOnSelectedDate,
                            eventsOnSelectedDate = eventsOnSelectedDate,
                            selectedDate = selectedDate,
                            showAddEventForm = showAddEventForm,
                            newEventTitle = newEventTitle,
                            newEventTime = newEventTime,
                            newEventDesc = newEventDesc,
                            onTitleChange = { newEventTitle = it },
                            onTimeChange = { newEventTime = it },
                            onDescChange = { newEventDesc = it },
                            onShowAddForm = { showAddEventForm = true },
                            onCancelAddForm = { showAddEventForm = false },
                            onToggleTask = onToggleTask,
                            onDeleteEvent = onDeleteEvent,
                            onAddEvent = {
                                onAddEvent(newEventTitle, selectedDate, newEventTime, newEventDesc)
                                newEventTitle = ""
                                newEventTime = ""
                                newEventDesc = ""
                                showAddEventForm = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(
    monthTitle: String,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = onPrev,
            modifier = Modifier.testTag("cal_prev_month_btn")
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Mês anterior")
        }

        Text(
            text = monthTitle,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        IconButton(
            onClick = onNext,
            modifier = Modifier.testTag("cal_next_month_btn")
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Próximo mês")
        }
    }
}

@Composable
private fun WeekdaysRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb").forEach { dayLabel ->
            Text(
                text = dayLabel,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun DayDetailsContent(
    formattedSelectedDate: String,
    isSelectedToday: Boolean,
    tasksOnSelectedDate: List<TaskEntity>,
    eventsOnSelectedDate: List<CalendarEventEntity>,
    selectedDate: String,
    showAddEventForm: Boolean,
    newEventTitle: String,
    newEventTime: String,
    newEventDesc: String,
    onTitleChange: (String) -> Unit,
    onTimeChange: (String) -> Unit,
    onDescChange: (String) -> Unit,
    onShowAddForm: () -> Unit,
    onCancelAddForm: () -> Unit,
    onToggleTask: (TaskEntity) -> Unit,
    onDeleteEvent: (CalendarEventEntity) -> Unit,
    onAddEvent: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formattedSelectedDate,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (isSelectedToday) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "Hoje",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Tarefas com prazo neste dia (${tasksOnSelectedDate.size})",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )

        if (tasksOnSelectedDate.isEmpty()) {
            Text(
                text = "Nenhuma tarefa para este dia.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tasksOnSelectedDate.forEach { task ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = task.isCompleted,
                                onCheckedChange = { onToggleTask(task) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.testTag("cal_task_toggle_${task.id}")
                            )
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                ),
                                color = if (task.isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Eventos e Provas (${eventsOnSelectedDate.size})",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF2563EB)
        )

        if (eventsOnSelectedDate.isEmpty()) {
            Text(
                text = "Nenhum evento agendado para este dia.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                eventsOnSelectedDate.forEach { event ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEFF6FF),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = event.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color(0xFF1E3A8A)
                                )
                                if (!event.time.isNullOrBlank() || !event.description.isNullOrBlank()) {
                                    val details = listOfNotNull(event.time, event.description).joinToString(" • ")
                                    Text(
                                        text = details,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF3B82F6)
                                    )
                                }
                            }
                            IconButton(
                                onClick = { onDeleteEvent(event) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Excluir evento",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(10.dp))

        if (!showAddEventForm) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onShowAddForm() }
                    .testTag("btn_show_add_event")
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Adicionar Evento neste dia",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Novo Evento para $selectedDate",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = newEventTitle,
                    onValueChange = onTitleChange,
                    label = { Text("Título (Ex: Prova de Anatomia)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("event_title_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newEventTime,
                        onValueChange = onTimeChange,
                        label = { Text("Horário (Ex: 14:00)") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("event_time_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = newEventDesc,
                        onValueChange = onDescChange,
                        label = { Text("Local / Detalhes") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .clickable { onCancelAddForm() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("Cancelar", style = MaterialTheme.typography.labelMedium)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable(enabled = newEventTitle.isNotBlank()) { onAddEvent() }
                            .testTag("event_submit_btn")
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Salvar Evento",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    yearMonth: YearMonth,
    selectedDate: String,
    todayString: String,
    tasks: List<TaskEntity>,
    events: List<CalendarEventEntity>,
    onSelectDate: (String) -> Unit
) {
    val firstDayOfMonth = yearMonth.atDay(1)
    val dayOfWeekOffset = firstDayOfMonth.dayOfWeek.value % 7
    val lengthOfMonth = yearMonth.lengthOfMonth()

    val totalCells = ((dayOfWeekOffset + lengthOfMonth + 6) / 7) * 7
    val rows = totalCells / 7

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (r in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                for (c in 0..6) {
                    val cellIndex = r * 7 + c
                    val dayNum = cellIndex - dayOfWeekOffset + 1

                    if (dayNum in 1..lengthOfMonth) {
                        val dateStr = String.format("%04d-%02d-%02d", yearMonth.year, yearMonth.monthValue, dayNum)
                        val isSelected = dateStr == selectedDate
                        val isToday = dateStr == todayString

                        val dateTasks = tasks.filter { it.dueDate == dateStr }
                        val hasPendingTasks = dateTasks.any { !it.isCompleted }
                        val hasCompletedTasksOnly = dateTasks.isNotEmpty() && dateTasks.all { it.isCompleted }
                        val hasEvents = events.any { it.eventDate == dateStr }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                                        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        else -> Color.Transparent
                                    }
                                )
                                .border(
                                    width = if (isToday) 1.5.dp else if (isSelected) 1.dp else 0.dp,
                                    color = if (isToday) MaterialTheme.colorScheme.primary else if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onSelectDate(dateStr) }
                                .testTag("cal_day_$dayNum"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = dayNum.toString(),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    ),
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        isToday -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (hasPendingTasks) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFF97316))
                                        )
                                    } else if (hasCompletedTasksOnly) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF94A3B8))
                                        )
                                    }

                                    if (hasEvents) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF2563EB))
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.outline
        )
    }
}
