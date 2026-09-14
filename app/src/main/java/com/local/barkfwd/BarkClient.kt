package com.local.barkfwd

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object BarkClient {
    fun send(
        key: String,
        title: String,
        body: String,
        group: String,
        icon: String = "",
        tries: Int = 5
    ): Result<String> {
        if (key.isBlank()) return Result.failure(IllegalArgumentException("No Bark key"))
        var last: Result<String> = Result.failure(IllegalStateException("No attempt"))
        repeat(tries) { i ->
            last = once(key, title, body, group, icon)
            if (last.isSuccess) return last
            if (i < tries - 1) {
                try {
                    Thread.sleep((1500L * (i + 1)).coerceAtMost(8000L))
                } catch (e: InterruptedException) {
                    return last
                }
            }
        }
        return last
    }

    private fun once(key: String, title: String, body: String, group: String, icon: String): Result<String> {
        val cleanKey = key.trim().trim('/')
        val url = URL("https://api.day.app/$cleanKey/")
        return try {
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            }
            val fields = mutableListOf(
                "title" to title,
                "body" to body.ifBlank { title },
                "group" to group
            )
            if (icon.isNotBlank()) fields.add("icon" to icon.trim())
            val payload = fields.joinToString("&") { (k, v) ->
                "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
            }
            conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                .bufferedReader().readText()
            conn.disconnect()
            if (code in 200..299) Result.success(text) else Result.failure(RuntimeException("HTTP $code $text"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}