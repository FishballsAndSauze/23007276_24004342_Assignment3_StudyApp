package com.example.a23007276_24004342_assignment3_studyapp

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

internal object StudyStore {
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