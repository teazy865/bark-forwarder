package com.local.barkfwd

import android.content.Context

data class WatchedApp(
    val pkg: String,
    val label: String,
    val group: String,
    val icon: String = "",
    val openUrl: String = ""
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
                val icon = parts.getOrElse(3) { "" }
                val storedUrl = parts.getOrElse(4) { "" }
                WatchedApp(
                    pkg,
                    label,
                    group,
                    icon,
                    storedUrl.ifBlank { defaultOpenUrl(pkg, label) }
                )
            }
        }.toMutableList()
    }

    fun saveApps(list: List<WatchedApp>) {
        val raw = list.joinToString("\n") {
            "${it.pkg}|${it.label}|${it.group}|${it.icon}|${it.openUrl.replace("|", "%7C")}"
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
                defaultOpenUrl(pkg, label)
            )
        )
        saveApps(list)
    }

    fun removeApp(pkg: String) {
        saveApps(apps().filterNot { it.pkg == pkg })
    }

    fun setIcon(pkg: String, icon: String) {
        saveApps(apps().map {
            if (it.pkg == pkg) it.copy(icon = icon.trim()) else it
        })
    }

    fun setOpenUrl(pkg: String, openUrl: String) {
        saveApps(apps().map {
            if (it.pkg == pkg) it.copy(openUrl = openUrl.trim()) else it
        })
    }

    fun groupFor(pkg: String): String? = apps().find { it.pkg == pkg }?.group

    fun iconFor(pkg: String): String = apps().find { it.pkg == pkg }?.icon.orEmpty()

    fun openUrlFor(pkg: String): String = apps().find { it.pkg == pkg }?.openUrl.orEmpty()

    companion object {
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
                "MAX" -> "https://raw.githubusercontent.com/rewritte/icons/main/max.png"
                "VK" -> "https://raw.githubusercontent.com/rewritte/icons/main/vk.png"
                else -> ""
            }
        }

        fun defaultOpenUrl(pkg: String, label: String): String {
            return when (guessGroup(pkg, label)) {
                "MAX" -> "https://web.max.ru"
                "VK" -> "vk://"
                else -> ""
            }
        }

        fun defaultApps() = mutableListOf(
            WatchedApp("ru.oneme.app", "MAX", "MAX", defaultIcon("ru.oneme.app", "MAX"), defaultOpenUrl("ru.oneme.app", "MAX")),
            WatchedApp("com.vkontakte.android", "VK", "VK", defaultIcon("com.vkontakte.android", "VK"), defaultOpenUrl("com.vkontakte.android", "VK")),
            WatchedApp("com.vk.im", "VK Messenger", "VK", defaultIcon("com.vk.im", "VK"), defaultOpenUrl("com.vk.im", "VK"))
        )
    }
}
