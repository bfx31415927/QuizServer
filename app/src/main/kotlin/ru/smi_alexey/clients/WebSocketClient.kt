package ru.smi_alexey.clients

import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.smi_alexey.db.dao.Gamer
import ru.smi_alexey.db.dao.GamerDao
import ru.smi_alexey.email.sendEmail
import ru.smi_alexey.log.log
import ru.smi_alexey.serialization.WebSocketMessage
import ru.smi_alexey.serialization.json

class WebSocketClient(
    val id: Long,  // ID клиента (уникальный для сессии)
    val session: DefaultWebSocketServerSession,
    val connectedAt: Long = System.currentTimeMillis()
) {
    var gamer: Gamer? = null  // Привязанный пользователь из БД
        private set

    val isAuthenticated: Boolean
        get() = gamer != null

    val gamerId: Long?
        get() = gamer?.id

    val login: String?
        get() = gamer?.login

    fun authenticate(login: String, password: String): Boolean {
        val authenticatedGamer = GamerDao.authenticate(login, password)

        if (authenticatedGamer != null) {
            this.gamer = authenticatedGamer
            log.info("[WebSocketClient.authenticate] Клиент id = $id авторизован под логином '$login' и паролем: '$password' " +
                    "(gamerId: ${authenticatedGamer.id})")
            return true
        }

        log.error("[WebSocketClient.authenticate] Неудачная попытка авторизации клиента id = $id под логином: '$login' и  паролем '$password'")
        return false
    }

    fun register(login: String, password: String, email: String): Boolean {
        val b = GamerDao.addGamer(login, password, email)
        if (b) {
            log.info("Клиент id = $id зарегистрирован под логином: '$login'")
            return true
        }
        return false
    }

    suspend inline fun <reified T : WebSocketMessage> sendMessage(message: T) {
        try {
            val jsonString = json.encodeToString(
                WebSocketMessage.serializer(),
                message
            )

            val frame = Frame.Text(jsonString)
            session.send(frame)

            log.info("sendMessage отправил сообщение: $jsonString")
        } catch (e: Exception) {
            log.error("Ошибка в sendMessage: message = '$message'  клиенту id = $id с логином: '${gamer?.login}' [${e.message}]")
        }
    }

    fun isActive(): Boolean = session.isActive

    override fun toString(): String = "WebSocketClient(id=$id, gamer=${gamer?.login ?: "unauthenticated"})"
}