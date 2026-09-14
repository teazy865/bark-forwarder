package com.local.barkfwd

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private lateinit var prefs: Prefs
    private lateinit var appsBox: LinearLayout
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = Prefs(this)
        val key = findViewById<EditText>(R.id.key)
        appsBox = findViewById(R.id.apps)
        status = findViewById(R.id.status)
        key.setText(prefs.barkKey)
        key.setOnFocusChangeListener { _, has -> if (!has) prefs.barkKey = key.text.toString() }

        findViewById<Button>(R.id.add).setOnClickListener {
            prefs.barkKey = key.text.toString()
            startActivity(Intent(this, AppPickerActivity::class.java))
        }
        findViewById<Button>(R.id.access).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        findViewById<Button>(R.id.test).setOnClickListener {
            prefs.barkKey = key.text.toString()
            status.text = "Отправляю тест…"
            thread {
                val r = BarkClient.send(prefs.barkKey, "Тест Bark", "Если это видно на iPhone — ключ верный", "TEST")
                runOnUiThread {
                    status.text = r.fold(
                        onSuccess = { "Тест ушёл. Смотрите iPhone." },
                        onFailure = { "Ошибка: ${it.message}" }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        renderApps()
        status.text =
            if (listenerOn()) "Доступ к уведомлениям включён."
            else "Нужно включить доступ к уведомлениям."
    }

    private fun renderApps() {
        appsBox.removeAllViews()
        for (app in prefs.apps()) {
            val row = TextView(this).apply {
                text = "${app.label}  →  ${app.group}\n${app.pkg}\nнажмите, чтобы удалить"
                textSize = 15f
                setPadding(0, 20, 0, 20)
                setOnClickListener {
                    prefs.removeApp(app.pkg)
                    renderApps()
                }
            }
            appsBox.addView(row)
        }
        if (prefs.apps().isEmpty()) {
            appsBox.addView(TextView(this).apply { text = "Список пуст. Добавьте MAX, VK или дубликат." })
        }
    }

    private fun listenerOn(): Boolean {
        val cn = ComponentName(this, NotifyService::class.java)
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        return flat.split(":").any { ComponentName.unflattenFromString(it) == cn }
    }
}
