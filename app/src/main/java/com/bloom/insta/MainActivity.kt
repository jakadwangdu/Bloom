package com.bloom.insta

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {
    private val bg = 0xFF0B0A0D.toInt()
    private val fg = 0xFFF2EEE8.toInt()
    private val mute = 0xFF8D8878.toInt()
    private lateinit var p: SharedPreferences
    private var cur = 0

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        p = getSharedPreferences("bloom", 0)
        window.statusBarColor = bg
        window.navigationBarColor = bg
    }

    override fun onResume() { super.onResume(); if (cur == 0) home() }
    override fun onBackPressed() { if (cur == 1) { cur = 0; home() } else super.onBackPressed() }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun svcOn(): Boolean {
        val s = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return s.contains("$packageName/")
    }

    private fun t(s: String, sz: Float, c: Int, top: Int = 0) = TextView(this).apply {
        text = s; textSize = sz; setTextColor(c); setPadding(0, dp(top), 0, 0)
    }

    private fun btn(s: String, fill: Boolean, f: () -> Unit) = TextView(this).apply {
        text = s; textSize = 15f; gravity = Gravity.CENTER
        setTextColor(if (fill) bg else fg)
        background = GradientDrawable().apply {
            cornerRadius = dp(40).toFloat()
            if (fill) setColor(fg) else setStroke(dp(1), 0x33FFFFFF)
        }
        setPadding(0, dp(14), 0, dp(14))
        setOnClickListener { f() }
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) }
    }

    private fun show(v: LinearLayout.() -> Unit) {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(28), dp(56), dp(28), dp(40)); v()
        }
        setContentView(ScrollView(this).apply { setBackgroundColor(bg); isFillViewport = true; addView(col) })
    }

    private fun go(a: String, uri: String? = null) {
        try { startActivity(Intent(a).apply { if (uri != null) data = Uri.parse(uri) }) }
        catch (e: Exception) { startActivity(Intent(Settings.ACTION_SETTINGS)) }
    }

    private fun home() {
        cur = 0
        val on = p.getBoolean("on", true)
        val sv = svcOn()
        val (st, c) = when {
            !sv -> "● Setup needed" to 0xFFFFB347.toInt()
            on -> "● Lock is on" to 0xFF6BE38F.toInt()
            else -> "● Lock is off" to mute
        }
        show {
            addView(t("Bloom", 44f, fg))
            addView(t("Watch one. Then stop.", 16f, mute, 4))
            addView(t(st, 15f, c, 48))
            addView(Switch(this@MainActivity).apply {
                text = "Reels lock"; textSize = 18f; setTextColor(fg)
                setPadding(0, dp(24), 0, dp(24)); isChecked = on
                setOnCheckedChangeListener { _, v -> p.edit().putBoolean("on", v).apply(); home() }
            })
            addView(btn(if (sv) "Setup guide" else "Set up Bloom", !sv) { cur = 1; setup() })
            addView(t("Switch off any time. Nothing leaves your phone.", 13f, mute, 20))
        }
    }

    private fun setup() {
        val steps = listOf(
            "Install" to "Open the APK. If Android asks, allow installs from your browser or Files app.",
            "Allow restricted settings" to "Android 13 and up. Tap App info below, then the ⋮ menu (top right), then Allow restricted settings. No ⋮ menu? Skip this step.",
            "Turn Bloom on" to "Tap Accessibility settings below, open Bloom (under Installed or Downloaded apps), switch it on, tap Allow.",
            "Keep it running" to "Tap Battery settings below, find Bloom, choose Unrestricted. On Xiaomi, Oppo, Vivo or Realme, also enable Autostart for Bloom.",
            "Test it" to "Open Instagram and tap the Reels tab: you should be sent back. Open a reel from a chat: it plays, and swiping on snaps back."
        )
        show {
            addView(t("Setup", 36f, fg))
            addView(t("Five steps, about two minutes.", 15f, mute, 4))
            steps.forEachIndexed { i, (h, d) ->
                addView(t("${i + 1}   $h", 17f, fg, 28))
                addView(t(d, 14f, mute, 6))
            }
            addView(t("", 8f, fg, 20))
            addView(btn("Accessibility settings", true) { go(Settings.ACTION_ACCESSIBILITY_SETTINGS) })
            addView(btn("App info", false) { go(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName") })
            addView(btn("Battery settings", false) { go(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS) })
            addView(btn("Back", false) { cur = 0; home() })
            addView(t("To stop Bloom: use the switch on the home screen, or turn it off in Accessibility settings.", 13f, mute, 20))
        }
    }
}
