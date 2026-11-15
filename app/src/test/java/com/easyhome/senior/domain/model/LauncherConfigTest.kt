package com.easyhome.senior.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherConfigTest {

    private fun buttons(n: Int) = (1..n).map { HomeButton("$it", ActionType.APP, "APP $it", 0xFF000000, packageName = "p$it") }

    @Test
    fun `nessun limite pratico di 4 pulsanti`() {
        val config = LauncherConfig(buttons = buttons(12)).sanitized()
        assertEquals(12, config.buttons.size)
        assertFalse(config.isFull)
    }

    @Test
    fun `calcolo delle schermate per lista e griglia`() {
        assertEquals(1, LauncherConfig(buttons = emptyList()).pageCount)
        assertEquals(1, LauncherConfig(buttons = buttons(4), columns = 1, rowsPerPage = 4).pageCount)
        assertEquals(2, LauncherConfig(buttons = buttons(5), columns = 1, rowsPerPage = 4).pageCount)
        assertEquals(1, LauncherConfig(buttons = buttons(6), columns = 2, rowsPerPage = 3).pageCount)
        assertEquals(2, LauncherConfig(buttons = buttons(10), columns = 3, rowsPerPage = 3).pageCount)
    }

    @Test
    fun `il colore successivo non ripete quelli gia usati finche possibile`() {
        val used = ButtonPalette.colors.take(3).mapIndexed { i, (_, c) ->
            HomeButton("$i", ActionType.APP, "X", c, packageName = "p")
        }
        assertTrue(ButtonPalette.next(used) !in used.map { it.colorArgb })
    }
}
