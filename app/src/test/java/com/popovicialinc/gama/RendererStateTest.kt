package com.popovicialinc.gama

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for RendererState.wasRebootedInternal — the pure decision core of
 * offline reboot detection. All times are synthetic; no android.os calls.
 */
class RendererStateTest {

    private val switchWallClock = 1_700_000_000_000L
    // Device booted 1 h before the user switched to Vulkan
    private val bootTime = switchWallClock - 3_600_000L

    @Test
    fun `same boot detected via boot-time stamp`() {
        val currentBoot = bootTime + 5_000L // 5 s drift between the two reads
        val result = RendererState.wasRebootedInternal(
            storedBootTimeMs = bootTime,
            currentBootTimeMs = currentBoot,
            storedUptimeMs = 3_600_000L,
            currentUptimeMs = 26 * 3_600_000L, // uptime long since grew past stored
            storedSwitchWallClockMs = switchWallClock,
            nowWallClockMs = switchWallClock + 25 * 3_600_000L
        )
        assertFalse(result!!)
    }

    @Test
    fun `reboot detected via boot-time stamp even when new uptime exceeds stored`() {
        // The regression this fixes: previous session switched at uptime 1 h;
        // device rebooted; new session has been up 2 h. Uptime-only heuristics
        // see 2h >= 1h and wrongly report "same session".
        val rebootedBoot = bootTime + 30 * 24 * 3_600_000L // next boot a month later
        val result = RendererState.wasRebootedInternal(
            storedBootTimeMs = bootTime,
            currentBootTimeMs = rebootedBoot,
            storedUptimeMs = 3_600_000L,
            currentUptimeMs = 2 * 3_600_000L,
            storedSwitchWallClockMs = switchWallClock,
            nowWallClockMs = switchWallClock + 40 * 24 * 3_600_000L
        )
        assertTrue(result!!)
    }

    @Test
    fun `boot-time stamp within drift tolerance is same session`() {
        val result = RendererState.wasRebootedInternal(
            storedBootTimeMs = bootTime,
            currentBootTimeMs = bootTime + RendererState.BOOT_TIME_DRIFT_MS - 1,
            storedUptimeMs = 0L,
            currentUptimeMs = 10_000L,
            storedSwitchWallClockMs = switchWallClock,
            nowWallClockMs = switchWallClock + 10_000L
        )
        assertFalse(result!!)
    }

    @Test
    fun `boot-time stamp beyond drift tolerance counts as reboot`() {
        val result = RendererState.wasRebootedInternal(
            storedBootTimeMs = bootTime,
            currentBootTimeMs = bootTime + RendererState.BOOT_TIME_DRIFT_MS + 1,
            storedUptimeMs = 0L,
            currentUptimeMs = 10_000L,
            storedSwitchWallClockMs = switchWallClock,
            nowWallClockMs = switchWallClock + 10_000L
        )
        assertTrue(result!!)
    }

    @Test
    fun `legacy uptime path detects reboot when current uptime smaller`() {
        val result = RendererState.wasRebootedInternal(
            storedBootTimeMs = 0L, // old install — no boot stamp
            currentBootTimeMs = bootTime + 90_000L,
            storedUptimeMs = 3_600_000L,   // switched after 1 h up
            currentUptimeMs = 60_000L,     // now only 1 min since reboot
            storedSwitchWallClockMs = switchWallClock,
            nowWallClockMs = switchWallClock + 120_000L
        )
        assertTrue(result!!)
    }

    @Test
    fun `legacy wall-clock fallback trusts only recent switches`() {
        // Recent switch (<12 h) and boot began after the switch → rebooted
        val recent = RendererState.wasRebootedInternal(
            storedBootTimeMs = 0L,
            currentBootTimeMs = switchWallClock + 60_000L,
            storedUptimeMs = 0L,
            currentUptimeMs = 60_000L,
            storedSwitchWallClockMs = switchWallClock,
            nowWallClockMs = switchWallClock + 120_000L
        )
        assertTrue(recent!!)

        // Old switch (2 days) → unknown-era fallback must NOT claim a reboot
        // even though the boot time is newer than the switch.
        val stale = RendererState.wasRebootedInternal(
            storedBootTimeMs = 0L,
            currentBootTimeMs = switchWallClock + 60_000L,
            storedUptimeMs = 0L,
            currentUptimeMs = 60_000L,
            storedSwitchWallClockMs = switchWallClock,
            nowWallClockMs = switchWallClock + 48 * 3_600_000L
        )
        assertFalse(stale!!)
    }

    @Test
    fun `no stamps at all yields unknown`() {
        val result = RendererState.wasRebootedInternal(
            storedBootTimeMs = 0L,
            currentBootTimeMs = bootTime,
            storedUptimeMs = 0L,
            currentUptimeMs = 60_000L,
            storedSwitchWallClockMs = 0L,
            nowWallClockMs = switchWallClock
        )
        assertNull(result)
    }

    @Test
    fun `desired renderer migrates legacy state and remains independent from actual state`() {
        val prefs = FakePrefs()
        prefs.edit().putString(RendererState.PREF_LAST_RENDERER, RendererState.RENDERER_VULKAN).commit()

        // Existing installs only have last_renderer; it remains their desired
        // restore target when the actual renderer later becomes OpenGL at boot.
        assertEquals(RendererState.RENDERER_VULKAN, RendererState.getDesiredRenderer(prefs))

        prefs.edit()
            .putString(RendererState.PREF_LAST_RENDERER, RendererState.RENDERER_OPENGL)
            .putString(RendererState.PREF_DESIRED_RENDERER, RendererState.RENDERER_VULKAN)
            .commit()
        assertEquals(RendererState.RENDERER_OPENGL, RendererState.getRenderer(prefs))
        assertEquals(RendererState.RENDERER_VULKAN, RendererState.getDesiredRenderer(prefs))
    }
}
