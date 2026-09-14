package com.local.barkfwd

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
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
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
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
            status.text = "Sending test..."
            thread {
                val r = BarkClient.send(prefs.barkKey, "Test Bark", "If you see this on iPhone, the key works", "TEST")
                runOnUiThread {
                    status.text = r.fold(
                        onSuccess = { "Test sent. Check iPhone." },
                        onFailure = { "Error: ${it.message}" }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        renderApps()
        status.text =
            if (listenerOn()) "Notification access is on."
            else "Turn on notification access."
    }

    private fun renderApps() {
        appsBox.removeAllViews()
        for (app in prefs.apps()) {
            val iconNote = if (app.icon.isBlank()) "no icon" else "icon set"
            val row = TextView(this).apply {
                text = "${app.label}  ->  ${app.group}  ($iconNote)\n${app.pkg}\ntap: icon URL    long tap: delete"
                textSize = 15f
                setPadding(0, 20, 0, 20)
                setOnClickListener { askIcon(app) }
                setOnLongClickListener {
                    prefs.removeApp(app.pkg)
                    renderApps()
                    true
                }
            }
            appsBox.addView(row)
        }
        if (prefs.apps().isEmpty()) {
            appsBox.addView(TextView(this).apply { text = "List is empty. Add MAX, VK or a clone." })
        }
    }

    private fun askIcon(app: WatchedApp) {
        val input = EditText(this).apply {
            hint = "https://...png"
            setText(app.icon)
            setSingleLine()
        }
        AlertDialog.Builder(this)
            .setTitle("Icon for ${app.label}")
            .setMessage("Direct image link for Bark. GitHub raw URL works.")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                prefs.setIcon(app.pkg, input.text.toString())
                renderApps()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun listenerOn(): Boolean {
        val cn = ComponentName(this, NotifyService::class.java)
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        return flat.split(":").any { ComponentName.unflattenFromString(it) == cn }
    }
}