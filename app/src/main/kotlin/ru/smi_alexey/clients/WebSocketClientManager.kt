package ru.smi_alexey.clients

import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.close
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.smi_alexey.clients.WebSocketClientManager.gamerIdToClientId
import ru.smi_alexey.db.dao.GamerDao
import ru.smi_alexey.email.sendEmail
import ru.smi_alexey.log.log
import ru.smi_alexey.serialization.ServerResponse
import ru.smi_alexey.serialization.ServerResponseWithDetails
import ru.smi_alexey.serialization.WebSocketMessage
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.collections.get
import kotlin.random.Random

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
     * Авторизация клиента
     */
    suspend fun authenticateClient(clientId: Long, login: String, password: String) {
        val client = clients[clientId]

        val success = client?.authenticate(login, password)!!
        if (!success){
            log.error("[authenticateClient] Неверно указаны логин '$login' и/или пароль '$password' клиента с id=$clientId")
        } else {
            log.info("[authenticateClient] Логин '$login' и пароль '$password' клиента с id=$clientId указаны верно!")
            val gamer = client.gamer
            // Проверяем, авторизован ли уже этот пользователь
            val existingClientId = gamerIdToClientId[gamer?.id]
            if (existingClientId != null) {
                if (existingClientId != clientId) { //пользователь уже авторизован на другом устройстве
                    log.info("[authenticateClient] Пользователь уже авторизован на другом устройстве!" +
                            " (existingClientId: $existingClientId, clientId: $clientId)")
                    client.sendMessage(ServerResponse(success = false, message = "login_repeated"))
                    return
                } else {//пользователь уже авторизован на этом устройстве
                    log.info("[authenticateClient] Пользователь уже авторизован на этом устройстве!" +
                            " (clientId: $clientId)")
                    client.sendMessage(ServerResponse(success = true, message = "login_yet"))
                    return
                }
            } else { //existingClientId == null (пользователь еще не авторизован на этом устройстве)
                // Авторизуем нового клиента
                gamerIdToClientId[gamer?.id!!] = clientId
                loginToClientId[gamer.login] = clientId
                log.info("[authenticateClient] Клиент c clientId = $clientId успешно авторизован с login: '${gamer.login}'")
            }
        }
        client.sendMessage(ServerResponse(success=success, message="login"))
    }

    /**
     * Регистрация нового пользователя
     */
    suspend fun registerClient(clientId: Long, login: String, password: String, email: String) {
        val client = clients[clientId] ?: return

        val success = client?.register(login, password, email)

        if (success!!) {
            log.info("[registerClient] Новый клиент c client.id = ${client.id} " +
                    "успешно зарегистрирован с логином '$login' и паролем '$password'")
        } else {
            log.error("[registerClient] Игрок c логином: '$login' уже есть в БД!")
        }
        client.sendMessage(ServerResponse(success=success, message="register"))
    }

    /**
     * Посылка email пользователю, забывшему пароль
     */
    suspend fun sendEmailToClient(clientId: Long, login: String, email: String) {
        val client = clients[clientId] ?: return

        val code = Random.nextInt(1, 2147483647)
        val success = GamerDao.addRowtoRP(login, code)

        if (success) {
            log.info("[sendEmailToClient] Для клиента c логином: '$login' " +
                    "успешно добавлена строка в 'restore_passwords'")
                //Готовим данные для email и затем отправляем его
            val subject = "Сброс пароля!"
            val htmlContent = """
                <h1>Сброс пароля</h1>
                Уважаемый(ая) $login!
                <p>Мы получили запрос на смену пароля.</p>
                <p>Для продолжения необходимо данный код:</p> 
                <p><h1>${code}</h1></p>
                ввести в диалоге [Восстановление пароля (Второй этап)].</p>
                <p>Диалог нужно вызвать из меню "Восстановление пароля"
                экрана авторизации программы QuizGamer,
                нажав на три точки в правом верхнем углу экрана.</p>
                <p>Код будет действителен в течение трёх часов.</p>
                <p>Если Вы не отправляли такой запрос,
                проигнорируйте это письмо.</p>
            """.trimIndent()

            // НЕ ждём результат, отправляем в фоне
            CoroutineScope(Dispatchers.IO).launch {
                sendEmail(email, subject, htmlContent)
                log.error("[sendEmailToClient] Письмо отправлено клиенту: '$login'")
            }
        } else {
            log.error("[sendEmailToClient] Логин: '$login' отсутствует в БД!")
        }

        client.sendMessage(ServerResponseWithDetails(success = success,
            details = "", message = "email"))

        //toDo: Пока непонятно, как поступать с ошибкой отправки email
    }
        /**
         * Замена пароля
         */
        suspend fun changePassword(clientId: Long, login: String, password: String, code: String) {
            val client = clients[clientId] ?: return

            val success = GamerDao.changePassword(login, password, code.toInt())

            if (success) {
                log.info("[changePassword] Для клиента c логином: '$login' " +
                        "пароль успешно изменён!")
            } else {
                log.error("[changePassword] Для клиента c логином: '$login' " +
                        "пароль не был изменён по одной из известных причин!")
            }

        client.sendMessage(ServerResponseWithDetails(success = success,
            details = "", message = "change_password"))

        //toDo: Пока непонятно, как поступать с ошибкой отправки email
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
    suspend inline fun <reified T : WebSocketMessage> broadcastToAuthenticated(message: T, excludeClientId: Long? = null) {
        val authenticatedClients = getAuthenticatedClients()
        var failedCount = 0

        authenticatedClients.forEach { client ->
            if (excludeClientId != client.id) {
                try {
                    client.sendMessage(message)
                } catch (e: Exception) {
                    failedCount++
                    log.warn("[broadcastToAuthenticated] Не удалось отправить сообщение = $message клиенту ${client.id}: ${e.message}")
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
    suspend inline fun <reified T : WebSocketMessage> sendToUser(gamerId: Long, message: T): Boolean {
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
    suspend inline fun <reified T : WebSocketMessage> sendToLogin(login: String, message: T): Boolean {
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