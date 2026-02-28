package org.example.app

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.slf4j.event.Level
import io.ktor.http.*
import io.ktor.server.request.receive

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
        }
    }.start(wait = true)
}
