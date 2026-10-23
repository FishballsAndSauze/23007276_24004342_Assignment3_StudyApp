package com.example.a23007276_24004342_assignment3_studyapp

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

internal object StudyDataStore {
    private const val PREFS = "studybuddy_store"
    private const val TASKS = "tasks"
    private const val LEVEL = "level"
    private const val XP = "xp"
    private const val COMPLETED = "completed"
    private const val STREAK = "streak"
    private const val LAST_STUDY_DATE = "last_study_date"
    private const val TOTAL_STUDY_MINUTES = "total_study_minutes"
    private const val USER_NAME = "user_name"

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
            // Start with empty list if data is corrupted
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
            .edit()
            .putString(TASKS, array.toString())
            .apply()
    }

    fun loadProgress(context: Context): UserProgress {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return UserProgress(
            level = prefs.getInt(LEVEL, 1),
            xp = prefs.getInt(XP, 0),
            completedTasks = prefs.getInt(COMPLETED, 0),
            streak = prefs.getInt(STREAK, 0),
            lastStudyDate = prefs.getString(LAST_STUDY_DATE, "") ?: "",
            totalStudyMinutes = prefs.getInt(TOTAL_STUDY_MINUTES, 0)
        )
    }

    fun saveProgress(context: Context, progress: UserProgress) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(LEVEL, progress.level)
            .putInt(XP, progress.xp)
            .putInt(COMPLETED, progress.completedTasks)
            .putInt(STREAK, progress.streak)
            .putString(LAST_STUDY_DATE, progress.lastStudyDate)
            .putInt(TOTAL_STUDY_MINUTES, progress.totalStudyMinutes)
            .apply()
    }

    fun loadUserName(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(USER_NAME, "Student") ?: "Student"
    }

    fun saveUserName(context: Context, name: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(USER_NAME, name)
            .apply()
    }

    fun resetAll(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}