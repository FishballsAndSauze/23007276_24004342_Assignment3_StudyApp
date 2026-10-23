package com.example.a23007276_24004342_assignment3_studyapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeParseException

@Composable
internal fun AddTaskDialog(
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