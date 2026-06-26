package com.diary.app.biometric

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BiometricHelperSourceTest {

    @Test
    fun `pin data uses secure config store for hash salt and hint`() {
        val source = File("src/main/java/com/diary/app/biometric/BiometricHelper.kt").readText()

        assertTrue(source.contains("SecureConfigStore.setString(context, KEY_PIN_HASH, hash)"))
        assertTrue(source.contains("SecureConfigStore.setString(context, KEY_PIN_SALT, salt)"))
        assertTrue(source.contains("SecureConfigStore.setString(context, KEY_PIN_HINT, hint)"))
        assertTrue(source.contains("SecureConfigStore.getString(context, KEY_PIN_HASH)"))
        assertTrue(source.contains("SecureConfigStore.remove(context, KEY_PIN_HASH)"))
    }
}
