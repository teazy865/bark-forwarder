package com.local.barkfwd

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
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
        val posted = sbn ?: return
        val pkg = posted.packageName ?: return
        if (pkg == packageName) return
        val prefs = Prefs(this)
        val group = prefs.groupFor(pkg) ?: return
        if (prefs.barkKey.isBlank()) return
        sendParsed(pkg, group, prefs.barkKey, prefs.iconFor(pkg), posted.notification)
        if (!pending.add(pkg)) return
        handler.postDelayed({
            pending.remove(pkg)
            val later = try {
                activeNotifications?.filter { it.packageName == pkg }?.maxByOrNull { it.postTime }
            } catch (e: Exception) {
                null
            }
            sendParsed(pkg, group, prefs.barkKey, prefs.iconFor(pkg), (later ?: posted).notification)
        }, 1800)
    }

    private fun sendParsed(pkg: String, group: String, key: String, icon: String, notification: Notification) {
        val extras = notification.extras
        var title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val big = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty()
        val sub = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim().orEmpty()
        val info = extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString()?.trim().orEmpty()
        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.mapNotNull { it?.toString()?.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()
        val joined = lines.joinToString("\n")
        val chat = chatFrom(extras)
        if (title.isBlank() || title.equals(group, true)) {
            if (chat.first.isNotEmpty()) title = chat.first
        }
        var body = listOf(chat.second, joined, big, text, sub, info)
            .firstOrNull { it.isNotBlank() }
            .orEmpty()
        if (body.startsWith(title) && body.length > title.length) {
            body = body.substring(title.length).trim().trimStart(':', '-', ' ')
        }
        if (title.isBlank() && body.isBlank()) return
        val hash = "$pkg|$title|$body"
        if (hash == lastHash) return
        lastHash = hash
        val head = if (title.isBlank() || title.equals(group, true)) group else "$group: $title"
        thread { BarkClient.send(key, head, body.ifBlank { title }, group, icon, 5) }
    }

    private fun chatFrom(extras: Bundle): Pair<String, String> {
        var who = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()?.trim().orEmpty()
        val texts = ArrayList<String>()
        try {
            val packs = extras.getParcelableArray(Notification.EXTRA_MESSAGES) ?: return who to ""
            for (p in packs) {
                if (p is Bundle) {
                    val t = p.getCharSequence("text")?.toString()?.trim().orEmpty()
                    val w = p.getCharSequence("sender")?.toString()?.trim().orEmpty()
                    if (w.isNotEmpty()) who = w
                    if (t.isNotEmpty()) texts.add(t)
                }
            }
        } catch (e: Exception) {
        }
        return who to texts.joinToString("\n")
    }
}