package com.local.barkfwd

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import kotlin.concurrent.thread

class NotifyService : NotificationListenerService() {
    private val handler = Handler(Looper.getMainLooper())
    private val pending = HashSet<String>()
    private var lastHash = ""

    override fun onListenerConnected() {
        super.onListenerConnected()
        startKeepAlive()
    }

    override fun onListenerDisconnected() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onListenerDisconnected()
    }

    private fun startKeepAlive() {
        val nm = getSystemService(NotificationManager::class.java)
        val channelId = "keep"
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(
                channelId,
                "Keep running",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setSound(null, null)
                enableVibration(false)
            }
            nm.createNotificationChannel(ch)
        }
        val open = PendingIntent.getActivity(
            this,
            1,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("iPhone Pushes")
            .setContentText("Running. Do not dismiss.")
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(open)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(42, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(42, n)
            }
        } catch (e: Exception) {
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val n = sbn ?: return
        val pkg = n.packageName ?: return
        if (pkg == packageName) return
        val prefs = Prefs(this)
        val group = prefs.groupFor(pkg) ?: return
        if (prefs.barkKey.isBlank()) return
        if (!pending.add(pkg)) return
        handler.postDelayed({
            pending.remove(pkg)
            flush(pkg, group, prefs.barkKey, prefs.iconFor(pkg))
        }, 1600)
    }

    private fun flush(pkg: String, group: String, key: String, icon: String) {
        val items = try {
            activeNotifications?.filter { it.packageName == pkg }.orEmpty()
        } catch (e: Exception) {
            emptyList()
        }
        if (items.isEmpty()) return
        val newest = items.maxByOrNull { it.postTime } ?: return
        val extras = newest.notification.extras
        var title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val big = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty()
        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.mapNotNull { it?.toString()?.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()
        val joined = lines.joinToString("\n")
        var chatText = ""
        var chatWho = ""
        try {
            val style = Notification.MessagingStyle.extractMessagingStyleFromNotification(newest.notification)
            if (style != null) {
                chatWho = style.conversationTitle?.toString()?.trim().orEmpty()
                chatText = style.messages.mapNotNull { it.text?.toString()?.trim() }
                    .filter { it.isNotEmpty() && !isShortCount(it) }
                    .joinToString("\n")
                if (chatWho.isEmpty()) {
                    chatWho = style.messages.lastOrNull()?.sender?.toString()?.trim().orEmpty()
                }
            }
        } catch (e: Exception) {
        }
        var body = when {
            chatText.isNotEmpty() -> chatText
            joined.isNotEmpty() && !isShortCount(joined) -> joined
            big.isNotEmpty() && !isShortCount(big) -> big
            text.isNotEmpty() && !isShortCount(text) -> text
            else -> text
        }
        if (title.isBlank() || title.equals(group, true) || isShortCount(title)) {
            if (chatWho.isNotEmpty()) title = chatWho
        }
        if (body.startsWith(title) && body.length > title.length) {
            body = body.substring(title.length).trim().trimStart(':', '-', ' ')
        }
        if (title.isBlank() && body.isBlank()) return
        if (isShortCount(body) && body.length < 8) return
        val hash = "$pkg|$title|$body"
        if (hash == lastHash) return
        lastHash = hash
        val head = if (title.isBlank() || title.equals(group, true)) group else "$group: $title"
        thread { BarkClient.send(key, head, body.ifBlank { title }, group, icon, 5) }
    }

    private fun isShortCount(s: String): Boolean {
        val t = s.trim().lowercase()
        if (t == "new message") return true
        val i = t.indexOf(' ')
        if (i <= 0) return false
        val first = t.substring(0, i)
        return first.all { it.isDigit() } && t.length < 24
    }
}