package com.asaniadouglas.terminalapp

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.asaniadouglas.terminalapp.ui.theme.TerminalAppTheme
import com.termux.terminal.TerminalSession
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient
import kotlinx.coroutines.launch

// --- Themes ---

fun color(hex: String): Int = android.graphics.Color.parseColor(hex)

data class TerminalTheme(
    val name: String,
    val background: Int,
    val foreground: Int,
    val cursor: Int,
    val ansiColors: IntArray
)

val dracula = TerminalTheme(
    name = "Dracula",
    background = color("#282A36"),
    foreground = color("#F8F8F2"),
    cursor = color("#FF79C6"),
    ansiColors = intArrayOf(
        color("#21222C"), color("#FF5555"), color("#50FA7B"), color("#F1FA8C"),
        color("#BD93F9"), color("#FF79C6"), color("#8BE9FD"), color("#F8F8F2"),
        color("#6272A4"), color("#FF6E6E"), color("#69FF94"), color("#FFFFA5"),
        color("#D6ACFF"), color("#FF92DF"), color("#A4FFFF"), color("#FFFFFF")
    )
)

val nord = TerminalTheme(
    name = "Nord",
    background = color("#2E3440"),
    foreground = color("#D8DEE9"),
    cursor = color("#88C0D0"),
    ansiColors = intArrayOf(
        color("#3B4252"), color("#BF616A"), color("#A3BE8C"), color("#EBCB8B"),
        color("#81A1C1"), color("#B48EAD"), color("#88C0D0"), color("#E5E9F0"),
        color("#4C566A"), color("#BF616A"), color("#A3BE8C"), color("#EBCB8B"),
        color("#81A1C1"), color("#B48EAD"), color("#8FBCBB"), color("#ECEFF4")
    )
)

val catppuccin = TerminalTheme(
    name = "Catppuccin",
    background = color("#1E1E2E"),
    foreground = color("#CDD6F4"),
    cursor = color("#F5C2E7"),
    ansiColors = intArrayOf(
        color("#45475A"), color("#F38BA8"), color("#A6E3A1"), color("#F9E2AF"),
        color("#89B4FA"), color("#F5C2E7"), color("#94E2D5"), color("#BAC2DE"),
        color("#585B70"), color("#F38BA8"), color("#A6E3A1"), color("#F9E2AF"),
        color("#89B4FA"), color("#F5C2E7"), color("#94E2D5"), color("#A6ADC8")
    )
)

val solarized = TerminalTheme(
    name = "Solarized",
    background = color("#002B36"),
    foreground = color("#839496"),
    cursor = color("#268BD2"),
    ansiColors = intArrayOf(
        color("#073642"), color("#DC322F"), color("#859900"), color("#B58900"),
        color("#268BD2"), color("#D33682"), color("#2AA198"), color("#EEE8D5"),
        color("#002B36"), color("#CB4B16"), color("#586E75"), color("#657B83"),
        color("#839496"), color("#6C71C4"), color("#93A1A1"), color("#FDF6E3")
    )
)

val oneDark = TerminalTheme(
    name = "One Dark",
    background = color("#282C34"),
    foreground = color("#ABB2BF"),
    cursor = color("#528BFF"),
    ansiColors = intArrayOf(
        color("#3F4451"), color("#E06C75"), color("#98C379"), color("#E5C07B"),
        color("#61AFEF"), color("#C678DD"), color("#56B6C2"), color("#ABB2BF"),
        color("#4F5666"), color("#E06C75"), color("#98C379"), color("#E5C07B"),
        color("#61AFEF"), color("#C678DD"), color("#56B6C2"), color("#FFFFFF")
    )
)

val defaultTheme = TerminalTheme(
    name = "Default",
    background = color("#000000"),
    foreground = color("#FFFFFF"),
    cursor = color("#FFFFFF"),
    ansiColors = intArrayOf(
        color("#000000"), color("#AA0000"), color("#00AA00"), color("#AA5500"),
        color("#0000AA"), color("#AA00AA"), color("#00AAAA"), color("#AAAAAA"),
        color("#555555"), color("#FF5555"), color("#55FF55"), color("#FFFF55"),
        color("#5555FF"), color("#FF55FF"), color("#55FFFF"), color("#FFFFFF")
    )
)

val allThemes = listOf(dracula, nord, catppuccin, solarized, oneDark, defaultTheme)

fun applyTheme(session: TerminalSession, view: TerminalView, theme: TerminalTheme) {
    val colors = session.emulator?.mColors?.mCurrentColors ?: return
    for (i in 0..15) colors[i] = theme.ansiColors[i]
    colors[256] = theme.foreground
    colors[257] = theme.background
    colors[258] = theme.cursor
    view.setBackgroundColor(theme.background)
    view.post { view.onScreenUpdated() }
}

// --- Activity ---

class MainActivity : ComponentActivity() {

    private val terminalServiceState = mutableStateOf<TerminalService?>(null)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            terminalServiceState.value = (service as TerminalService.LocalBinder).getService()
        }
        override fun onServiceDisconnected(name: ComponentName) {
            terminalServiceState.value = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TerminalAppTheme {
                val svc = terminalServiceState.value
                if (svc != null) TerminalScreen(service = svc)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, TerminalService::class.java)
        startForegroundService(intent)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return try {
            super.dispatchKeyEvent(event)
        } catch (e: IllegalStateException) {
            false
        }
    }
}

// --- Keyboard Row ---

@Composable
fun KeyboardRow(session: TerminalSession?) {
    var ctrlActive by remember { mutableStateOf(false) }

    val keys = listOf(
        "ESC" to "\u001b", "TAB" to "\t", "^C" to "\u0003", "^D" to "\u0004",
        "↑" to "\u001b[A", "↓" to "\u001b[B", "←" to "\u001b[D", "→" to "\u001b[C",
        "|" to "|", "/" to "/", "-" to "-", "~" to "~", "_" to "_",
        "\\" to "\\", "`" to "`", "'" to "'", "\"" to "\"",
        "(" to "(", ")" to ")", "{" to "{", "}" to "}", "&" to "&", ";" to ";"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A))
            .navigationBarsPadding()
            .height(48.dp)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .height(36.dp).widthIn(min = 52.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (ctrlActive) Color(0xFF4A9EFF) else Color(0xFF2D2D2D))
                .clickable { ctrlActive = !ctrlActive }
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) { Text("CTRL", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium) }

        keys.forEach { (label, sequence) ->
            Box(
                modifier = Modifier
                    .height(36.dp).widthIn(min = 36.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF2D2D2D))
                    .clickable {
                        if (ctrlActive && label.length == 1 && label[0].isLetter()) {
                            session?.write(String(byteArrayOf((label[0].code and 0x1f).toByte())))
                            ctrlActive = false
                        } else {
                            session?.write(sequence)
                        }
                    }
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) { Text(label, color = Color.White, fontSize = 12.sp) }
        }
    }
}

// --- Settings Sheet ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    currentTheme: TerminalTheme,
    fontSize: Float,
    onThemeChange: (TerminalTheme) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1E1E1E)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text("Settings", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(24.dp))

            Text("Theme", fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                allThemes.forEach { theme ->
                    val isSelected = theme.name == currentTheme.name
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onThemeChange(theme) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(theme.background))
                                .then(
                                    if (isSelected)
                                        Modifier.border(2.dp, Color(0xFF4A9EFF), CircleShape)
                                    else
                                        Modifier.border(1.dp, Color(0xFF3A3A3A), CircleShape)
                                )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            theme.name,
                            fontSize = 10.sp,
                            color = if (isSelected) Color(0xFF4A9EFF) else Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Font Size: ${fontSize.toInt()}sp", fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Slider(value = fontSize, onValueChange = onFontSizeChange, valueRange = 20f..80f)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Permissions", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }) {
                    Text("Grant Notification Permission")
                }
            }
        }
    }
}

// --- Terminal Screen ---

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TerminalScreen(service: TerminalService) {
    val context = LocalContext.current
    val tabs = remember { mutableStateListOf<TerminalTab>().also { it.addAll(service.tabs) } }
    val viewCache = remember { mutableMapOf<Int, TerminalView>() }
    val pagerState = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()
    var renamingTab by remember { mutableStateOf<TerminalTab?>(null) }
    var renameText by remember { mutableStateOf("") }
    var currentTheme by remember { mutableStateOf(dracula) }
    var fontSize by remember { mutableStateOf(50f) }
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (tabs.isEmpty()) tabs.add(service.createTab())
    }

    val currentSession = if (tabs.isNotEmpty() && pagerState.currentPage < tabs.size)
        tabs[pagerState.currentPage].session else null

    // Rename dialog
    if (renamingTab != null) {
        AlertDialog(
            onDismissRequest = { renamingTab = null },
            title = { Text("Rename Session") },
            text = {
                TextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    renamingTab?.let { tab ->
                        val index = tabs.indexOfFirst { it.id == tab.id }
                        if (index >= 0) {
                            val updated = tab.copy(name = renameText)
                            tabs[index] = updated
                            val si = service.tabs.indexOfFirst { it.id == tab.id }
                            if (si >= 0) service.tabs[si] = updated
                        }
                    }
                    renamingTab = null
                }) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { renamingTab = null }) { Text("Cancel") }
            }
        )
    }

    if (showSettings) {
        SettingsSheet(
            currentTheme = currentTheme,
            fontSize = fontSize,
            onThemeChange = { theme ->
                currentTheme = theme
                viewCache.forEach { (id, view) ->
                    tabs.find { it.id == id }?.let { applyTheme(it.session, view, theme) }
                }
            },
            onFontSizeChange = { size ->
                fontSize = size
                viewCache.values.forEach { it.setTextSize(size.toInt()) }
            },
            onDismiss = { showSettings = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Color.Black)
    ) {
        // Tab bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(Color(0xFF1A1A1A)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Scrollable tab list
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .horizontalScroll(rememberScrollState())
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = index == pagerState.currentPage
                    val indicatorColor = Color(0xFF4A9EFF)

                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .widthIn(min = 80.dp, max = 150.dp)
                            .background(
                                if (isSelected) Color(0xFF2D2D2D) else Color.Transparent
                            )
                            .drawBehind {
                                if (isSelected) {
                                    val strokePx = 2.dp.toPx()
                                    drawLine(
                                        color = indicatorColor,
                                        start = Offset(0f, size.height - strokePx / 2),
                                        end = Offset(size.width, size.height - strokePx / 2),
                                        strokeWidth = strokePx
                                    )
                                }
                            }
                            .combinedClickable(
                                onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                                onLongClick = { renamingTab = tab; renameText = tab.name }
                            )
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = tab.name,
                            color = if (isSelected) Color.White else Color(0xFF888888),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (tabs.size > 1) {
                            Spacer(Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        val tabId = tab.id
                                        val tabIdx = tabs.indexOfFirst { it.id == tabId }
                                        service.closeTab(tabId)
                                        viewCache.remove(tabId)
                                        tabs.removeAt(tabIdx)
                                        scope.launch {
                                            if (tabs.isNotEmpty()) {
                                                pagerState.scrollToPage(
                                                    pagerState.currentPage.coerceAtMost(tabs.size - 1)
                                                )
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "×",
                                    color = Color(0xFF888888),
                                    fontSize = 14.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            // New tab button
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .fillMaxHeight()
                    .clickable {
                        val newTab = service.createTab()
                        tabs.add(newTab)
                        scope.launch { pagerState.animateScrollToPage(tabs.size - 1) }
                    },
                contentAlignment = Alignment.Center
            ) { Text("+", color = Color.White, fontSize = 20.sp) }

            // Settings button
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .fillMaxHeight()
                    .clickable { showSettings = true },
                contentAlignment = Alignment.Center
            ) { Text("⚙", color = Color(0xFFAAAAAA), fontSize = 16.sp) }
        }

        if (tabs.isNotEmpty()) {
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                val tab = tabs[page]
                var currentTextSize = fontSize
                val view = viewCache.getOrPut(tab.id) {
                    TerminalView(context, null).also { v ->
                        v.setTerminalViewClient(object : TerminalViewClient {
                            override fun onScale(scale: Float): Float {
                                currentTextSize = (currentTextSize * scale).coerceIn(10f, 100f)
                                v.setTextSize(currentTextSize.toInt())
                                return scale
                            }
                            override fun onSingleTapUp(e: MotionEvent) {
                                v.requestFocus()
                                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE)
                                        as InputMethodManager
                                imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT)
                            }
                            override fun shouldBackButtonBeMappedToEscape(): Boolean = false
                            override fun shouldEnforceCharBasedInput(): Boolean = false
                            override fun shouldUseCtrlSpaceWorkaround(): Boolean = false
                            override fun isTerminalViewSelected(): Boolean = true
                            override fun copyModeChanged(copyMode: Boolean) {}
                            override fun onKeyDown(keyCode: Int, e: KeyEvent, session: TerminalSession): Boolean = false
                            override fun onKeyUp(keyCode: Int, e: KeyEvent): Boolean = false
                            override fun onLongPress(event: MotionEvent): Boolean = false
                            override fun readControlKey(): Boolean = false
                            override fun readAltKey(): Boolean = false
                            override fun readShiftKey(): Boolean = false
                            override fun readFnKey(): Boolean = false
                            override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: TerminalSession): Boolean = false
                            override fun onEmulatorSet() {}
                            override fun logError(tag: String, message: String) {}
                            override fun logWarn(tag: String, message: String) {}
                            override fun logInfo(tag: String, message: String) {}
                            override fun logDebug(tag: String, message: String) {}
                            override fun logVerbose(tag: String, message: String) {}
                            override fun logStackTraceWithMessage(tag: String, message: String, e: Exception) {}
                            override fun logStackTrace(tag: String, e: Exception) {}
                        })
                        v.setFocusableInTouchMode(true)
                        v.setTextSize(fontSize.toInt())
                        v.setBackgroundColor(currentTheme.background)
                        v.attachSession(tab.session)
                        applyTheme(tab.session, v, currentTheme)
                    }
                }

                DisposableEffect(tab.id) {
                    service.viewCallbacks[tab.id] = { view.post { view.onScreenUpdated() } }
                    onDispose { service.viewCallbacks.remove(tab.id) }
                }

                AndroidView(factory = { view }, modifier = Modifier.fillMaxSize())
            }
        }

        KeyboardRow(session = currentSession)
    }
}
