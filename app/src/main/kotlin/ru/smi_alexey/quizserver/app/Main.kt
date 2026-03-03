package ru.smi_alexey.quizserver.app

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.util.*
import io.ktor.websocket.*
import org.slf4j.event.Level
import java.time.Duration

fun main() {

    val StartTime = AttributeKey<Long>("StartTime")

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
//                val remoteAddress = call.request.origin.remoteAddress
//                val method = call.request.httpMethod.value
//                val uri = call.request.uri
//                val userAgent = call.request.headers["User-Agent"] ?: "Unknown"
//                val status = call.response.status() ?: HttpStatusCode.OK
//
//                // Получаем время начала из атрибутов
//                val startTime = call.attributes[StartTime]
//                val time = System.currentTimeMillis() - startTime
//
//                "REMOTE: $remoteAddress | $method $uri | $status | User-Agent: $userAgent | in ${time}ms"
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
                val clientAddress = call.request.origin.remoteHost
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