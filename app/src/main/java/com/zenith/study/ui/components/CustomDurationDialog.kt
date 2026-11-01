package com.zenith.study.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenith.study.ui.theme.BorderMedium
import com.zenith.study.ui.theme.CardCharcoal
import com.zenith.study.ui.theme.CardElevated
import com.zenith.study.ui.theme.NeonLime
import com.zenith.study.ui.theme.TextInverse
import com.zenith.study.ui.theme.TextMuted
import com.zenith.study.ui.theme.TextPrimary
import com.zenith.study.ui.theme.TextSecondary

@Composable
fun CustomDurationDialog(
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var textValue by remember { mutableStateOf(initialMinutes.toString()) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardCharcoal,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "Custom Focus Duration",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column {
                Text(
                    text = "Enter study block duration in minutes (1 - 180):",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = textValue,
                    onValueChange = {
                        textValue = it
                        val intVal = it.toIntOrNull()
                        errorText = when {
                            intVal == null -> "Please enter a valid number"
                            intVal !in 1..180 -> "Duration must be between 1 and 180 min"
                            else -> null
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = errorText != null,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonLime,
                        unfocusedBorderColor = BorderMedium,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        color = androidx.compose.ui.graphics.Color.Red,
                        fontSize = 11.sp,
                        modifier = Modifier.height(16.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val intVal = textValue.toIntOrNull()
                    if (intVal != null && intVal in 1..180) {
                        onConfirm(intVal)
                    }
                },
                enabled = errorText == null && textValue.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonLime)
            ) {
                Text("Set Duration", color = TextInverse, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CardElevated)
            ) {
                Text("Cancel", color = TextPrimary)
            }
        }
    )
}

