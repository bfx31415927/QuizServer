package ru.smi_alexey.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import ru.smi_alexey.log.log
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
//        log.info("✅ DataSource создан")

        // Подключаемся к БД с помощью Exposed
        Database.connect(dataSource)
//        log.info("✅ Exposed подключён к DataSource")

        log.info("🚀 Настраиваем Flyway...")
        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
//            .validateOnMigrate(true)
//            .baselineOnMigrate(false)
            .load()
//        log.info("✅ Flyway настроен")

        // Запускаем миграции...
        log.info("🔍 Запускаем миграции...")
        flyway.migrate()

//        dataSource.close() // Не закрывать, если будет использоваться дальше
    }
}

fun runMigrations() {
    try {
        MigrationUtils.runMigrations()
        log.info("🎉 Миграции успешно применены")
    } catch (t: Throwable) {
        log.info("🎉 C миграциями проблемы")
        log.info("❌ Тип ошибки: ${t.javaClass}")
        log.info("📝 Сообщение: ${t.message}")
        log.info("📊 Стектрейс:")
        t.printStackTrace()
    }
}
