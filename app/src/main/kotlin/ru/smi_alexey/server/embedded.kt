package ru.smi_alexey.server

import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.utils.io.CancellationException
import io.ktor.websocket.*
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.Json.Default.decodeFromString
import kotlinx.serialization.json.Json.Default.serializersModule
import org.slf4j.event.Level
import ru.smi_alexey.db.testConnection
import ru.smi_alexey.handle_client_message.handleWrapperMessage
import ru.smi_alexey.handle_client_message.handleWebSocketMessage
import ru.smi_alexey.handle_client_message.sendWrapperMessage
import ru.smi_alexey.log.log
import ru.smi_alexey.quizserver.app.serverPort
import ru.smi_alexey.serialization.MessageType
import ru.smi_alexey.serialization.MessageWrapper
import ru.smi_alexey.serialization.ServerResponse
import ru.smi_alexey.serialization.WebSocketMessage
import ru.smi_alexey.serialization.analyzeMessageType
import ru.smi_alexey.serialization.json
import ru.smi_alexey.serialization.webSocketSerializersModule
import java.io.IOException
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
            contentConverter = KotlinxWebsocketSerializationConverter(json)
        }

        routing {
            webSocket("/ws") {
                val clientAddress = call.request.origin.remoteAddress
                log.info("WebSocket connected: $clientAddress")

                var exceptionCode = 0
                try {
//                    send(Frame.Text("You are connected to WebSocket!"))//временно
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val jsonString = frame.readText()
                            log.debug("Получен JSON: $jsonString")
                            try {
                                val messageType = analyzeMessageType(jsonString)
                                when (messageType) {
                                    MessageType.DIRECT -> {
                                        // Прямой экземпляр sealed-класса
                                        val message = json.decodeFromString(
                                            WebSocketMessage.serializer(),
                                            jsonString
                                        )
                                        handleWebSocketMessage(this, message)
                                    }

                                    MessageType.WRAPPED -> {
                                        // Сообщение в обёртке
                                        val wrapper = json.decodeFromString<MessageWrapper>(jsonString)
                                        handleWrapperMessage(this, wrapper)
                                    }

                                    MessageType.UNKNOWN -> {
                                        val mess = "Получено сообщения неподдерживаемого формата: $jsonString"
                                        log.error(mess)
                                        sendWrapperMessage(this,
                                            ServerResponse( success = false, message = mess))
                                    }
                                }
                            } catch (e: Exception) {
                                val mess = "Ошибка обработки сообщения: $jsonString"
                                log.error(mess, e)
                                sendWrapperMessage(this,
                                    ServerResponse(success = false, message = mess))
                            }
                        } else {
                            val mess = "Получен фрейм неподдерживаемого типа: ${frame::class.simpleName}"
                            log.warn(mess)
                            sendWrapperMessage(this,
                                ServerResponse( success = false, message = mess))
                        }
                    }

                    log.info("Цикл 'for' завершён нормально: канал закрыт")

                } catch (e: ClosedReceiveChannelException) {
                    exceptionCode = 1
                    log.info("Цикл 'for' прерван: явное закрытие канала (ClosedReceiveChannelException)")
                } catch (e: CancellationException) {
                    exceptionCode = 2
                    log.info("Цикл 'for' прерван: корутина отменена (CancellationException)")
                } catch (e: TimeoutCancellationException) {
                    exceptionCode = 3
                    log.info("Цикл 'for' прерван: таймаут (TimeoutCancellationException)")
                } catch (e: IOException) {
                    exceptionCode = 4
                    log.error("Цикл 'for' прерван: сетевая ошибка (IOException): ${e.message}")
                } catch (e: Exception) {
                    exceptionCode = 5
                    log.error("Цикл 'for' прерван из‑за неожиданной ошибки: ${e.javaClass.simpleName}: ${e.message}")
                } finally {
                    log.error("WebSocket disconnected: $clientAddress")
                }
            }
        }
    }.start(wait = true)
}