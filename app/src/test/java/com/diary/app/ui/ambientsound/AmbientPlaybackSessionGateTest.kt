package com.diary.app.ui.ambientsound

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AmbientPlaybackSessionGateTest {

    @Test
    fun `stop callback dispatches by default`() {
        val gate = AmbientPlaybackSessionGate()

        assertTrue(gate.shouldDispatchStopCallback())
    }

    @Test
    fun `replacing session suppresses only the next stop callback`() {
        val gate = AmbientPlaybackSessionGate()

        gate.beginSessionReplacement()

        assertFalse(gate.shouldDispatchStopCallback())
        assertTrue(gate.shouldDispatchStopCallback())
    }
}
