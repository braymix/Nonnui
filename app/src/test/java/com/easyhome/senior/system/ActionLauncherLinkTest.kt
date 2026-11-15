package com.easyhome.senior.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActionLauncherLinkTest {

    @Test
    fun `aggiunge https ai link incollati senza schema`() {
        assertEquals("https://chat.whatsapp.com/AbC", ActionLauncher.normalizeLink(" chat.whatsapp.com/AbC "))
        assertEquals("https://t.me/nipoti", ActionLauncher.normalizeLink("https://t.me/nipoti"))
        assertEquals("tg://resolve?domain=x", ActionLauncher.normalizeLink("tg://resolve?domain=x"))
    }

    @Test
    fun `sceglie l'app giusta in base al link`() {
        assertEquals(ContactResolver.WHATSAPP_PACKAGE, ActionLauncher.packageForLink("https://chat.whatsapp.com/AbC"))
        assertEquals(ContactResolver.WHATSAPP_PACKAGE, ActionLauncher.packageForLink("wa.me/393331234567"))
        assertEquals("org.telegram.messenger", ActionLauncher.packageForLink("https://t.me/+AbCdEf"))
        assertNull(ActionLauncher.packageForLink("https://www.comune.it"))
    }
}
