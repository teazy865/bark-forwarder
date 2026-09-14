package com.local.barkfwd

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity

class AppPickerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val list = ListView(this)
        setContentView(list)

        val pm = packageManager
        val apps = pm.getInstalledApplications(0)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 || visibleLauncher(it.packageName) }
            .map { it.packageName to (it.loadLabel(pm)?.toString() ?: it.packageName) }
            .distinctBy { it.first }
            .sortedBy { it.second.lowercase() }

        list.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            apps.map { "${it.second}\n${it.first}" }
        )
        list.setOnItemClickListener { _, _, pos, _ ->
            val (pkg, label) = apps[pos]
            Prefs(this).addApp(pkg, label)
            finish()
        }
    }

    private fun visibleLauncher(pkg: String): Boolean {
        val intent = packageManager.getLaunchIntentForPackage(pkg)
        return intent != null
    }
}
