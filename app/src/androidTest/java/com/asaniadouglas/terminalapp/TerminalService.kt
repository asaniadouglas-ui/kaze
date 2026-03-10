package com.asaniadouglas.terminalapp

class TerminalService {
}package com.asaniadouglas.terminalapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient

class TerminalService : Service() {

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "terminal_channel"
    }

    private val binder = LocalBinder()
    val tabs = mutableListOf<TerminalTab>()
    val viewCallbacks = mutableMapOf<Int, () -> Unit>()

    inner class LocalBinder : Binder() {
        fun getService(): TerminalService = this@TerminalService
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int,
                                startId: Int): Int {
        return START_STICKY
    }

    fun createTab(): TerminalTab {
        val id = (tabs.maxOfOrNull { it.id } ?: 0) + 1
        val filesDir = filesDir.absolutePath
        val args = arrayOf("/system/bin/sh")
        val env = arrayOf("TERM=xterm-256color", "HOME=" + filesDir)
        val client = object : TerminalSessionClient {
            override fun onTextChanged(changedSession:
                                       TerminalSession) {
                viewCallbacks[id]?.invoke()
            }
            override fun onTitleChanged(changedSession:
                                        TerminalSession) {}
            override fun onSessionFinished(finishedSession:
                                           TerminalSession) {}
            override fun onCopyTextToClipboard(session:
                                               TerminalSession, text: String) {}
            override fun onPasteTextFromClipboard(session:
                                                  TerminalSession?) {}
            override fun onBell(session: TerminalSession) {}
            override fun onColorsChanged(session: TerminalSession)
            {}
            override fun onTerminalCursorStateChange(state: Boolean)
            {}
            override fun setTerminalShellPid(session:
                                             TerminalSession, pid: Int) {}
            override fun getTerminalCursorStyle(): Int = 0
            override fun logError(tag: String, message: String) {}
            override fun logWarn(tag: String, message: String) {}
            override fun logInfo(tag: String, message: String) {}
            override fun logDebug(tag: String, message: String) {}
            override fun logVerbose(tag: String, message: String) {}
            override fun logStackTraceWithMessage(tag: String,
                                                  message: String, e: Exception) {}
            override fun logStackTrace(tag: String, e: Exception) {}
        }
        val session = TerminalSession("/system/bin/sh", filesDir,
            args, env, 4000, client)
        val tab = TerminalTab(id, "Shell $id", session)
        tabs.add(tab)
        session.write("\n")
        updateNotification()
        return tab
    }

    fun updateNotification() {
        val manager =
            getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Terminal")
            .setContentText("${tabs.size} session${if (tabs.size !=
                1) "s" else ""} running")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Terminal Sessions",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps terminal sessions alive"
        }
        getSystemService(NotificationManager::class.java).createNoti
        ficationChannel(channel)
    }

    override fun onDestroy() {
        tabs.forEach { it.session.finishIfRunning() }
        super.onDestroy()
    }
}
