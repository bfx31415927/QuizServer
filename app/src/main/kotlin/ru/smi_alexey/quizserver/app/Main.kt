package ru.smi_alexey.quizserver.app

import ru.smi_alexey.db.MigrationUtils.runMigrations
import ru.smi_alexey.log.AppLogger
import ru.smi_alexey.server.startEmbeddedServer

fun main() {
    AppLogger.info("Запуск main()...")
    runMigrations()
    startEmbeddedServer()
}