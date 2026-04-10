package ru.smi_alexey.handle_client_message

import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.serializer
import ru.smi_alexey.clients.WebSocketClient
import ru.smi_alexey.clients.WebSocketClientManager
import ru.smi_alexey.log.log
import ru.smi_alexey.serialization.*


// Обработка авторизации
suspend fun handleAuthMessage(
    session: DefaultWebSocketServerSession,
    message: AuthMessage,
    client: WebSocketClient
) {
    val result = when (message.action) {
        "login" -> {
            WebSocketClientManager.authenticateClient(client.id, message.login, message.password)
        }
        "register" -> {
            WebSocketClientManager.registerClient(client.id, message.login, message.password, message.email!!)
        }
        else -> {
            sendDirectMessage(session, ServerResponse(success = false, message="auth_unknown"))
            return
        }
    }
    sendDirectMessage(session, ServerResponse(success=result, message="${message.action}"))
}

// Обработка прямого экземпляра sealed-класса
suspend fun handleWebSocketMessage(
    session: DefaultWebSocketServerSession,
    message: WebSocketMessage,
    client: WebSocketClient
) {
    when (message) {

        is AuthMessage -> {
            handleAuthMessage(session, message, client)
        }

        is TextMessage -> {
            val mess = "Получено сообщение TextMessage: $message"
            log.info(mess)
            sendDirectMessage(
                session,
                ServerResponse(success = true, message = mess)
            )
        }

        is CommandMessage -> {
            log.info("Получена команда: $message")
            sendDirectMessage(session, processCommand(message))
        }

        is StatusUpdate -> {
            val mess = "Статус пользователя обновлен: ${message.status}"
            log.info(mess)
            updateUserStatus(message.userId ?: "unknown", message.status)
            sendDirectMessage(
                session,
                ServerResponse(success = true, message = mess)
            )
        }

        is ClientResponse -> {
            log.info("Получено сообщение ClientResponse: $message")
        }

        is ServerResponse -> {
            //сюда попасть не должны
        }
    }
}

private fun processCommand(command: CommandMessage): ServerResponse {
    return when (command.command) {
        "start_game" -> ServerResponse(success = true, message = "Игра начата")
        "stop_game" -> ServerResponse(success = true, message = "Игра остановлена")
        else -> ServerResponse(success = false, message = "Неизвестная команда: ${command.command}")
    }
}

private fun updateUserStatus(userId: String, status: String) {
    log.info("У пользователя $userId теперь статус: '$status'")
}

suspend inline fun <reified T : WebSocketMessage> sendDirectMessage(
    session: DefaultWebSocketServerSession,
    message: T
) {
    try {
        val jsonString = json.encodeToString(
            WebSocketMessage.serializer(),
            message
        )

        val frame = Frame.Text(jsonString)
        session.send(frame)

        log.info("sendDirectMessage отправил сообщение: $jsonString")
    } catch (e: Exception) {
        log.error("Ошибка в sendDirectMessage: ${e.message}")
    }
}