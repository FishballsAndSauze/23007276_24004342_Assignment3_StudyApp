// Archibald Po  (23007276)
// Dhairya Rathod (24004342)
// Pair submission
//Assignment 3

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.YearMonth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { StudyBuddyApp(this) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyBuddyApp(context: Context) {
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

    MaterialTheme (darkColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize(), color = Ink) {
            Scaffold(
                containerColor = Ink,
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text("StudyBuddy", color = WhiteText, fontWeight = FontWeight.Bold)
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
                            Text("StudyBuddy • Stay consistent", color = Muted, fontSize = 12.sp)
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
