package ru.smi_alexey.handle_client_message

import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import ru.smi_alexey.log.log
import ru.smi_alexey.serialization.CommandMessage
import ru.smi_alexey.serialization.MessageWrapper
import ru.smi_alexey.serialization.ServerResponse
import ru.smi_alexey.serialization.StatusUpdate
import ru.smi_alexey.serialization.TextMessage
import ru.smi_alexey.serialization.WebSocketMessage
import ru.smi_alexey.serialization.json

// Обработка прямого экземпляра sealed-класса

suspend fun handleWebSocketMessage(
    session: DefaultWebSocketServerSession,
    message: WebSocketMessage
) {
    when (message) {
        is TextMessage -> {
            log.info("Получено текстовое сообщение: ${message.content}")
            val response = ServerResponse(true, "Сообщение получено: ${message.content}")
            val frame = Frame.Text(json.encodeToString(response))
            session.send(frame)
        }
        is CommandMessage -> {
            log.info("Получена команда: ${message.command}")
            val response = processCommand(message)
            val frame = Frame.Text(json.encodeToString(response))
            session.send(frame)
        }
        is StatusUpdate -> {
            log.info("Статус пользователя обновлен: ${message.status}")
            updateUserStatus(message.userId ?: "unknown", message.status)
            val response = ServerResponse(true, "Статус обновлен")
            val frame = Frame.Text(json.encodeToString(response))
            session.send(frame)
        }
    }
}

suspend fun handleMessageWrapper(
    session: DefaultWebSocketServerSession,
    wrapper: MessageWrapper
) {
    when (wrapper.wr_type) {
        "text" -> {
            val message = json.decodeFromJsonElement(TextMessage.serializer(),
                wrapper.data)
//            log.info("Получено текстовое сообщение (в обёртке): ${message.content}")
            log.info("Получено текстовое сообщение (в обёртке): $message")
            val response = ServerResponse(true, "Сообщение получено (обёртка): ${message.content}")
            // Преобразуем ServerResponse в JSON-строку и оборачиваем в Frame.Text
            val frame = Frame.Text(json.encodeToString(response))
            session.send(frame)
        }
        "command" -> {
            val command = json.decodeFromJsonElement(CommandMessage.serializer(),
                wrapper.data)
            log.info("Получена команда (в обёртке): $command")
            val response = processCommand(command)
            val frame = Frame.Text(json.encodeToString(response))
            session.send(frame)
        }
        "status" -> {
            val statusUpdate = json.decodeFromJsonElement(StatusUpdate.serializer(),
                wrapper.data)
            log.info("Статус пользователя обновлен (в обёртке): $statusUpdate")
            updateUserStatus(statusUpdate.userId ?: "unknown", statusUpdate.status)
            val response = ServerResponse(true, "Статус обновлен (обёртка)")
            val frame = Frame.Text(json.encodeToString(response))
            session.send(frame)
        }
        else -> {
            log.warn("Неизвестный тип сообщения в обёртке: ${wrapper.wr_type}")
            val errorResponse = ServerResponse(false, "Неизвестный тип сообщения в обёртке")
            val frame = Frame.Text(json.encodeToString(errorResponse))
            session.send(frame)
        }
    }
}

private fun processCommand(command: CommandMessage): ServerResponse {
    return when (command.command) {
        "start_game" -> ServerResponse(true, "Игра начата")
        "stop_game" -> ServerResponse(true, "Игра остановлена")
        else -> ServerResponse(false, "Неизвестная команда: ${command.command}")
    }
}

private fun updateUserStatus(userId: String, status: String) {
    log.info("У пользователя $userId теперь статус: '$status'")
}