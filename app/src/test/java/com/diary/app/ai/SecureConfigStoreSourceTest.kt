package com.diary.app.ai

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SecureConfigStoreSourceTest {

    @Test
    fun `ai config store is backed by secure config storage`() {
        val aiConfigStore = File("src/main/java/com/diary/app/ai/AiConfigStore.kt").readText()
        val secureStore = File("src/main/java/com/diary/app/security/SecureConfigStore.kt").readText()

        assertTrue(aiConfigStore.contains("SecureConfigStore"))
        assertTrue(aiConfigStore.contains("getString(context, secureApiKeyKey(providerId))"))
        assertTrue(aiConfigStore.contains("setString(context, secureApiKeyKey(providerId), key)"))
        assertTrue(secureStore.contains("AndroidKeyStore"))
        assertTrue(secureStore.contains("AES/GCM/NoPadding"))
    }
}
