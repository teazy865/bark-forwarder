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
        } catch (_: Exception) {
            emptyList()
        }
        if (items.isEmpty()) return

        val parsed = items.map { parse(it.notification, group) }
            .maxByOrNull { it.score } ?: return
        if (parsed.title.isBlank() && parsed.body.isBlank()) return
        if (isCountOnly(parsed.body) && parsed.score < 3) return

        val hash = "$pkg|${parsed.title}|${parsed.body}"
        if (hash == lastHash) return
        lastHash = hash

        val head = when {
            parsed.title.isBlank() -> group
            parsed.title.equals(group, true) -> group
            parsed.title.equals("MAX", true) && group == "MAX" -> "MAX"
            else -> "$group: ${parsed.title}"
        }
        thread { BarkClient.send(key, head, parsed.body.ifBlank { parsed.title }, group) }
    }

    private fun parse(n: Notification, group: String): Parsed {
        val extras = n.extras
        val titleRaw = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val big = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty()
        val sub = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim().orEmpty()
        val conv = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()?.trim().orEmpty()
        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.mapNotNull { it?.toString()?.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()

        var sender = ""
        var messages = emptyList<String>()
        try {
            val style = Notification.MessagingStyle.extractMessagingStyleFromNotification(n)
            if (style != null) {
                sender = style.conversationTitle?.toString()?.trim().orEmpty()
                messages = style.messages.mapNotNull { m ->
                    val who = m.sender?.toString()?.trim().orEmpty()
                    val t = m.text?.toString()?.trim().orEmpty()
                    when {
                        t.isEmpty() -> null
                        who.isNotEmpty() && !who.equals(group, true) -> "$who: $t"
                        else -> t
                    }
                }
                if (sender.isEmpty()) {
                    sender = style.messages.lastOrNull()?.sender?.toString()?.trim().orEmpty()
                }
            }
        } catch (_: Exception) {
        }

        val bodyCandidates = listOf(
            messages.joinToString("\n"),
            lines.joinToString("\n"),
            big,
            text,
            sub
        ).map { cleanup(it) }.filter { it.isNotEmpty() && !isCountOnly(it) }

        val body = bodyCandidates.maxByOrNull { it.length }.orEmpty()
        var title = listOf(sender, conv, titleRaw)
            .map { cleanup(it) }
            .firstOrNull { it.isNotEmpty() && !it.equals(group, true) && !isCountOnly(it) }
            .orEmpty()
        if (title.isEmpty()) title = cleanup(titleRaw)

        if (body.isNotEmpty() && title.isNotEmpty() && body.startsWith(title) && body != title) {
            val cut = body.removePrefix(title).trimStart(':', '--', '-', ' ').trim()
            if (cut.isNotEmpty() && !isCountOnly(cut)) {
                return Parsed(title, cut, 5 + cut.length)
            }
        }
        val score = body.length + if (messages.isNotEmpty()) 50 else 0
        return Parsed(title, body.ifBlank { text }, score)
    }

    private fun cleanup(s: String): String =
        s.replace('\u00A0', ' ').trim()

    private fun isCountOnly(s: String): Boolean {
        val t = s.lowercase()
        return Regex("^\\d+\\s+сообщен").containsMatchIn(t) ||
            t == "новое сообщение" ||
            t == "new message"
    }

    private data class Parsed(val title: String, val body: String, val score: Int)
}