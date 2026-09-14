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
        }, 1600)
    }

    private fun flush(pkg: String, group: String, key: String) {
        val items = try {
            activeNotifications?.filter { it.packageName == pkg }.orEmpty()
        } catch (e: Exception) {
            emptyList()
        }
        if (items.isEmpty()) return

        var bestTitle = ""
        var bestBody = ""
        var bestScore = -1

        for (item in items) {
            val extras = item.notification.extras
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
            val big = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty()
            val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                ?.mapNotNull { it?.toString()?.trim() }
                ?.filter { it.isNotEmpty() }
                .orEmpty()
            val joined = lines.joinToString("\n")

            var body = when {
                joined.isNotEmpty() && !isShortCount(joined) -> joined
                big.isNotEmpty() && !isShortCount(big) -> big
                text.isNotEmpty() && !isShortCount(text) -> text
                else -> text
            }
            if (body.startsWith(title) && body.length > title.length) {
                body = body.substring(title.length).trim().trimStart(':', '-', ' ')
            }
            val score = body.length
            if (score > bestScore) {
                bestScore = score
                bestTitle = title
                bestBody = body
            }
        }

        if (bestTitle.isBlank() && bestBody.isBlank()) return
        if (isShortCount(bestBody) && bestScore < 8) return

        val hash = "$pkg|$bestTitle|$bestBody"
        if (hash == lastHash) return
        lastHash = hash

        val head = if (bestTitle.isBlank() || bestTitle.equals(group, true)) group
        else "$group: $bestTitle"

        thread { BarkClient.send(key, head, bestBody.ifBlank { bestTitle }, group) }
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