package com.asaniadouglas.terminalapp

import com.termux.terminal.TerminalSession
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock

/**
 * Tests for the tab ID assignment logic used in TerminalService.createTab():
 *   val id = (tabs.maxOfOrNull { it.id } ?: 0) + 1
 */
class TabIdAssignmentTest {

    private val session: TerminalSession = mock(TerminalSession::class.java)

    /** Replicates the ID assignment expression from TerminalService.createTab(). */
    private fun nextId(tabs: List<TerminalTab>): Int =
        (tabs.maxOfOrNull { it.id } ?: 0) + 1

    @Test
    fun `first tab gets id 1`() {
        assertEquals(1, nextId(emptyList()))
    }

    @Test
    fun `second tab gets id 2`() {
        val existing = listOf(TerminalTab(1, "Shell 1", session))
        assertEquals(2, nextId(existing))
    }

    @Test
    fun `new id is one more than current maximum`() {
        // Simulates a state where tabs 1 and 3 exist (tab 2 was closed)
        val existing = listOf(
            TerminalTab(1, "Shell 1", session),
            TerminalTab(3, "Shell 3", session)
        )
        assertEquals(4, nextId(existing))
    }
}
