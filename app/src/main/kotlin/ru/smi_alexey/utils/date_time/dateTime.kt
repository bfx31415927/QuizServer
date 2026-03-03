package ru.smi_alexey.utils.date_time

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun currentDateTimeFormatted(): String {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss.SSS")
    return "[${LocalDateTime.now().format(formatter)}]"
}
