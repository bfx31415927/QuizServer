package ru.smi_alexey.clients

import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import kotlinx.coroutines.isActive
import ru.smi_alexey.db.dao.Gamer
import ru.smi_alexey.db.dao.GamerDao
import ru.smi_alexey.log.log

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
            log.info("Клиент id = $id авторизован как '${authenticatedGamer.login}' (gamerId: ${authenticatedGamer.id})")
            return true
        }

        log.warn("Неудачная попытка авторизации клиента id = $id под логином: '$login' и  паролем '$password'")
        return false
    }

    fun register(login: String, password: String, email: String): Boolean {
        val newGamer = GamerDao.createGamer(login, password, email)

        if (newGamer != null) {
            this.gamer = newGamer
            log.info("Клиент id = $id зарегистрирован под логином: '${newGamer.login}' (gamerId: ${newGamer.id})")
            return true
        }

        return false
    }

    suspend fun sendMessage(message: String) {
        try {
            session.send(Frame.Text(message))
        } catch (e: Exception) {
            log.error("Ошибка отправки сообщения '$message'  клиенту id = $id с логином: '${gamer?.login}' [${e.message}]")
        }
    }

    fun isActive(): Boolean = session.isActive

    override fun toString(): String = "WebSocketClient(id=$id, gamer=${gamer?.login ?: "unauthenticated"})"
}