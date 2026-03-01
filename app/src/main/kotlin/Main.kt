package org.example.app

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.Serializable
import org.slf4j.event.Level
import java.time.Duration

@Serializable
data class Message(val text: String)

@Serializable
data class PostRequest(val userInput: String)

@Serializable
data class PostResponse(val processedText: String, val status: String)

fun main() {

    embeddedServer(Netty, port = 16999, host = "0.0.0.0") {
        install(ContentNegotiation) {
            json()
        }
        install(CallLogging) {
            level = Level.INFO
            format { call ->
                val remoteAddress = call.request.origin.remoteAddress
                val method = call.request.httpMethod.value
                val uri = call.request.uri
                val userAgent = call.request.headers["User-Agent"] ?: "Unknown"
                "REMOTE: $remoteAddress | $method $uri | User-Agent: $userAgent"
            }
        }
        // Установка поддержки WebSocket
        install(WebSockets) {
            pingPeriod = Duration.ofSeconds(15)
            timeout = Duration.ofMinutes(1)
            maxFrameSize = Long.MAX_VALUE
            masking = false
        }

        routing {
            get("/") {
                call.respond(Message("Hello from Ktor!"))
            }
            get("/ping") {
                call.respond(mapOf("status" to "ok", "message" to "Pong!"))
            }
            post("/post") {
                try {
                    val request = call.receive<PostRequest>()
                    val response = PostResponse(
                        processedText = "Received: ${request.userInput.uppercase()}",
                        status = "success"
                    )
                    call.respond(response)
                } catch (e: Exception) {
                    call.respondText("Error: ${e.message}", status = HttpStatusCode.BadRequest)
                }
            }

            // Новый маршрут — WebSocket
            webSocket("/ws") { // ws://localhost:16999/ws
                val logger = application.environment.log
                val clientAddress = call.request.origin.remoteHost

                logger.info("WebSocket connecting: $clientAddress")

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