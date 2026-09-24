package com.local.barkfwd

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class WatchedApp(
    val pkg: String,
    val label: String,
    val group: String,
    val icon: String = "",
    val openUrl: String = "",
    val enabled: Boolean = true
)

class Prefs(context: Context) {
    private val p = context.getSharedPreferences("barkfwd", Context.MODE_PRIVATE)

    var barkKey: String
        get() = p.getString("key", "") ?: ""
        set(v) { p.edit().putString("key", v.trim()).apply() }

    fun apps(): MutableList<WatchedApp> {
        val raw = p.getString("apps", "") ?: ""
        if (raw.isBlank()) return defaultApps()
        return raw.lineSequence().mapNotNull { line ->
            val parts = line.split('|')
            if (parts.size < 2) null
            else {
                val pkg = parts[0]
                val label = parts[1]
                val group = parts.getOrElse(2) { guessGroup(pkg, label) }
                val icon = parts.getOrElse(3) { "" }.ifBlank { defaultIcon(pkg, label) }
                val storedUrl = parts.getOrElse(4) { "" }
                val enabled = parts.getOrElse(5) { "1" } != "0"
                WatchedApp(
                    pkg,
                    label,
                    group,
                    icon,
                    storedUrl.ifBlank { defaultOpenUrl(pkg, label) },
                    enabled
                )
            }
        }.toMutableList()
    }

    fun saveApps(list: List<WatchedApp>) {
        val raw = list.joinToString("\n") {
            val on = if (it.enabled) "1" else "0"
            "${it.pkg}|${it.label}|${it.group}|${it.icon}|${it.openUrl.replace("|", "%7C")}|$on"
        }
        p.edit().putString("apps", raw).apply()
    }

    fun addApp(pkg: String, label: String) {
        val list = apps()
        if (list.any { it.pkg == pkg }) return
        list.add(
            WatchedApp(
                pkg,
                label,
                guessGroup(pkg, label),
                defaultIcon(pkg, label),
                defaultOpenUrl(pkg, label),
                true
            )
        )
        saveApps(list)
    }

    fun removeApp(pkg: String) {
        saveApps(apps().filterNot { it.pkg == pkg })
    }

    fun updateApp(pkg: String, icon: String, openUrl: String) {
        saveApps(apps().map {
            if (it.pkg == pkg) it.copy(icon = icon.trim(), openUrl = openUrl.trim()) else it
        })
    }

    fun setEnabled(pkg: String, enabled: Boolean) {
        saveApps(apps().map {
            if (it.pkg == pkg) it.copy(enabled = enabled) else it
        })
    }

    fun app(pkg: String): WatchedApp? = apps().find { it.pkg == pkg }

    fun groupFor(pkg: String): String? {
        val a = app(pkg) ?: return null
        return if (a.enabled) a.group else null
    }

    fun iconFor(pkg: String): String = app(pkg)?.icon.orEmpty()
    fun openUrlFor(pkg: String): String = app(pkg)?.openUrl.orEmpty()

    fun addEvent(group: String, text: String) {
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val line = "$time|$group|$text"
        val old = p.getString("log", "") ?: ""
        val next = (listOf(line) + old.lineSequence().filter { it.isNotBlank() }.toList()).take(8)
        p.edit().putString("log", next.joinToString("\n")).apply()
    }

    fun events(): List<String> {
        val raw = p.getString("log", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.lineSequence().filter { it.isNotBlank() }.toList()
    }

    companion object {
        const val ICON_MAX = "https://raw.githubusercontent.com/teazy865/icons/refs/heads/main/IMG_0246.png"
        const val ICON_VK = "https://raw.githubusercontent.com/teazy865/icons/refs/heads/main/IMG_0251.png"

        fun guessGroup(pkg: String, label: String): String {
            val s = "$pkg $label".lowercase()
            return when {
                "oneme" in s || (s.contains("max") && "vk" !in pkg) -> "MAX"
                "vkontakte" in s || "vk.im" in s || s.split(" ").any { it == "vk" } -> "VK"
                else -> label.take(12)
            }
        }

        fun defaultIcon(pkg: String, label: String): String {
            return when (guessGroup(pkg, label)) {
                "MAX" -> ICON_MAX
                "VK" -> ICON_VK
                else -> ""
            }
        }

        fun defaultOpenUrl(pkg: String, label: String): String {
            return when (guessGroup(pkg, label)) {
                "MAX" -> "https://web.max.ru"
                "VK" -> "vk://vk.com/im"
                else -> ""
            }
        }

        fun defaultApps() = mutableListOf(
            WatchedApp("ru.oneme.app", "MAX", "MAX", ICON_MAX, "https://web.max.ru", true),
            WatchedApp("com.vkontakte.android", "VK", "VK", ICON_VK, "vk://vk.com/im", true),
            WatchedApp("com.vk.im", "VK Messenger", "VK", ICON_VK, "vk://vk.com/im", true)
        )
    }
}
