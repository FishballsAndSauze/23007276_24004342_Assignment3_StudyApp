package com.example.a23007276_24004342_assignment3_studyapp

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

data class UserProgress(
    val level: Int = 1,
    val xp: Int = 0,
    val completedTasks: Int = 0,
    val streak: Int = 0,
    val lastStudyDate: String = "",
    val totalStudyMinutes: Int = 0
)

internal enum class AppPage {
    HOME, TIMER, STATS, ACHIEVEMENTS, PROFILE, SETTINGS
}

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val unlocked: Boolean
)