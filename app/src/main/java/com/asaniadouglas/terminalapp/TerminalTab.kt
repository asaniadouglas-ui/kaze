package com.asaniadouglas.terminalapp

import com.termux.terminal.TerminalSession

data class TerminalTab(val id: Int, val name: String, val session: TerminalSession)
