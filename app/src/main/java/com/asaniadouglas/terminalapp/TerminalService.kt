package com.asaniadouglas.terminalapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import java.io.File

class TerminalService : Service() {

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "terminal_channel"
    }

    private val binder = LocalBinder()
    val tabs = mutableListOf<TerminalTab>()
    val viewCallbacks = mutableMapOf<Int, () -> Unit>()
    val titleCallbacks = mutableMapOf<Int, (String) -> Unit>()

    inner class LocalBinder : Binder() {
        fun getService(): TerminalService = this@TerminalService
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    fun createTab(startCwd: String? = null): TerminalTab {
        val id = (tabs.maxOfOrNull { it.id } ?: 0) + 1
        val homeDir = filesDir.absolutePath
        val shell = listOf(
            "/data/data/com.termux/files/usr/bin/bash",
            "/system/bin/bash",
            "/system/bin/sh"
        ).firstOrNull { File(it).canExecute() } ?: "/system/bin/sh"
        val args = arrayOf(shell)
        val env = arrayOf(
            "TERM=xterm-256color",
            "COLORTERM=truecolor",
            "HOME=$homeDir",
            "PATH=/system/bin:/system/xbin",
            "LANG=en_US.UTF-8",
            "USER=terminal",
            "LOGNAME=terminal"
        )
        val client = object : TerminalSessionClient {
            override fun onTextChanged(changedSession: TerminalSession) {
                viewCallbacks[id]?.invoke()
            }
            override fun onTitleChanged(changedSession: TerminalSession) {
                val title = changedSession.title
                if (!title.isNullOrEmpty()) titleCallbacks[id]?.invoke(title)
            }
            override fun onSessionFinished(finishedSession: TerminalSession) {}
            override fun onCopyTextToClipboard(session: TerminalSession, text: String) {
                val cm = getSystemService(ClipboardManager::class.java)
                cm.setPrimaryClip(ClipData.newPlainText("terminal", text))
            }
            override fun onPasteTextFromClipboard(session: TerminalSession?) {}
            override fun onBell(session: TerminalSession) {}
            override fun onColorsChanged(session: TerminalSession) {}
            override fun onTerminalCursorStateChange(state: Boolean) {}
            override fun setTerminalShellPid(session: TerminalSession, pid: Int) {}
            override fun getTerminalCursorStyle(): Int = 0
            override fun logError(tag: String, message: String) {}
            override fun logWarn(tag: String, message: String) {}
            override fun logInfo(tag: String, message: String) {}
            override fun logDebug(tag: String, message: String) {}
            override fun logVerbose(tag: String, message: String) {}
            override fun logStackTraceWithMessage(tag: String, message: String, e: Exception) {}
            override fun logStackTrace(tag: String, e: Exception) {}
        }
        val session = TerminalSession(shell, startCwd ?: homeDir, args, env, 4000, client)
        val tab = TerminalTab(id, "Shell $id", session)
        tabs.add(tab)
        session.write("\n")
        updateNotification()
        return tab
    }

    fun closeTab(id: Int) {
        val index = tabs.indexOfFirst { it.id == id }
        if (index >= 0) {
            tabs[index].session.finishIfRunning()
            tabs.removeAt(index)
            viewCallbacks.remove(id)
            titleCallbacks.remove(id)
            saveTabs()
            updateNotification()
        }
    }

    private fun saveTabs() {
        val serialized = tabs.joinToString(separator = "\n") { tab ->
            val cwd = tab.session.getCwd() ?: filesDir.absolutePath
            "${tab.name}\t$cwd"
        }
        getSharedPreferences("kaze_prefs", MODE_PRIVATE)
            .edit().putString("saved_tabs", serialized).apply()
    }

    fun updateNotification() {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("kaze")
            .setContentText("${tabs.size} session${if (tabs.size != 1) "s" else ""} running")
            .setSmallIcon(R.drawable.ic_terminal)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Terminal Sessions",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Keeps terminal sessions alive" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onDestroy() {
        saveTabs()
        tabs.forEach { it.session.finishIfRunning() }
        super.onDestroy()
    }
}
