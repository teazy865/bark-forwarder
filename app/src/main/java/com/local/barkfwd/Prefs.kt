package com.local.barkfwd

import android.content.Context

data class WatchedApp(val pkg: String, val label: String, val group: String)

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
            else WatchedApp(parts[0], parts[1], parts.getOrElse(2) { guessGroup(parts[0], parts[1]) })
        }.toMutableList()
    }

    fun saveApps(list: List<WatchedApp>) {
        val raw = list.joinToString("\n") { "${it.pkg}|${it.label}|${it.group}" }
        p.edit().putString("apps", raw).apply()
    }

    fun addApp(pkg: String, label: String) {
        val list = apps()
        if (list.any { it.pkg == pkg }) return
        list.add(WatchedApp(pkg, label, guessGroup(pkg, label)))
        saveApps(list)
    }

    fun removeApp(pkg: String) {
        saveApps(apps().filterNot { it.pkg == pkg })
    }

    fun groupFor(pkg: String): String? = apps().find { it.pkg == pkg }?.group

    companion object {
        fun guessGroup(pkg: String, label: String): String {
            val s = "$pkg $label".lowercase()
            return when {
                "oneme" in s || s.contains("max") && "vk" !in pkg -> "MAX"
                "vkontakte" in s || "vk.im" in s || s.split(" ").any { it == "vk" } -> "VK"
                else -> label.take(12)
            }
        }

        fun defaultApps() = mutableListOf(
            WatchedApp("ru.oneme.app", "MAX", "MAX"),
            WatchedApp("com.vkontakte.android", "VK", "VK"),
            WatchedApp("com.vk.im", "VK Мессенджер", "VK")
        )
    }
}
