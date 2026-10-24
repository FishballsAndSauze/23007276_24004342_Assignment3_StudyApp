package com.example.a23007276_24004342_assignment3_studyapp

/* One assignment. progress is 0-100, and rewardClaimed makes sure XP
is only given once even if the task is un-completed and completed again.*/
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

// One block of a timer session: either a study block or a break.
data class StudyPhase(val name: String, val durationMs: Long, val isBreak: Boolean)

// Everything about the user's progress that gets saved: level, XP,
// tasks completed, daily streak and total study minutes
data class UserProgress(
    val level: Int = 1,
    val xp: Int = 0,
    val completedTasks: Int = 0,
    val streak: Int = 0,
    val lastStudyDate: String = "",
    val totalStudyMinutes: Int = 0
)

// The pages the user can navigate between
internal enum class AppPage {
    HOME, TIMER, STATS, ACHIEVEMENTS, PROFILE, SETTINGS
}

// Badge shown on the Achievements page

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val unlocked: Boolean
)