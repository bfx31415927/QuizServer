package ru.smi_alexey.db

import org.jetbrains.exposed.sql.transactions.transaction

fun testConnection() {
    transaction {
        println("Успешное подключение к БД!")
        exec("SELECT version() as ver") { rs ->
            if (rs.next()) {
                println("Версия PostgreSQL: ${rs.getString("ver")}")
            }
        }
    }
}