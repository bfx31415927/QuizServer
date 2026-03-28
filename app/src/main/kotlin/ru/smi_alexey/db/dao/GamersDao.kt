package ru.smi_alexey.db.dao

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
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
    val email = text("email")
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
    val email: String,
    val createdAt: Instant
)

// DAO с методами для работы с БД
object GamerDao {

    fun createGamer(login: String, password: String, email: String): Gamer? {
        return transaction {
            // Проверяем, существует ли пользователь с таким login или email
            val existing = Gamers.select {
                (Gamers.login eq login) or (Gamers.email eq email)
            }.firstOrNull()

            if (existing != null) {
                log.warn("Попытка создать существующего пользователя: $login / $email")
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
    fun createGamerWithId(id: Long, login: String, password: String, email: String): Gamer? {
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

    fun updatePassword(gamerId: Long, newPassword: String): Boolean {
        return transaction {
            Gamers.update({ Gamers.id eq gamerId }) {
                it[password] = newPassword  // TODO: хешировать!
            } > 0
        }
    }

    fun deleteGamer(gamerId: Long): Boolean {
        return transaction {
            Gamers.deleteWhere { Gamers.id eq gamerId } > 0
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
}