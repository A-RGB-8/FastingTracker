package com.example.fastingtracker.utils

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.LocalDate

fun showDateTimePicker(
    context: Context,
    initialDateTime: LocalDateTime = LocalDateTime.now(),
    onDateTimeSelected: (LocalDateTime) -> Unit
) {
    val date = initialDateTime.toLocalDate()
    val time = initialDateTime.toLocalTime()

    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
            
            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    val selectedDateTime = LocalDateTime.of(
                        selectedDate,
                        LocalTime.of(hourOfDay, minute)
                    )
                    onDateTimeSelected(selectedDateTime)
                },
                time.hour,
                time.minute,
                true // 24-hour format
            ).show()
        },
        date.year,
        date.monthValue - 1,
        date.dayOfMonth
    ).show()
}