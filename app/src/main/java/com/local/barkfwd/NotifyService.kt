package com.local.barkfwd

import android.app.Notification
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlin.concurrent.thread

class NotifyService : NotificationListenerService() {
    private val handler = Handler(Looper.getMainLooper())
    private val pending = HashSet<String>()
    private var lastHash = ""

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val n = sbn ?: return
        val pkg = n.packageName ?: return
        val prefs = Prefs(this)
        val group = prefs.groupFor(pkg) ?: return
        if (prefs.barkKey.isBlank()) return
        if (!pending.add(pkg)) return
        handler.postDelayed({
            pending.remove(pkg)
            flush(pkg, group, prefs.barkKey)
        }, 900)
    }

    private fun flush(pkg: String, group: String, key: String) {
        val item = try {
            activeNotifications?.filter { it.packageName == pkg }?.maxByOrNull { it.postTime }
        } catch (_: Exception) {
            null
        } ?: return
        val extras = item.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val big = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty()
        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.mapNotNull { it?.toString()?.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()
        val body = when {
            lines.isNotEmpty() -> lines.joinToString("\n")
            big.isNotEmpty() -> big
            text.isNotEmpty() -> text
            else -> title
        }
        if (title.isEmpty() && body.isEmpty()) return
        val hash = "$pkg|$title|$body"
        if (hash == lastHash) return
        lastHash = hash
        thread {
            BarkClient.send(key, "$group: ${title.ifBlank { group }}", body, group)
        }
    }
}
