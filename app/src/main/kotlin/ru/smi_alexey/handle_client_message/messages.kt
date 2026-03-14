package ru.smi_alexey.handle_client_message

import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import ru.smi_alexey.log.log
import ru.smi_alexey.serialization.ClientResponse
import ru.smi_alexey.serialization.CommandMessage
import ru.smi_alexey.serialization.MessageWrapper
import ru.smi_alexey.serialization.ServerResponse
import ru.smi_alexey.serialization.StatusUpdate
import ru.smi_alexey.serialization.TextMessage
import ru.smi_alexey.serialization.WebSocketMessage
import ru.smi_alexey.serialization.json
import kotlinx.serialization.serializer
import kotlinx.serialization.json.jsonObject


// Обработка прямого экземпляра sealed-класса
suspend fun handleWebSocketMessage(
    session: DefaultWebSocketServerSession,
    message: WebSocketMessage
) {
    when (message) {
        is TextMessage -> {
            val mess = "Получено сообщение TextMessage: $message"
            log.info(mess)
            sendDirectMessage(session,
                ServerResponse( success = true, message = mess )
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
            sendDirectMessage(session,
                ServerResponse(success = true, message = mess))
        }
        is ClientResponse -> {
            log.info("Получено сообщение ClientResponse: $message")
        }
        is ServerResponse -> {
            val mess = "Получено сообщение ServerResponse: $message"
            log.error(mess)
            sendDirectMessage(session,
                ServerResponse(success = false,  message = mess))
        }
    }
}

suspend fun handleWrapperMessage(
    session: DefaultWebSocketServerSession,
    wrapper: MessageWrapper
) {
    when (wrapper.wr_type) {
        "text" -> {
            val message = json.decodeFromJsonElement(TextMessage.serializer(),
                wrapper.data)
            val mess = "Получено сообщение TextMessage (в обёртке): $message"
            log.info(mess)
            sendWrapperMessage(session,ServerResponse(success = true,message = mess)
            )
        }
        "command" -> {
            val command = json.decodeFromJsonElement(CommandMessage.serializer(),
                wrapper.data)
            log.info("Получена команда (в обёртке): $command")
            sendWrapperMessage(session, processCommand(command))
        }
        "status" -> {
            val statusUpdate = json.decodeFromJsonElement(StatusUpdate.serializer(),
                wrapper.data)
            val mess = "Статус пользователя обновлен (в обёртке): $statusUpdate"
            log.info(mess)
            updateUserStatus(statusUpdate.userId ?: "unknown", statusUpdate.status)
            sendWrapperMessage(session,ServerResponse(success = true, message = mess))

        }
        "client_response" -> {
            val clientResponse = json.decodeFromJsonElement(ClientResponse.serializer(),
                wrapper.data)
            log.info("Получен clientResponse (в обёртке): $clientResponse")
        }
        else -> {
            val mess = "Получен неизвестный/неверный тип сообщения в обёртке: ${wrapper.wr_type}"
            log.error(mess)
            sendWrapperMessage(session,ServerResponse(success = false, message = mess))
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

inline suspend fun <reified T : WebSocketMessage> sendWrapperMessage(
    session: DefaultWebSocketServerSession,
    message: T
) {
    try {
        val data = json.encodeToJsonElement(serializer<T>(), message).jsonObject
        val wrapper = MessageWrapper(
            wr_type = message._type,
            version = "1.0",
            data = data
        )
        log.info("sendWrapperMessage готовит к отправке сообщение: $wrapper")
        val jsonString = json.encodeToString(wrapper)
        val frame = Frame.Text(jsonString)
        session.send(frame)
        log.info("sendWrapperMessage отправил сообщение: $jsonString")
    } catch (e: Exception) {
        log.error( "Ошибка в sendWrapperMessage: ${e.message}")
    }
}

inline suspend fun <reified T : WebSocketMessage> sendDirectMessage(
    session: DefaultWebSocketServerSession,
    message: T
) {
    try {
        val jsonString = json.encodeToString(WebSocketMessage.serializer(),
            message)

        val frame = Frame.Text(jsonString)
        session.send(frame)

        log.info("sendDirectMessage отправил сообщение: $jsonString")
    } catch (e: Exception) {
        log.error("Ошибка в sendDirectMessage: ${e.message}")
    }
}