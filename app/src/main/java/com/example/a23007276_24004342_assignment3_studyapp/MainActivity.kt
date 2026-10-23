// Archibald Po  (23007276)
// Dhairya Rathod (24004342)
// Pair submission
// Assignment 3

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
    // ---------- Data ----------
    val tasks = remember {
        mutableStateListOf<StudyTask>().apply {
            addAll(StudyDataStore.loadTasks(context))
        }
    }
    var progress by remember { mutableStateOf(StudyDataStore.loadProgress(context)) }
    var userName by remember { mutableStateOf(StudyDataStore.loadUserName(context)) }

    // ---------- Navigation ----------
    var page by remember { mutableStateOf(AppPage.HOME) }
    var drawerOpen by remember { mutableStateOf(false) }
    var addTaskOpen by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var calendarMonth by remember { mutableStateOf(YearMonth.now()) }

    // ---------- Level-up dialog ----------
    var showLevelUp by remember { mutableStateOf(false) }
    var newLevel by remember { mutableIntStateOf(1) }

    // ---------- Timer state ----------
    var totalHours by remember { mutableIntStateOf(1) }
    var totalMinutes by remember { mutableIntStateOf(0) }
    var breakCount by remember { mutableIntStateOf(1) }
    var breakMinutes by remember { mutableIntStateOf(10) }
    var phases by remember { mutableStateOf<List<StudyPhase>>(emptyList()) }
    var phaseIndex by remember { mutableIntStateOf(0) }
    var remainingMs by remember { mutableLongStateOf(0L) }
    var timerRunning by remember { mutableStateOf(false) }
    var timerStarted by remember { mutableStateOf(false) }

    // ---------- Helpers ----------
    fun saveAll() {
        StudyDataStore.saveTasks(context, tasks.toList())
        StudyDataStore.saveProgress(context, progress)
    }

    fun updateStreak() {
        val today = LocalDate.now().toString()
        val yesterday = LocalDate.now().minusDays(1).toString()
        val last = progress.lastStudyDate

        val newStreak = when {
            last == today -> progress.streak          // already counted today
            last == yesterday -> progress.streak + 1  // continue streak
            last.isEmpty() -> 1                       // first ever
            else -> 1                                 // broken streak
        }

        if (last != today) {
            progress = progress.copy(
                streak = newStreak,
                lastStudyDate = today
            )
            saveAll()
        }
    }

    fun awardTaskXp(task: StudyTask) {
        val earned = when (task.size.lowercase()) {
            "medium" -> 15
            "large" -> 25
            else -> 10
        }
        var level = progress.level
        var xp = progress.xp + earned
        val oldLevel = level

        while (xp >= 100) {
            xp -= 100
            level += 1
        }

        progress = progress.copy(
            level = level,
            xp = xp,
            completedTasks = progress.completedTasks + 1
        )

        if (level > oldLevel) {
            newLevel = level
            showLevelUp = true
        }
        updateStreak()
        saveAll()
    }

    fun updateTask(task: StudyTask, newProgress: Int) {
        val bounded = newProgress.coerceIn(0, 100)
        val nowCompleted = bounded == 100
        val old = tasks.firstOrNull { it.id == task.id } ?: return
        val shouldAward = nowCompleted && !old.rewardClaimed

        val updated = old.copy(
            progress = bounded,
            completed = nowCompleted,
            rewardClaimed = old.rewardClaimed || shouldAward
        )

        val index = tasks.indexOfFirst { it.id == task.id }
        if (index >= 0) tasks[index] = updated

        if (shouldAward) {
            awardTaskXp(updated)
        } else {
            saveAll()
        }
    }

    // ---------- Timer logic ----------
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
                    // Add study minutes when session finishes
                    val studyMinutes = phases
                        .filter { !it.isBreak }
                        .sumOf { it.durationMs } / 60_000
                    if (studyMinutes > 0) {
                        progress = progress.copy(
                            totalStudyMinutes = progress.totalStudyMinutes + studyMinutes.toInt()
                        )
                        updateStreak()
                        saveAll()
                    }
                }
            }
        }
    }

    // Pause timer when app goes to background
    DisposableEffect(Unit) {
        val lifecycle = (context as ComponentActivity).lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                timerRunning = false
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    // ---------- UI ----------
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize(), color = Ink) {
            Scaffold(
                containerColor = Ink,
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text("StudyBuddy", color = WhiteText, fontWeight = FontWeight.Bold)
                                Text(
                                    when (page) {
                                        AppPage.HOME -> "Your study space"
                                        AppPage.TIMER -> "Focus timer"
                                        AppPage.STATS -> "Your stats"
                                        AppPage.ACHIEVEMENTS -> "Achievements"
                                        AppPage.PROFILE -> "Profile"
                                        AppPage.SETTINGS -> "Settings"
                                    },
                                    color = Muted,
                                    fontSize = 12.sp
                                )
                            }
                        },
                        navigationIcon = {
                            Text(
                                "☰",
                                color = WhiteText,
                                fontSize = 25.sp,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .clickable { drawerOpen = !drawerOpen }
                            )
                        }
                    )
                }
            ) { padding ->
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {

                    when (page) {
                        AppPage.HOME -> {
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
                        }

                        AppPage.TIMER -> {
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
                                            if (segmentMs > 0) {
                                                built.add(StudyPhase("Study time", segmentMs, false))
                                            }
                                            if (i < breaks && breakMs > 0) {
                                                built.add(StudyPhase("Break time", breakMs, true))
                                            }
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

                        AppPage.STATS -> {
                            StatsScreen(progress = progress, tasks = tasks)
                        }

                        AppPage.ACHIEVEMENTS -> {
                            AchievementsScreen(progress = progress, tasks = tasks)
                        }

                        AppPage.PROFILE -> {
                            ProfileScreen(
                                progress = progress,
                                userName = userName,
                                onNameChange = {
                                    userName = it
                                    StudyDataStore.saveUserName(context, it)
                                }
                            )
                        }

                        AppPage.SETTINGS -> {
                            SettingsScreen(
                                onReset = {
                                    StudyDataStore.resetAll(context)
                                    tasks.clear()
                                    progress = UserProgress()
                                    userName = "Student"
                                    page = AppPage.HOME
                                }
                            )
                        }
                    }

                    // ---------- Drawer ----------
                    AnimatedVisibility(
                        visible = drawerOpen,
                        enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
                        exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.78f)
                                .background(Panel)
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("DASHBOARD", color = Cyan, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Level ${progress.level}  •  ${progress.xp}/100 XP", color = WhiteText)
                            LinearProgressIndicator(
                                progress = { progress.xp / 100f },
                                modifier = Modifier.fillMaxWidth(),
                                color = Purple
                            )
                            if (progress.streak > 0) {
                                Text("🔥 ${progress.streak} day streak", color = Orange, fontSize = 13.sp)
                            }

                            Spacer(Modifier.height(8.dp))

                            DrawerButton("⌂  Home") {
                                page = AppPage.HOME
                                drawerOpen = false
                            }
                            DrawerButton("◷  Study Timer") {
                                page = AppPage.TIMER
                                drawerOpen = false
                            }
                            DrawerButton("📊  Stats") {
                                page = AppPage.STATS
                                drawerOpen = false
                            }
                            DrawerButton("★  Achievements") {
                                page = AppPage.ACHIEVEMENTS
                                drawerOpen = false
                            }
                            DrawerButton("👤  Profile") {
                                page = AppPage.PROFILE
                                drawerOpen = false
                            }
                            DrawerButton("⚙  Settings") {
                                page = AppPage.SETTINGS
                                drawerOpen = false
                            }

                            Spacer(Modifier.weight(1f))
                            Text("StudyBuddy • Stay consistent", color = Muted, fontSize = 12.sp)
                            OutlinedButton(
                                onClick = { drawerOpen = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Close menu")
                            }
                        }
                    }
                }
            }

            // ---------- Add Task Dialog ----------
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

            // ---------- Level-up Dialog ----------
            if (showLevelUp) {
                AlertDialog(
                    onDismissRequest = { showLevelUp = false },
                    containerColor = Panel,
                    title = {
                        Text("Level Up!", color = WhiteText, fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Congratulations!", color = Cyan, fontSize = 18.sp)
                            Text("You reached Level $newLevel", color = WhiteText, fontSize = 16.sp)
                            Text("Keep up the great work!", color = Muted)
                        }
                    },
                    confirmButton = {
                        Button(onClick = { showLevelUp = false }) {
                            Text("Awesome!")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun DrawerButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label)
    }
}