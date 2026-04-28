package ru.smi_alexey.db.dao

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import ru.smi_alexey.log.log
import java.time.Instant

/*
    код object Gamers содержит неявно:
    val id = long("id").autoIncrement() // BIGSERIAL → Long
    override val primaryKey = PrimaryKey(id)
*/
object Gamers : LongIdTable() {
    val login = text("login")
    val password = text("password")
    val email = text("email").nullable()
    /*
        Время будет ставиться только в БД.
        В Kotlin можно не передавать createdAt при вставке.
     */
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
}

data class Gamer(
    val id: Long,
    val login: String,
    val password: String,
    val email: String?,
    val createdAt: Instant
)

object Restore_passwords : Table() {
    val login = text("login")
    val code = integer("code")
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
}

data class Restore_password(
    val login: String,
    val code: Int,
    val createdAt: Instant
)

// DAO с методами для работы с БД
object GamerDao {
    //добавление игрока в таблицу gamers
    fun addGamer(login: String, password: String, email: String?): Boolean{
        return transaction {
            // Проверяем, существует ли пользователь с таким login
            val existing = Gamers.select {Gamers.login eq login}.firstOrNull()

            if (existing != null) {
                log.warn("[GamerDao.addGamer()] Попытка создать существующего пользователя: $login")
                return@transaction false
            }

            Gamers.insert {
                it[Gamers.login] = login
                it[Gamers.password] = password  // TODO: хешировать пароль!
                it[Gamers.email] = email
                // createdAt передастся по времени БД
            }
            return@transaction true
        }
    }

    fun createGamer(login: String, password: String, email: String?): Gamer? {
        return transaction {
            // Проверяем, существует ли пользователь с таким login
            val existing = Gamers.select {Gamers.login eq login}.firstOrNull()

            if (existing != null) {
                log.warn("Попытка создать существующего пользователя: $login")
                return@transaction null
            }

            val id = Gamers.insertAndGetId {
                it[Gamers.login] = login
                it[Gamers.password] = password  // TODO: хешировать пароль!
                it[Gamers.email] = email
                // createdAt передастся по времени БД
            }.value

            getGamerById(id)
        }
    }

    //функция на всякий случай
    fun createGamerWithId(id: Long, login: String, password: String, email: String?): Gamer? {
        return transaction {
            // Проверяем, нет ли уже такого id
            if (Gamers.select { Gamers.id eq id }.singleOrNull() != null) {
                log.warn("Попытка вставить игрока с уже существующим id = $id")
                return@transaction null
            }

            // Вставляем с явным id
            Gamers.insert { row ->
                row[Gamers.id] = id
                row[Gamers.login] = login
                row[Gamers.password] = password
                row[Gamers.email] = email
                // createdAt передастся по времени БД
            }

            getGamerById(id)
        }
    }

    fun getGamerById(id: Long): Gamer? {
        return transaction {
            Gamers.select { Gamers.id eq id }
                .map { rowToGamer(it) }
                .singleOrNull()
        }
    }

    fun getGamerByLogin(login: String): Gamer? {
        return transaction {
            Gamers.select { Gamers.login eq login }
                .map { rowToGamer(it) }
                .singleOrNull()
        }
    }

    fun getGamerByEmail(email: String): Gamer? {
        return transaction {
            Gamers.select { Gamers.email eq email }
                .map { rowToGamer(it) }
                .singleOrNull()
        }
    }

    fun authenticate(login: String, password: String): Gamer? {
        return transaction {
            Gamers.select {
                (Gamers.login eq login) and (Gamers.password eq password)
            }.map { rowToGamer(it) }
                .singleOrNull()
        }
    }

    fun updatePassword(login: String, newPassword: String): Boolean {
        return transaction {
            Gamers.update({ Gamers.login eq login }) {
                it[password] = newPassword  // TODO: хешировать!
            } > 0
        }
    }

    fun deleteGamerPerID(gamerId: Long): Boolean {
        return transaction {
            Gamers.deleteWhere { Gamers.id eq gamerId } > 0
        }
    }



    fun deleteGamerPerLogin(login: String): Boolean {
        return transaction {
            Gamers.deleteWhere { Gamers.login eq login } > 0
        }
    }

    fun deleteAllGamers(): Boolean {
        log.debug("deleteAllGamers()")
        return transaction {
            Gamers.deleteAll() > 0  // true если были удалены записи
        }
    }

    private fun rowToGamer(row: ResultRow): Gamer {
        return Gamer(
            id = row[Gamers.id].value, // ✅ Извлекаем Long из EntityID<Long>
            login = row[Gamers.login],
            password = row[Gamers.password],
            email = row[Gamers.email],
            createdAt = row[Gamers.createdAt]
        )
    }

    fun getGamersRowsCount(): Long {
        return transaction {
            var count = 0L
            exec("SELECT COUNT(*) FROM gamers") { rs ->
                if (rs.next()) {
                    count = rs.getLong(1)
                }
            }
            count
        }
    }

    //добавление или модификация строки в таблице restore_passwords
    fun addRowtoRP(login: String, code: Int): Boolean{
        return transaction {
            // Проверяем, существует ли пользователь с таким login в таблице gamers
            val b1 = Gamers.select {Gamers.login eq login}.firstOrNull()

            if (b1 == null) {
                log.warn("[GamerDao.addRowToRP()] Пользователь с логином: '$login'" +
                        " отсутствует в таблице gamers! ")
                return@transaction false
            }

            // Проверяем, существует ли пользователь с таким login в таблице restore_passwords
            val b2 = Restore_passwords.select {Restore_passwords.login eq login}.firstOrNull()
            if (b2 != null) {
                log.warn("[GamerDao.addRowToRP()] Пользователь с логином: '$login'" +
                        " есть в таблице 'restore_passwords'! ")
                //удалим его из таблицы Restore_passwords
                Restore_passwords.deleteWhere { Restore_passwords.login eq login }
            }

            Restore_passwords.insert {
                it[Restore_passwords.login] = login
                it[Restore_passwords.code] = code
                // createdAt передастся по времени БД
            }

            return@transaction true
        }
    }

    //Замена пароля игроку
    fun changePassword(login: String, newPassword: String, code: Int): Boolean {
        return transaction {
            val row = Restore_passwords.select { Restore_passwords.login eq login }.firstOrNull()
            if (row != null && row[Restore_passwords.code] == code) {
                // Проверка времени (например, 3 часа = 10800000 мс)
                val elapsed = System.currentTimeMillis() - row[Restore_passwords.createdAt].toEpochMilli()
                if (elapsed <= 3 * 60 * 60 * 1000L) {
                    GamerDao.updatePassword(login, newPassword)
                    // Удаляем запись восстановления
                    Restore_passwords.deleteWhere { Restore_passwords.login eq login }
                    return@transaction true
                }
            }
            return@transaction false
        }
    }
}