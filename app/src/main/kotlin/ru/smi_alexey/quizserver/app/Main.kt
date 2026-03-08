package ru.smi_alexey.quizserver.app

import ru.smi_alexey.db.MigrationUtils.runMigrations
import ru.smi_alexey.server.startEmbeddedServer

fun main() {
    `AppLogger.kt`.info("Запуск main()...")
    runMigrations()
    startEmbeddedServer()
}