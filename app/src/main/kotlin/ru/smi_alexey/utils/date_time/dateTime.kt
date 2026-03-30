package ru.smi_alexey.utils.date_time

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.ZoneId

fun currentDateTimeFormatted(): String {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss.SSS")
    return "[${LocalDateTime.now().format(formatter)}]"
}

fun Instant.toLocalString(): String {
    return this.atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss.SSS"))
}
