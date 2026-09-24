package com.local.barkfwd

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private lateinit var prefs: Prefs
    private lateinit var appsBox: LinearLayout
    private lateinit var logBox: LinearLayout
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = Prefs(this)
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        val key = findViewById<EditText>(R.id.key)
        appsBox = findViewById(R.id.apps)
        logBox = findViewById(R.id.log)
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
            status.text = "Отправка..."
            thread {
                val r = BarkClient.send(
                    prefs.barkKey,
                    "Test Bark",
                    "If you see this on iPhone, the key works",
                    "TEST",
                    Prefs.ICON_VK,
                    "vk://vk.com/im"
                )
                runOnUiThread {
                    status.text = r.fold(
                        onSuccess = { "Тест ушёл. Нажмите пуш на iPhone." },
                        onFailure = { "Ошибка: ${it.message}" }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        renderStatus()
        renderApps()
        renderLog()
    }

    private fun renderStatus() {
        val keyOk = prefs.barkKey.isNotBlank()
        val acc = listenerOn()
        colorLine(findViewById(R.id.stKey), keyOk, if (keyOk) "Ключ Bark сохранён" else "Вставьте ключ Bark")
        colorLine(findViewById(R.id.stAccess), acc, if (acc) "Доступ к уведомлениям" else "Нужен доступ к уведомлениям")
        colorLine(findViewById(R.id.stRun), acc, if (acc) "Служба запущена · Running" else "Служба не запущена")
        status.text = if (acc) "Можно писать в MAX и VK." else "Включите доступ к уведомлениям."
    }

    private fun colorLine(tv: TextView, ok: Boolean, text: String) {
        tv.text = (if (ok) "✓  " else "!  ") + text
        tv.setTextColor(getColor(if (ok) R.color.ok else R.color.bad))
    }

    private fun renderApps() {
        appsBox.removeAllViews()
        val inf = LayoutInflater.from(this)
        for (app in prefs.apps()) {
            val row = inf.inflate(R.layout.item_app, appsBox, false)
            row.findViewById<TextView>(R.id.badge).text = app.group.take(1)
            row.findViewById<TextView>(R.id.name).text = app.label
            val url = if (app.openUrl.isBlank()) "URL не задан" else app.openUrl
            row.findViewById<TextView>(R.id.meta).text = url
            val sw = row.findViewById<Switch>(R.id.on)
            sw.isChecked = app.enabled
            sw.setOnCheckedChangeListener { _, on -> prefs.setEnabled(app.pkg, on) }
            row.setOnClickListener {
                startActivity(Intent(this, EditAppActivity::class.java).putExtra("pkg", app.pkg))
            }
            appsBox.addView(row)
        }
        if (prefs.apps().isEmpty()) {
            appsBox.addView(TextView(this).apply {
                text = "Список пуст. Добавьте MAX или VK."
                setTextColor(getColor(R.color.muted))
            })
        }
    }

    private fun renderLog() {
        logBox.removeAllViews()
        val ev = prefs.events()
        if (ev.isEmpty()) {
            logBox.addView(TextView(this).apply {
                text = "Пока пусто. Напишите себе в MAX или VK."
                setTextColor(getColor(R.color.muted))
            })
            return
        }
        for (line in ev) {
            val parts = line.split('|')
            val text = if (parts.size >= 3) "${parts[0]}   ${parts[1]}   ${parts[2]}" else line
            logBox.addView(TextView(this).apply {
                this.text = text
                setTextColor(getColor(R.color.text))
                textSize = 14f
                setPadding(0, 6, 0, 6)
            })
        }
    }

    private fun listenerOn(): Boolean {
        val cn = ComponentName(this, NotifyService::class.java)
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        return flat.split(":").any { ComponentName.unflattenFromString(it) == cn }
    }
}
