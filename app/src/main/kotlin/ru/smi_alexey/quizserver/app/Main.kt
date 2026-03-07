package ru.smi_alexey.quizserver.app

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.event.Level
import java.time.Duration


object MigrationUtils {
    private fun createDataSource() = HikariDataSource(HikariConfig().apply {
        jdbcUrl = "jdbc:postgresql://localhost:5432/quiz_db"
        username = "quiz_user"
        password = "31415926"
        driverClassName = "org.postgresql.Driver"
    })

    fun runMigrations() {
        println("🔧 Создаём DataSource...")
        val dataSource = createDataSource()
        println("✅ DataSource создан")

        // Подключаем Exposed — чтобы потом работал transaction
        Database.connect(dataSource)
        println("✅ Exposed подключён к DataSource")

        println("🚀 Настраиваем Flyway...")
        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
//            .validateOnMigrate(false)
//            .baselineOnMigrate(true)
            .load()
        println("✅ Flyway настроен")

        println("🔍 Запускаем миграции...")
        flyway.migrate()

//        dataSource.close() // Не закрывать, если будет использоваться дальше
    }
}


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

fun main() {
    println("🟢 Запуск main()")
    // 🔎 Печатаем версию Flyway
    val flywayVersion = Flyway::class.java.`package`.implementationVersion
    println("📦 Flyway version: ${flywayVersion ?: "unknown (class not found)"}")


    try {
        MigrationUtils.runMigrations()
        println("🎉 Миграции успешно применены")
    } catch (t: Throwable) {  // ← важно: не Exception, а Throwable
        println("❌ Тип ошибки: ${t.javaClass}")
        println("📝 Сообщение: ${t.message}")
        println("📊 Стектрейс:")
        t.printStackTrace()
    }



    testConnection()

//    val StartTime = AttributeKey<Long>("StartTime")

    embeddedServer(Netty, port = serverPort, host = "0.0.0.0") {

//        intercept(ApplicationCallPipeline.Monitoring) {
//            call.attributes.put(StartTime, System.currentTimeMillis())
//        }

        install(ContentNegotiation) {
            json()
        }
        install(CallLogging) {
            level = Level.INFO
//            format { call ->
//                val method = call.request.httpMethod.value
//                val uri = call.request.uri
//                val status = call.response.status()?.value ?: "Unknown"
//                val start = call.attributes.getOrNull(StartTime) ?: System.currentTimeMillis()
//                val elapsed = System.currentTimeMillis() - start
//
//                "[HANDSHAKE_LOG] $method $uri → $status in ${elapsed}ms"
//            }
        }

        install(WebSockets) {
            pingPeriod = Duration.ofSeconds(15)
            timeout = Duration.ofMinutes(1)
            maxFrameSize = Long.MAX_VALUE
            masking = false
        }

        routing {
            webSocket("/ws") {
                val logger = application.environment.log
                val clientAddress = call.request.origin.remoteAddress
                try {
                    send(Frame.Text("Connected to WebSocket!"))
                    logger.info("WebSocket connected: $clientAddress")

                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            logger.info("WebSocket received from $clientAddress: '$text'")
                            val responseText = "Echo (uppercase): ${text.uppercase()}"
                            send(Frame.Text(responseText))
                            logger.info("WebSocket sent to $clientAddress: '$responseText'")
                        }
                    }
                } catch (e: Exception) {
                    logger.error("WebSocket error with $clientAddress", e)
                    close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, "Error"))
                } finally {
                    logger.info("WebSocket disconnected: $clientAddress")
                }
            }
        }
    }.start(wait = true)
}