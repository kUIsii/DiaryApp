package com.diary.app.data.sync

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudSyncManagerTest {
    private val gson = Gson()

    @Test
    fun `auth response parsing`() {
        val json = """{"token":"abc123","message":"Login successful"}"""
        val parsed = gson.fromJson(json, SyncAuthResponse::class.java)
        assertEquals("abc123", parsed.token)
        assertEquals("Login successful", parsed.message)
    }

    @Test
    fun `auth response with error`() {
        val json = """{"error":"Wrong PIN"}"""
        val parsed = gson.fromJson(json, SyncAuthResponse::class.java)
        assertEquals("Wrong PIN", parsed.error)
        assertTrue(parsed.token.isNullOrBlank())
    }

    @Test
    fun `backup response parsing`() {
        val json = """{"message":"Backup saved"}"""
        val parsed = gson.fromJson(json, SyncBackupResponse::class.java)
        assertEquals("Backup saved", parsed.message)
    }

    @Test
    fun `desktop sync payload json structure`() {
        val payload = mapOf(
            "version" to 1,
            "syncMeta" to mapOf("deviceId" to "test-phone", "exportedAt" to System.currentTimeMillis()),
            "summary" to mapOf("total" to 3, "completed" to 1, "active" to 2),
            "tasks" to listOf(
                mapOf("id" to 1, "title" to "测试任务", "completed" to false, "priority" to 1)
            )
        )
        val json = gson.toJson(payload)
        assertNotNull(json)
        assertTrue(json.contains("test-phone"))
        assertTrue(json.contains("测试任务"))
    }
}
