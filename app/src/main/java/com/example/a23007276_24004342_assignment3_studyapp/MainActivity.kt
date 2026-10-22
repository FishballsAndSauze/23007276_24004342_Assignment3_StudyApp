

package com.example.a23007276_24004342_assignment3_studyapp


import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException
import kotlin.math.max
import kotlin.math.min

private val Ink = Color(0xFF101426)
private val Panel = Color(0xFF1B2238)
private val Purple = Color(0xFF9275FF)
private val Cyan = Color(0xFF55D6E8)
private val SoftRed = Color(0xFFFFE1E1)
private val SoftGreen = Color(0xFFDDF7E6)
private val WhiteText = Color(0xFFF5F6FC)
private val Muted = Color(0xFFB7BED2)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { StudySprintApp(this) }
    }
}

data class StudyTask(
    val id: Long,
    val title: String,
    val description: String,
    val dueDate: String,
    val size: String,
    val progress: Int = 0,
    val completed: Boolean = false,
    val rewardClaimed: Boolean = false
)

data class StudyPhase(val name: String, val durationMs: Long, val isBreak: Boolean)

data class UserProgress(val level: Int = 1, val xp: Int = 0, val completedTasks: Int = 0)

private object StudyStore {
    private const val PREFS = "studysprint_store"
    private const val TASKS = "tasks"
    private const val LEVEL = "level"
    private const val XP = "xp"
    private const val COMPLETED = "completed"

    fun loadTasks(context: Context): List<StudyTask> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(TASKS, "[]") ?: "[]"
        val result = mutableListOf<StudyTask>()
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                result.add(
                    StudyTask(
                        id = o.optLong("id"),
                        title = o.optString("title"),
                        description = o.optString("description"),
                        dueDate = o.optString("dueDate"),
                        size = o.optString("size", "Small"),
                        progress = o.optInt("progress", 0),
                        completed = o.optBoolean("completed", false),
                        rewardClaimed = o.optBoolean("rewardClaimed", false)
                    )
                )
            }
        } catch (_: Exception) {
            // Start with an empty task list if saved data cannot be read.
        }
        return result
    }

    fun saveTasks(context: Context, tasks: List<StudyTask>) {
        val array = JSONArray()
        tasks.forEach { task ->
            array.put(
                JSONObject().apply {
                    put("id", task.id)
                    put("title", task.title)
                    put("description", task.description)
                    put("dueDate", task.dueDate)
                    put("size", task.size)
                    put("progress", task.progress)
                    put("completed", task.completed)
                    put("rewardClaimed", task.rewardClaimed)
                }
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(TASKS, array.toString()).apply()
    }

    fun loadProgress(context: Context): UserProgress {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return UserProgress(
            level = prefs.getInt(LEVEL, 1),
            xp = prefs.getInt(XP, 0),
            completedTasks = prefs.getInt(COMPLETED, 0)
        )
    }

    fun saveProgress(context: Context, progress: UserProgress) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(LEVEL, progress.level)
            .putInt(XP, progress.xp)
            .putInt(COMPLETED, progress.completedTasks)
            .apply()
    }
}

private enum class AppPage { HOME, TIMER }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudySprintApp(context: Context) {
    val tasks = remember { mutableStateListOf<StudyTask>().apply { addAll(StudyStore.loadTasks(context)) } }
    var progress by remember { mutableStateOf(StudyStore.loadProgress(context)) }
    var page by remember { mutableStateOf(AppPage.HOME) }
    var drawerOpen by remember { mutableStateOf(false) }
    var addTaskOpen by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var calendarMonth by remember { mutableStateOf(YearMonth.now()) }

    var totalHours by remember { mutableIntStateOf(1) }
    var totalMinutes by remember { mutableIntStateOf(0) }
    var breakCount by remember { mutableIntStateOf(1) }
    var breakMinutes by remember { mutableIntStateOf(10) }
    var phases by remember { mutableStateOf<List<StudyPhase>>(emptyList()) }
    var phaseIndex by remember { mutableIntStateOf(0) }
    var remainingMs by remember { mutableLongStateOf(0L) }
    var timerRunning by remember { mutableStateOf(false) }
    var timerStarted by remember { mutableStateOf(false) }

    fun saveAll() {
        StudyStore.saveTasks(context, tasks.toList())
        StudyStore.saveProgress(context, progress)
    }

    fun awardTaskXp(task: StudyTask) {
        val earned = when (task.size.lowercase()) {
            "medium" -> 15
            "large" -> 25
            else -> 10
        }
        var level = progress.level
        var xp = progress.xp + earned
        while (xp >= 100) {
            xp -= 100
            level += 1
        }
        progress = UserProgress(level, xp, progress.completedTasks + 1)
    }

    fun updateTask(task: StudyTask, newProgress: Int) {
        val bounded = newProgress.coerceIn(0, 100)
        val nowCompleted = bounded == 100
        val old = tasks.firstOrNull { it.id == task.id } ?: return
        val shouldAward = nowCompleted && !old.rewardClaimed
        val updated = old.copy(progress = bounded, completed = nowCompleted, rewardClaimed = old.rewardClaimed || shouldAward)
        val index = tasks.indexOfFirst { it.id == task.id }
        if (index >= 0) tasks[index] = updated
        if (shouldAward) awardTaskXp(updated)
        // Once earned, XP is not awarded again if the task is later marked incomplete and completed again.
        saveAll()
    }

    LaunchedEffect(timerRunning) {
        while (timerRunning && timerStarted) {
            delay(1000L)
            if (remainingMs > 1000L) {
                remainingMs -= 1000L
            } else {
                if (phaseIndex < phases.lastIndex) {
                    phaseIndex += 1
                    remainingMs = phases[phaseIndex].durationMs
                } else {
                    remainingMs = 0L
                    timerRunning = false
                    timerStarted = false
                }
            }
        }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Ink) {
            Scaffold(
                containerColor = Ink,
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text("StudySprint", color = WhiteText, fontWeight = FontWeight.Bold)
                                Text(if (page == AppPage.HOME) "Your study space" else "Focus timer", color = Muted, fontSize = 12.sp)
                            }
                        },
                        navigationIcon = {
                            Text(
                                "☰",
                                color = WhiteText,
                                fontSize = 25.sp,
                                modifier = Modifier.padding(horizontal = 16.dp).clickable { drawerOpen = !drawerOpen }
                            )
                        }
                    )
                }
            ) { padding ->
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    if (page == AppPage.HOME) {
                        HomeScreen(
                            tasks = tasks,
                            progress = progress,
                            selectedDate = selectedDate,
                            month = calendarMonth,
                            onPreviousMonth = { calendarMonth = calendarMonth.minusMonths(1) },
                            onNextMonth = { calendarMonth = calendarMonth.plusMonths(1) },
                            onSelectDate = { selectedDate = it },
                            onAddTask = { addTaskOpen = true },
                            onProgressChange = ::updateTask,
                            onDeleteTask = {
                                tasks.removeAll { task -> task.id == it.id }
                                saveAll()
                            }
                        )
                    } else {
                        TimerScreen(
                            totalHours = totalHours,
                            totalMinutes = totalMinutes,
                            breakCount = breakCount,
                            breakMinutes = breakMinutes,
                            onHoursChange = { totalHours = it },
                            onMinutesChange = { totalMinutes = it },
                            onBreakCountChange = { breakCount = it },
                            onBreakMinutesChange = { breakMinutes = it },
                            phases = phases,
                            phaseIndex = phaseIndex,
                            remainingMs = remainingMs,
                            running = timerRunning,
                            started = timerStarted,
                            onStart = {
                                val totalMs = (totalHours * 60L + totalMinutes) * 60_000L
                                val breaks = breakCount.coerceAtLeast(0)
                                val breakMs = breakMinutes * 60_000L
                                val studyMs = totalMs - breaks * breakMs
                                if (totalMs > 0 && studyMs > 0) {
                                    val studySegments = breaks + 1
                                    val baseStudy = studyMs / studySegments
                                    val remainder = studyMs % studySegments
                                    val built = mutableListOf<StudyPhase>()
                                    for (i in 0 until studySegments) {
                                        val segmentMs = baseStudy + if (i == 0) remainder else 0L
                                        if (segmentMs > 0) built.add(StudyPhase("Study time", segmentMs, false))
                                        if (i < breaks && breakMs > 0) built.add(StudyPhase("Break time", breakMs, true))
                                    }
                                    phases = built
                                    phaseIndex = 0
                                    remainingMs = built.firstOrNull()?.durationMs ?: 0L
                                    timerStarted = built.isNotEmpty()
                                    timerRunning = built.isNotEmpty()
                                }
                            },
                            onPauseResume = { timerRunning = !timerRunning },
                            onReset = {
                                timerRunning = false
                                timerStarted = false
                                phases = emptyList()
                                phaseIndex = 0
                                remainingMs = 0L
                            }
                        )
                    }

                    AnimatedVisibility(
                        visible = drawerOpen,
                        enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
                        exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(0.78f).background(Panel).padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text("DASHBOARD", color = Cyan, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Level ${progress.level}  •  ${progress.xp}/100 XP", color = WhiteText)
                            LinearProgressIndicator(
                                progress = { progress.xp / 100f },
                                modifier = Modifier.fillMaxWidth(),
                                color = Purple
                            )
                            Spacer(Modifier.height(4.dp))
                            DrawerButton("⌂  Home dashboard") {
                                page = AppPage.HOME
                                drawerOpen = false
                            }
                            DrawerButton("◷  Study timer") {
                                page = AppPage.TIMER
                                drawerOpen = false
                            }
                            Spacer(Modifier.weight(1f))
                            Text("StudySprint • Stay consistent", color = Muted, fontSize = 12.sp)
                            OutlinedButton(onClick = { drawerOpen = false }, modifier = Modifier.fillMaxWidth()) {
                                Text("Close menu")
                            }
                        }
                    }
                }
            }

            if (addTaskOpen) {
                AddTaskDialog(
                    onDismiss = { addTaskOpen = false },
                    onCreate = { title, description, dueDate, size ->
                        tasks.add(
                            StudyTask(
                                id = System.currentTimeMillis(),
                                title = title,
                                description = description,
                                dueDate = dueDate,
                                size = size
                            )
                        )
                        tasks.sortBy { it.dueDate }
                        saveAll()
                        addTaskOpen = false
                    }
                )
            }
        }
    }
}

@Composable
private fun DrawerButton(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(label) }
}

@Composable
private fun HomeScreen(
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
    val dueTasks = tasks.filter { it.dueDate == selectedDate.toString() }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("YOUR PROGRESS", color = Cyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                        progress = { progress.xp / 100f },
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
        if (tasks.isEmpty()) {
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
            items(tasks, key = { it.id }) { task ->
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
            Text("Tasks for ${selectedDate}", color = WhiteText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(task.title, color = WhiteText, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    if (task.description.isNotBlank()) Text(task.description, color = Muted, fontSize = 13.sp)
                    Text("Due ${task.dueDate}  •  ${task.size}  •  ${task.sizeXp()} XP", color = Cyan, fontSize = 12.sp)
                }
                TextButton(onClick = onDelete) { Text("Delete", color = Color(0xFFFF9292)) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${task.progress}%", color = WhiteText, fontWeight = FontWeight.Bold, modifier = Modifier.width(48.dp))
                LinearProgressIndicator(
                    progress = { task.progress / 100f },
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

private fun StudyTask.sizeXp(): Int = when (size.lowercase()) {
    "medium" -> 15
    "large" -> 25
    else -> 10
}

@Composable
private fun CalendarCard(
    month: YearMonth,
    selectedDate: LocalDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    tasks: List<StudyTask>
) {
    val firstDay = month.atDay(1)
    val offset = firstDay.dayOfWeek.value - 1
    val days = month.lengthOfMonth()
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Study calendar", color = WhiteText, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = onPreviousMonth) { Text("‹", color = Cyan, fontSize = 22.sp) }
                Text("${month.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${month.year}", color = WhiteText, fontSize = 13.sp)
                TextButton(onClick = onNextMonth) { Text("›", color = Cyan, fontSize = 22.sp) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                    Text(it, color = Muted, modifier = Modifier.width(32.dp), fontSize = 12.sp)
                }
            }
            val cells = buildList<LocalDate?> {
                repeat(offset) { add(null) }
                for (day in 1..days) add(month.atDay(day))
            }
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    week.forEach { date ->
                        if (date == null) {
                            Box(Modifier.size(32.dp))
                        } else {
                            val hasTask = tasks.any { it.dueDate == date.toString() }
                            val isSelected = date == selectedDate
                            val isToday = date == LocalDate.now()
                            Box(
                                modifier = Modifier.size(32.dp)
                                    .background(
                                        when {
                                            isSelected -> Purple
                                            isToday -> Cyan.copy(alpha = 0.22f)
                                            else -> Color.Transparent
                                        },
                                        CircleShape
                                    )
                                    .clickable { onSelectDate(date) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    Text("${date.dayOfMonth}", color = if (isSelected) Color.White else WhiteText, fontSize = 12.sp)
                                    if (hasTask) Box(Modifier.size(4.dp).background(if (isSelected) Color.White else Cyan, CircleShape))
                                }
                            }
                        }
                    }
                    repeat(7 - week.size) { Spacer(Modifier.size(32.dp)) }
                }
            }
            Text("Today: ${LocalDate.now()}  •  Selected: $selectedDate", color = Muted, fontSize = 12.sp)
            Text("A cyan dot marks a date with an assignment.", color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun AddTaskDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var size by remember { mutableStateOf("Small") }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Panel,
        title = { Text("Add assignment", color = WhiteText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it; error = "" }, label = { Text("Task title") }, singleLine = true)
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it; error = "" },
                    label = { Text("Due date (YYYY-MM-DD)") },
                    singleLine = true
                )
                Text("Task size", color = WhiteText, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Small", "Medium", "Large").forEach { option ->
                        if (size == option) Button(onClick = { size = option }) { Text(option) }
                        else OutlinedButton(onClick = { size = option }) { Text(option) }
                    }
                }
                Text("Reward: ${when (size) { "Small" -> 10; "Medium" -> 15; else -> 25 }} XP", color = Cyan)
                if (error.isNotBlank()) Text(error, color = Color(0xFFFF9292), fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(onClick = {
                val parsed = try { LocalDate.parse(dueDate); true } catch (_: DateTimeParseException) { false }
                when {
                    title.isBlank() -> error = "Please enter a task title."
                    !parsed -> error = "Enter a valid date, for example 2026-10-25."
                    else -> onCreate(title.trim(), description.trim(), dueDate, size)
                }
            }) { Text("Add task") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun TimerScreen(
    totalHours: Int,
    totalMinutes: Int,
    breakCount: Int,
    breakMinutes: Int,
    onHoursChange: (Int) -> Unit,
    onMinutesChange: (Int) -> Unit,
    onBreakCountChange: (Int) -> Unit,
    onBreakMinutesChange: (Int) -> Unit,
    phases: List<StudyPhase>,
    phaseIndex: Int,
    remainingMs: Long,
    running: Boolean,
    started: Boolean,
    onStart: () -> Unit,
    onPauseResume: () -> Unit,
    onReset: () -> Unit
) {
    val currentPhase = phases.getOrNull(phaseIndex)
    val background = when {
        !started -> Ink
        currentPhase?.isBreak == true -> Color(0xFF143C2A)
        else -> Color(0xFF48252E)
    }
    Column(
        modifier = Modifier.fillMaxSize().background(background).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (!started) {
            Text("Plan your focus session", color = WhiteText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Your chosen duration includes all breaks.", color = Muted)
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Total session length", color = WhiteText, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberControl("Hours", totalHours, 0, 12, onHoursChange)
                        NumberControl("Minutes", totalMinutes, 0, 59, onMinutesChange)
                    }
                    Text("Number of breaks: $breakCount", color = WhiteText)
                    Slider(value = breakCount.toFloat(), onValueChange = { onBreakCountChange(it.toInt()) }, valueRange = 0f..5f, steps = 4)
                    Text("Length of each break: $breakMinutes min", color = WhiteText)
                    Slider(value = breakMinutes.toFloat(), onValueChange = { onBreakMinutesChange(it.toInt()) }, valueRange = 1f..30f, steps = 28)
                    val total = totalHours * 60 + totalMinutes
                    val study = total - breakCount * breakMinutes
                    Text(
                        if (study > 0) "Study time: $study min  •  Break time: ${breakCount * breakMinutes} min"
                        else "Total session must be longer than the combined breaks.",
                        color = if (study > 0) Cyan else Color(0xFFFF9292),
                        fontSize = 13.sp
                    )
                    Button(
                        onClick = onStart,
                        enabled = total > 0 && study > 0,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Start focus session") }
                }
            }
            Text("Study phases are divided evenly around your breaks.", color = Muted, fontSize = 12.sp)
        } else {
            Text("FOCUS SESSION", color = WhiteText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(currentPhase?.name ?: "Session complete", color = WhiteText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(
                formatTime(remainingMs),
                color = WhiteText,
                fontSize = 54.sp,
                fontWeight = FontWeight.Bold
            )
            Text("Phase ${min(phaseIndex + 1, phases.size)} of ${phases.size}", color = WhiteText.copy(alpha = 0.8f))
            LinearProgressIndicator(
                progress = {
                    val duration = currentPhase?.durationMs ?: 1L
                    (1f - remainingMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                },
                modifier = Modifier.fillMaxWidth().height(9.dp),
                color = if (currentPhase?.isBreak == true) Cyan else Color(0xFFFF9B9B),
                trackColor = Color.White.copy(alpha = 0.2f)
            )
            Spacer(Modifier.height(18.dp))
            Button(onClick = onPauseResume, modifier = Modifier.fillMaxWidth()) {
                Text(if (running) "Pause timer" else "Resume timer")
            }
            OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
                Text("Reset and change settings", color = WhiteText)
            }
            Text(
                if (running) "Keep this screen open to watch the session." else "Timer paused or finished.",
                color = WhiteText.copy(alpha = 0.8f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun NumberControl(label: String, value: Int, minValue: Int, maxValue: Int, onChange: (Int) -> Unit) {
    Column(Modifier.width(150.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Muted, fontSize = 12.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { onChange(max(minValue, value - 1)) }) { Text("−", color = Cyan, fontSize = 22.sp) }
            Text(value.toString(), color = WhiteText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = { onChange(min(maxValue, value + 1)) }) { Text("+", color = Cyan, fontSize = 22.sp) }
        }
    }
}

private fun formatTime(milliseconds: Long): String {
    val seconds = max(0L, milliseconds / 1000L)
    val hours = seconds / 3600L
    val minutes = (seconds % 3600L) / 60L
    val secs = seconds % 60L
    return if (hours > 0) "%02d:%02d:%02d".format(hours, minutes, secs)
    else "%02d:%02d".format(minutes, secs)
}
