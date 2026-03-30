package ru.smi_alexey

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SimpleTest {

    @Test
    fun testAddition() {
        val result = 2 + 2
        assertEquals(4, result)
    }

    @Test
    fun testString() {
        val message = "Hello, JUnit 5!"
        assertTrue(message.contains("JUnit"))
        assertFalse(message.contains("JUnit 4"))
    }

    @Test
    fun testNotNull() {
        val obj = "some value"
        assertNotNull(obj)
    }
}