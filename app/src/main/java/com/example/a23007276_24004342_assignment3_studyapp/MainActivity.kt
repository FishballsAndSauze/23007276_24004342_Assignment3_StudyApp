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

// The single activity of the app. It only hosts the Compose UI;
// all screens and logic live in StudyBuddyApp and the other files.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { StudyBuddyApp(this) }
    }
}

// Root composable. It holds all shared app state (tasks, XP, timer, navigation),
// contains the logic that changes that state, and decides which screen to show.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyBuddyApp(context: Context) {
    // Saved data: tasks, level/XP progress and the user's name are loaded from
    // SharedPreferences once when the app starts.
    val tasks = remember {
        mutableStateListOf<StudyTask>().apply {
            addAll(StudyDataStore.loadTasks(context))
        }
    }
    var progress by remember { mutableStateOf(StudyDataStore.loadProgress(context)) }
    var userName by remember { mutableStateOf(StudyDataStore.loadUserName(context)) }

    // Navigation and UI state: which page is showing, whether the side drawer
    // or add-task dialog is open, and which date/month the calendar displays.
    var page by remember { mutableStateOf(AppPage.HOME) }
    var drawerOpen by remember { mutableStateOf(false) }
    var addTaskOpen by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var calendarMonth by remember { mutableStateOf(YearMonth.now()) }

    // Level-up pop-up state: whether to show it and which level was reached.
    var showLevelUp by remember { mutableStateOf(false) }
    var newLevel by remember { mutableIntStateOf(1) }

    // Timer state. The first four values are the user's chosen settings.
    // phases is the planned list of study/break blocks, phaseIndex is the block
    // currently running, and remainingMs is the time left in that block.
    var totalHours by remember { mutableIntStateOf(1) }
    var totalMinutes by remember { mutableIntStateOf(0) }
    var breakCount by remember { mutableIntStateOf(1) }
    var breakMinutes by remember { mutableIntStateOf(10) }
    var phases by remember { mutableStateOf<List<StudyPhase>>(emptyList()) }
    var phaseIndex by remember { mutableIntStateOf(0) }
    var remainingMs by remember { mutableLongStateOf(0L) }
    var timerRunning by remember { mutableStateOf(false) }
    var timerStarted by remember { mutableStateOf(false) }

    // Writes the current tasks and progress to storage so nothing is lost
    // when the app is closed.
    fun saveAll() {
        StudyDataStore.saveTasks(context, tasks.toList())
        StudyDataStore.saveProgress(context, progress)
    }

    // Updates the daily study streak. Doing something on consecutive days adds
    // one, missing a day restarts it at 1, and it only counts once per day.
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

    // Gives XP for a completed task (small 10, medium 15, large 25). Every 100 XP
    // is one level, and any leftover XP carries over to the next level.
    // Also increases the completed-task counter and triggers the level-up pop-up.
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

    // Called by the +20% / -20% buttons. Keeps progress between 0 and 100.
    // When a task first reaches 100% it is marked complete and XP is awarded;
    // rewardClaimed stops the same task giving XP twice.
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

    // The countdown loop. It restarts whenever timerRunning changes and ticks
    // once per second. When a phase ends it moves to the next one; when the last
    // phase ends it stops, adds the study minutes to the total and updates the streak.
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

    // Pause timer when app goes to background. Listens to the activity lifecycle
    // and stops the countdown on ON_STOP so no work is done while the app is hidden.
    // The observer is removed when this composable leaves the screen.
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

    // The visible UI: dark theme, a top bar with the menu button, and the
    // content area that shows whichever page is selected.
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize(), color = Ink) {
            Scaffold(
                containerColor = Ink,
                topBar = {
                    // Top bar: app name, a subtitle that changes with the page,
                    // and the ☰ button that opens or closes the drawer.
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

                    // Screen switcher: shows exactly one page depending on "page".
                    when (page) {
                        // Home: level card, task cards and the calendar.
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

                        // Timer: the settings screen before starting, then the live countdown.
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
                                // Builds the session plan. Total time includes the breaks, so
                                // study time = total - (breaks x break length). That study time is
                                // split evenly into (breaks + 1) blocks, with any leftover
                                // milliseconds added to the first block. Breaks go between blocks.
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
                                // Pause button toggles between paused and running.
                                onPauseResume = { timerRunning = !timerRunning },
                                // Reset clears the session and returns to the settings screen.
                                onReset = {
                                    timerRunning = false
                                    timerStarted = false
                                    phases = emptyList()
                                    phaseIndex = 0
                                    remainingMs = 0L
                                }
                            )
                        }

                        // Stats: study time, tasks done and a 7-day chart.
                        AppPage.STATS -> {
                            StatsScreen(progress = progress, tasks = tasks)
                        }

                        // Achievements: badges that unlock from tasks, levels, streaks and study time.
                        AppPage.ACHIEVEMENTS -> {
                            AchievementsScreen(progress = progress, tasks = tasks)
                        }

                        // Profile: shows the user's name and stats, and lets them edit the name.
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

                        // Settings: reset button that wipes all saved data and returns to Home.
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

                    // Dashboard drawer. It slides in from the left over the current page
                    // and shows the level, XP bar, streak and a shortcut button for each page.
                    // Choosing a page switches to it and closes the drawer.
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

            // Add-task dialog. Appears when the user taps "Add task". On confirm it creates
            // a new task from the entered title, description, due date and size, sorts the
            // list by due date, saves it and closes the dialog.
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

            // Level-up dialog. Shown after XP pushes the user into a new level.
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

// A reusable full-width button used for each shortcut in the dashboard drawer.
@Composable
private fun DrawerButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label)
    }
}