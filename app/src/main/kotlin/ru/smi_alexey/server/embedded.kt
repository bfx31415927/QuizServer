package ru.smi_alexey.server

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import org.slf4j.event.Level
import ru.smi_alexey.db.testConnection
import ru.smi_alexey.log.AppLogger
import ru.smi_alexey.quizserver.app.serverPort
import java.time.Duration

fun startEmbeddedServer() {
    embeddedServer(Netty, port = serverPort, host = "0.0.0.0") {

// Пока /get и /post не используются
//        install(ContentNegotiation) {
//            json()
//        }

        install(CallLogging) {
            level = Level.INFO
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

                    testConnection()

                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            AppLogger.info("WebSocket received from $clientAddress: '$text'")
                            val responseText = "Echo (uppercase): ${text.uppercase()}"
                            send(Frame.Text(responseText))
                            AppLogger.info("WebSocket sent to $clientAddress: '$responseText'")
                        }
                    }
                } catch (e: Exception) {
                    AppLogger.error("WebSocket error with $clientAddress", e)
                    close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, "Error"))
                } finally {
                    AppLogger.error("WebSocket disconnected: $clientAddress")
                }
            }
        }
    }.start(wait = true)
}