package ru.smi_alexey.db.dao

//import ru.smi_alexey.db.MigrationUtils.runMigrations
import ru.smi_alexey.db.runMigrations
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
import org.junit.jupiter.api.AfterAll
import ru.smi_alexey.log.log
import java.time.Instant

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import ru.smi_alexey.db.MigrationUtils
import ru.smi_alexey.db.dao.GamerDao.createGamer
import ru.smi_alexey.db.dao.GamerDao.deleteAllGamers
import ru.smi_alexey.utils.date_time.toLocalString

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreateGamerTest {

    @BeforeAll
    fun setUp() {
        // Выполняется один раз перед всеми тестами
        runMigrations()
        deleteAllGamers()
    }

    @Test
    fun createGamer1() {
        val gamer: Gamer? = createGamer("bfx683", "butlerova13686", "smi_alexey@yandex.ru")
        log.debug("gamer.id = ${gamer?.id} gamer.createdAt = ${gamer?.createdAt?.toLocalString()}")
        assertEquals(Gamer(gamer?.id!!, "bfx683","butlerova13686",
            "smi_alexey@yandex.ru", gamer?.createdAt!!), gamer )
    }

    @AfterAll
    fun tearDown() {
        // Выполняется один раз после всех тестов
        MigrationUtils.shutdown()    }
}