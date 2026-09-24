package com.local.barkfwd

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.thread

class EditAppActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_app)
        val prefs = Prefs(this)
        val pkg = intent.getStringExtra("pkg") ?: return finish()
        val app = prefs.app(pkg) ?: return finish()

        findViewById<TextView>(R.id.title).text = app.label
        findViewById<TextView>(R.id.pkg).text = app.pkg
        val icon = findViewById<EditText>(R.id.icon)
        val url = findViewById<EditText>(R.id.url)
        icon.setText(app.icon)
        url.setText(app.openUrl)
        findViewById<TextView>(R.id.hint).text =
            "Пустой vk:// часто открывает сайт. Для VK лучше vk://vk.com/im"

        val chips = findViewById<LinearLayout>(R.id.chips)
        val presets = if (app.group == "VK") {
            listOf("Сообщения" to "vk://vk.com/im", "Лента" to "vk://vk.com/feed", "Главная" to "vk://vk.com")
        } else if (app.group == "MAX") {
            listOf("Веб" to "https://web.max.ru", "Сайт" to "https://max.ru", "max://" to "max://")
        } else emptyList()
        for ((name, value) in presets) {
            val b = Button(this)
            b.text = name
            b.setOnClickListener { url.setText(value) }
            chips.addView(b)
        }

        findViewById<Button>(R.id.save).setOnClickListener {
            prefs.updateApp(pkg, icon.text.toString(), url.text.toString())
            finish()
        }
        findViewById<Button>(R.id.delete).setOnClickListener {
            prefs.removeApp(pkg)
            finish()
        }
        findViewById<Button>(R.id.test).setOnClickListener {
            prefs.updateApp(pkg, icon.text.toString(), url.text.toString())
            thread {
                BarkClient.send(
                    prefs.barkKey,
                    app.group + " test",
                    "Tap should open " + url.text.toString(),
                    app.group,
                    icon.text.toString(),
                    url.text.toString()
                )
            }
        }
    }
}
