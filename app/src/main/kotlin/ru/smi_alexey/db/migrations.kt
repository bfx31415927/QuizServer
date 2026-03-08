package ru.smi_alexey.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import ru.smi_alexey.quizserver.app.dbUrl
import ru.smi_alexey.quizserver.app.dbUser
import ru.smi_alexey.quizserver.app.dbUserPassword

object MigrationUtils {
    private fun createDataSource() = HikariDataSource(HikariConfig().apply {
        jdbcUrl = dbUrl
        username = dbUser
        password = dbUserPassword
        driverClassName = "org.postgresql.Driver"
    })

    fun runMigrations() {
        // Создаём DataSource
        val dataSource = createDataSource()
//        AppLogger.info("✅ DataSource создан")

        // Подключаемся к БД с помощью Exposed
        Database.connect(dataSource)
//        AppLogger.info("✅ Exposed подключён к DataSource")

        `AppLogger.kt`.info("🚀 Настраиваем Flyway...")
        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
//            .validateOnMigrate(true)
//            .baselineOnMigrate(false)
            .load()
//        AppLogger.info("✅ Flyway настроен")

        // Запускаем миграции...
        `AppLogger.kt`.info("🔍 Запускаем миграции...")
        flyway.migrate()

//        dataSource.close() // Не закрывать, если будет использоваться дальше
    }
}

fun runMigrations() {
    try {
        MigrationUtils.runMigrations()
        `AppLogger.kt`.info("🎉 Миграции успешно применены")
    } catch (t: Throwable) {
        `AppLogger.kt`.info("🎉 C миграциями проблемы")
        `AppLogger.kt`.info("❌ Тип ошибки: ${t.javaClass}")
        `AppLogger.kt`.info("📝 Сообщение: ${t.message}")
        `AppLogger.kt`.info("📊 Стектрейс:")
        t.printStackTrace()
    }
}
