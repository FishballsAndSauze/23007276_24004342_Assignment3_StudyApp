package com.example.a23007276_24004342_assignment3_studyapp

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth

@Composable
internal fun HomeScreen(
    tasks: List<StudyTask>,
    progress: UserProgress,
    selectedDate: LocalDate,
    month: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onAddTask: () -> Unit,
    onProgressChange: (StudyTask, Int) -> Unit,
    onDeleteTask: (StudyTask) -> Unit
) {
    var filter by remember { mutableStateOf("All") }
    val filtered = when (filter) {
        "Active" -> tasks.filter { !it.completed }
        "Done" -> tasks.filter { it.completed }
        else -> tasks
    }
    val dueTasks = tasks.filter { it.dueDate == selectedDate.toString() }
    val animatedXp by animateFloatAsState(targetValue = progress.xp / 100f, label = "xp")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("YOUR PROGRESS", color = Cyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        if (progress.streak > 0) {
                            Text("🔥 ${progress.streak} day streak", color = Orange, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Level ${progress.level}", color = WhiteText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            Text("${progress.xp} / 100 XP to next level", color = Muted)
                        }
                        Box(
                            modifier = Modifier.size(62.dp).background(Purple.copy(alpha = 0.18f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) { Text("★", color = Purple, fontSize = 28.sp) }
                    }
                    LinearProgressIndicator(
                        progress = { animatedXp },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = Purple,
                        trackColor = Color(0xFF343B55)
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tasks completed", color = Muted)
                        Text("${progress.completedTasks}", color = WhiteText, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Your assignments", color = WhiteText, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text("${tasks.count { !it.completed }} tasks remaining", color = Muted, fontSize = 13.sp)
                }
                Button(onClick = onAddTask) { Text("+ Add task") }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "Active", "Done").forEach { option ->
                    FilterChip(
                        selected = filter == option,
                        onClick = { filter = option },
                        label = { Text(option) }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                    Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No assignments yet", color = WhiteText, fontWeight = FontWeight.Bold)
                        Text("Add your first task to start building your study streak.", color = Muted)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = onAddTask) { Text("Create a task") }
                    }
                }
            }
        } else {
            items(filtered, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    onMinus = { onProgressChange(task, task.progress - 20) },
                    onPlus = { onProgressChange(task, task.progress + 20) },
                    onDelete = { onDeleteTask(task) }
                )
            }
        }

        item {
            CalendarCard(
                month = month,
                selectedDate = selectedDate,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onSelectDate = onSelectDate,
                tasks = tasks
            )
        }

        item {
            Text("Tasks for $selectedDate", color = WhiteText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (dueTasks.isEmpty()) {
                Text("Nothing is due on this date.", color = Muted)
            } else {
                dueTasks.forEach { task ->
                    Text("• ${task.title}${if (task.completed) " (completed)" else ""}", color = WhiteText, modifier = Modifier.padding(vertical = 3.dp))
                }
            }
        }
    }
}

@Composable
private fun TaskCard(task: StudyTask, onMinus: () -> Unit, onPlus: () -> Unit, onDelete: () -> Unit) {
    val today = LocalDate.now().toString()
    val isOverdue = !task.completed && task.dueDate < today
    val isDueToday = !task.completed && task.dueDate == today
    val stripeColor = when (task.size.lowercase()) {
        "medium" -> Purple
        "large" -> Orange
        else -> Cyan
    }
    val animatedProgress by animateFloatAsState(targetValue = task.progress / 100f, label = "taskProgress")

    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
        Row {
            Box(Modifier.width(6.dp).fillMaxHeight().background(stripeColor))
            Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(task.title, color = WhiteText, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            if (isOverdue) {
                                Text("Overdue", color = Color(0xFFFF6B6B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            } else if (isDueToday) {
                                Text("Due today", color = Orange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (task.description.isNotBlank()) Text(task.description, color = Muted, fontSize = 13.sp)
                        Text("Due ${task.dueDate}  •  ${task.size}  •  ${task.sizeXp()} XP", color = Cyan, fontSize = 12.sp)
                    }
                    TextButton(onClick = onDelete) { Text("Delete", color = Color(0xFFFF9292)) }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${task.progress}%", color = WhiteText, fontWeight = FontWeight.Bold, modifier = Modifier.width(48.dp))
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.weight(1f).height(8.dp),
                        color = if (task.completed) Cyan else Purple,
                        trackColor = Color(0xFF343B55)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (task.completed) "Done" else "In progress", color = if (task.completed) Cyan else Muted, fontSize = 11.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = onMinus, enabled = task.progress > 0) { Text("−20%") }
                    Button(onClick = onPlus, enabled = task.progress < 100) { Text("+20%") }
                    Spacer(Modifier.weight(1f))
                    if (task.completed) Text("✓ Complete", color = Cyan, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun StudyTask.sizeXp(): Int = when (size.lowercase()) {
    "medium" -> 15
    "large" -> 25
    else -> 10
}