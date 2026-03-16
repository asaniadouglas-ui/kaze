package com.asaniadouglas.terminalapp

import com.termux.terminal.TerminalSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.Mockito.mock

class TerminalTabTest {

    private val session: TerminalSession = mock(TerminalSession::class.java)

    @Test
    fun `copy with new name preserves id and session`() {
        val tab = TerminalTab(1, "Shell 1", session)
        val renamed = tab.copy(name = "My Shell")

        assertEquals(1, renamed.id)
        assertEquals("My Shell", renamed.name)
        assertSame(session, renamed.session)
    }

    @Test
    fun `tabs with same fields are equal`() {
        val tab1 = TerminalTab(2, "Shell 2", session)
        val tab2 = TerminalTab(2, "Shell 2", session)
        assertEquals(tab1, tab2)
    }

    @Test
    fun `tabs with different ids are not equal`() {
        val tab1 = TerminalTab(1, "Shell 1", session)
        val tab2 = TerminalTab(2, "Shell 1", session)
        assertNotEquals(tab1, tab2)
    }

    @Test
    fun `tabs with different names are not equal`() {
        val tab1 = TerminalTab(1, "Shell 1", session)
        val tab2 = TerminalTab(1, "Renamed", session)
        assertNotEquals(tab1, tab2)
    }
}
