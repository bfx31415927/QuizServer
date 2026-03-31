package ru.smi_alexey.db.dao

//import ru.smi_alexey.db.MigrationUtils.runMigrations

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import ru.smi_alexey.db.MigrationUtils
import ru.smi_alexey.db.dao.GamerDao.authenticate
import ru.smi_alexey.db.dao.GamerDao.createGamer
import ru.smi_alexey.db.dao.GamerDao.createGamerWithId
import ru.smi_alexey.db.dao.GamerDao.deleteAllGamers
import ru.smi_alexey.db.dao.GamerDao.deleteGamerPerID
import ru.smi_alexey.db.dao.GamerDao.deleteGamerPerLogin
import ru.smi_alexey.db.dao.GamerDao.getGamerByEmail
import ru.smi_alexey.db.dao.GamerDao.getGamerById
import ru.smi_alexey.db.dao.GamerDao.getGamerByLogin
import ru.smi_alexey.db.dao.GamerDao.getGamersRowsCount
import ru.smi_alexey.db.dao.GamerDao.updatePassword
import ru.smi_alexey.db.runMigrations
import ru.smi_alexey.log.log
import ru.smi_alexey.utils.date_time.toLocalString

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GamersDaoTest {

    @BeforeAll
    fun beforeAll() {
        // Выполняется один раз перед всеми тестами
        runMigrations()
    }

    @BeforeEach
    fun beforeEach() {
        // Выполняется один раз перед всеми тестами
        deleteAllGamers()
    }

    @Test
    //Вставка одного конкретного игрока (id по автоинкременту)
    fun insertOneGamerWithoudID_Test() {
        log.debug("insertOneGamerWithoudID_Test()[BEGIN]")
        val gamer: Gamer? = createGamer("bfx683", "butlerova13686", "smi_alexey@yandex.ru")
        log.debug("gamer.id = ${gamer?.id} gamer.createdAt = ${gamer?.createdAt?.toLocalString()}")
        assertEquals(
            Gamer(
                gamer?.id!!, "bfx683", "butlerova13686",
                "smi_alexey@yandex.ru", gamer?.createdAt!!
            ), gamer
        )
        log.debug("insertOneGamerWithoudID_Test()[END]")
    }

    @Test
    //Вставка одного конкретного игрока (c id)
    fun insertOneGamerWithID_Test() {
        log.debug("insertOneGamerWithID_Test()[BEGIN]")
        val gamer: Gamer? = createGamerWithId(99, "bfx683", "butlerova13686", null)
        log.debug("gamer.id = ${gamer?.id} gamer.createdAt = ${gamer?.createdAt?.toLocalString()}")
        assertEquals(
            Gamer(
                99, "bfx683", "butlerova13686",
                null, gamer?.createdAt!!
            ), gamer
        )
        log.debug("insertOneGamerWithID_Test()[END]")
    }

    @Test
    //Вставка 1000 игроков (id по автоинкременту) (На моем компе выполняется меньше секунды)
    fun insertManyGamerWithoudID_Test() {
        log.debug("insertManyGamerWithoudID_Test() [BEGIN]")
        insertGamers(1000)
        log.debug("insertManyGamerWithoudID_Test() [END]")
    }

    private fun insertGamers(count: Int) {
        for (i in 1..count) {
            var login = "login$i"
            var password = "password$i"
            var email = "email$i"
            createGamer(login, password, email)
        }
    }

    @Test
    //Возвратить игрока по его логину
    fun getGamerByLogin_Test() {
        log.debug("getGamerByLogin_Test() [BEGIN]")
        insertManyGamerWithoudID_Test()
        val gamer: Gamer? = getGamerByLogin("login100")
        assertEquals("login100", gamer?.login)
        assertEquals("password100", gamer?.password)
        log.debug("getGamerByLogin_Test() [END]")
    }

    @Test
    //Возвратить игрока по его email
    fun getGamerByEmail_Test() {
        log.debug("getGamerByEmail_Test() [BEGIN]")
        insertManyGamerWithoudID_Test()
        val gamer: Gamer? = getGamerByEmail("email100")
        assertEquals("login100", gamer?.login)
        assertEquals("password100", gamer?.password)
        assertEquals("email100", gamer?.email)
        log.debug("getGamerByEmail_Test() [END]")
    }

    @Test
    //Проверить авторизацию  игрока по его логину и паролю
    fun authenticate_Test() {
        log.debug("authenticate_Test() [BEGIN]")
        insertManyGamerWithoudID_Test()
        val gamer: Gamer? = authenticate("login100", "password100")
        assertEquals("login100", gamer?.login)
        assertEquals("password100", gamer?.password)
        assertEquals("email100", gamer?.email)
        log.debug("authenticate_Test() [END]")
    }

    @Test
    //Проверка функции updatePassword(...)
    fun updatePassword_Test() {
        log.debug("updatePassword_Test() [BEGIN]")
        insertManyGamerWithoudID_Test()
        val b = updatePassword("login100", "new_password100")
        assertEquals(true, b)
        log.debug("updatePassword_Test() [END]")
    }

    @Test
    //Проверка функции deleteGamerPerID(...)
    fun deleteGamerPerID_Test() {
        log.debug("deleteGamerPerID_Test() [BEGIN]")
        insertManyGamerWithoudID_Test()
        val gamer: Gamer? = getGamerByLogin("login100")
        val b = deleteGamerPerID(gamer?.id!!)
        assertEquals(true, b)
        val count = getGamersRowsCount()
        assertEquals(999, count)
        assertEquals(null, getGamerById(gamer?.id!!))
        log.debug("deleteGamerPerID_Test() [END]")
    }

    //Проверка функции deleteGamerPerLogin(...)
    fun deleteGamerPerLogin_Test() {
        log.debug("deleteGamerPerLogin_Test() [BEGIN]")
        insertManyGamerWithoudID_Test()
        val b = deleteGamerPerLogin("login100")
        assertEquals(true, b)
        val count = getGamersRowsCount()
        assertEquals(999, count)
        assertEquals(null, getGamerByLogin("login100"))
        log.debug("deleteGamerPerLogin_Test() [END]")
    }

    @AfterAll
    fun tearDown() {
        // Выполняется один раз после всех тестов
        MigrationUtils.shutdown()
    }
}