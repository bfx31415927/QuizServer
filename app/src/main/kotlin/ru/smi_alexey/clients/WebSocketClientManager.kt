package ru.smi_alexey.clients

import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.close
import ru.smi_alexey.log.log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

//import kotlin.concurrent.atomics.AtomicLong

object WebSocketClientManager {
    private val clientIdGenerator = AtomicLong(1)

    // Все активные клиенты (по ID сессии)
    private val clients = ConcurrentHashMap<Long, WebSocketClient>()

    // Индекс для быстрого поиска по ID игрока из БД
    private val gamerIdToClientId = ConcurrentHashMap<Long, Long>()

    // Индекс для быстрого поиска по login
    private val loginToClientId = ConcurrentHashMap<String, Long>()

    /**
     * Добавление нового клиента
     */
    fun addClient(session: DefaultWebSocketServerSession): WebSocketClient {
        val clientId = clientIdGenerator.getAndIncrement()
        val client = WebSocketClient(id = clientId, session = session)
        clients[client.id] = client

        log.info("Клиент добавлен. ID: ${client.id}, Всего клиентов: ${clients.size}")
        return client
    }

    /**
     * Удаление клиента
     */
    fun removeClient(clientId: Long) {
        val client = clients.remove(clientId) ?: return
        client.gamer?.let { gamer ->
            gamerIdToClientId.remove(gamer.id)
            loginToClientId.remove(gamer.login)
            log.info("Пользователь ${gamer.login} отключен")
        }
        log.info("Клиент c ID: $clientId удалён, всего клиентов: ${clients.size}")
    }

    /**
     * Авторизация клиента (упрощённая версия с remove)
     */
    suspend fun authenticateClient(clientId: Long, login: String, password: String): Boolean {
        val client = clients[clientId]
        if(client == null) {
            log.error("Клиент id=$clientId отсутствует в clients")
            return false
        }

        val success = client.authenticate(login, password)
        if (!success){
            log.error("Неверно указаны логин '$login' и/или пароль '$password' клиента с id=$clientId")
            return false
        }

        val gamer = client.gamer

        // Сначала удаляем старого клиента, если он есть
        val existingClientId = gamerIdToClientId.remove(gamer?.id)

        if (existingClientId != null && existingClientId != clientId) {
            val oldClient = clients[existingClientId]
            if (oldClient?.isActive() == true) {
                log.warn("Пользователь ${gamer?.login} уже подключен. Завершаем старое соединение")
                try {
                    oldClient.sendMessage("""{"type":"warning","message":"Вы вошли с другого устройства"}""")
                    oldClient.session.close()
                } catch (e: Exception) {
                    // Игнорируем ошибки при отключении
                }
            }
            // Удаляем из индекса логинов
            oldClient?.gamer?.login?.let { loginToClientId.remove(it, existingClientId) }
        }

        // Теперь регистрируем нового клиента
        gamerIdToClientId[gamer?.id!!] = clientId
        loginToClientId[gamer.login] = clientId

        log.info("Клиент c clientId = $clientId авторизован как '${gamer.login}'")
        return true
    }

    /**
     * Регистрация нового пользователя
     */
    fun registerClient(clientId: Long, login: String, password: String, email: String): Boolean {
        val client = clients[clientId]
        if(client == null) {
            log.error("Клиент id=$clientId отсутствует в clients")
            return false
        }

        val success = client.register(login, password, email)

        if (success) {
            val gamer = client.gamer!!  // безопасно, т.к. register установил gamer
            gamerIdToClientId[gamer.id] = clientId
            loginToClientId[gamer.login] = clientId
            return true
        }

        log.error("Игрок c логином: '$login' уже есть в БД")
        return false
    }

    /**
     * Получение клиента по ID пользователя
     */
    fun getClientByGamerId(gamerId: Long): WebSocketClient? {
        val clientId = gamerIdToClientId[gamerId] ?: return null
        return clients[clientId]?.takeIf { it.isActive() }
    }

    /**
     * Получение клиента по login
     */
    fun getClientByLogin(login: String): WebSocketClient? {
        val clientId = loginToClientId[login] ?: return null
        return clients[clientId]?.takeIf { it.isActive() }
    }

    /**
     * Получение клиента по ID сессии
     */
    fun getClientById(clientId: Long): WebSocketClient? = clients[clientId]

    /**
     * Получение всех аутентифицированных клиентов
     */
    fun getAuthenticatedClients(): List<WebSocketClient> {
        return clients.values.filter { it.isAuthenticated && it.isActive() }
    }

    /**
     * Получение всех клиентов
     */
    fun getAllClients(): List<WebSocketClient> = clients.values.filter { it.isActive() }

    /**
     * Отправка сообщения всем аутентифицированным клиентам
     */
    suspend fun broadcastToAuthenticated(message: String, excludeClientId: Long? = null) {
        val authenticatedClients = getAuthenticatedClients()
        var failedCount = 0

        authenticatedClients.forEach { client ->
            if (excludeClientId != client.id) {
                try {
                    client.sendMessage(message)
                } catch (e: Exception) {
                    failedCount++
                    log.warn("Не удалось отправить сообщение клиенту ${client.id}: ${e.message}")
                }
            }
        }

        if (failedCount > 0) {
            log.error("[broadcastToAuthenticated] Отправил не всем нужным клиентам: ${authenticatedClients.size - failedCount}/${authenticatedClients.size}")
        } else {
            log.warn("[broadcastToAuthenticated] Отправил всем нужным клиентам")
        }
    }

    /**
     * Отправка сообщения конкретному пользователю по ID
     */
    suspend fun sendToUser(gamerId: Long, message: String): Boolean {
        val client = getClientByGamerId(gamerId)
        return if (client != null) {
            client.sendMessage(message)
            true
        } else {
            log.error("sendToUser($gamerId,$message) client == null")
            false
        }
    }

    /**
     * Отправка сообщения конкретному пользователю по login
     */
    suspend fun sendToLogin(login: String, message: String): Boolean {
        val client = getClientByLogin(login)
        return if (client != null) {
            client.sendMessage(message)
            true
        } else {
            log.error("sendToLogin($login,$message) client == null")
            false
        }
    }

    /**
     * Очистка неактивных клиентов
     */
    fun cleanupInactiveClients() {
        var removedCount = 0

        clients.values.forEach { client ->
            if (!client.isActive()) {
                val removed = clients.remove(client.id, client)
                if (removed) {
                    removedCount++
                    client.gamer?.let { gamer ->
                        gamerIdToClientId.remove(gamer.id, client.id)
                        loginToClientId.remove(gamer.login, client.id)
                        log.info("Очищен неактивный клиент: login=${gamer.login} (client.id=${client.id})")
                    }
                }
            }
        }

        if (removedCount > 0) {
            log.info("Очищено $removedCount неактивных клиентов")
        }
    }

    /**
     * Получение статистики
     */
    fun getStats(): Map<String, Int> {
        val all = getAllClients()
        val authenticated = getAuthenticatedClients()

        return mapOf(
            "total_connections" to all.size,
            "authenticated_users" to authenticated.size,
            "anonymous" to (all.size - authenticated.size)
        )
    }
}