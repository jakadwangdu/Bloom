package com.bloom.insta

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.*

class MainActivity : Activity() {
    private val bg = 0xFF0B0A0D.toInt()
    private val fg = 0xFFF2EEE8.toInt()
    private val mute = 0xFF8D8878.toInt()
    private val green = 0xFF6BE38F.toInt()
    private val amber = 0xFFFFB347.toInt()
    private lateinit var p: SharedPreferences
    private var step = -1   // -1 = home, 0.. = setup step

    private class Step(val title: String, val desc: String, val label: String?, val act: (() -> Unit)?)

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        p = getSharedPreferences("bloom", 0)
        window.statusBarColor = bg
        window.navigationBarColor = bg
    }

    override fun onResume() { super.onResume(); render() }

    override fun onBackPressed() {
        if (step >= 0) { step = if (step > 0) step - 1 else -1; render() } else super.onBackPressed()
    }

    private fun render() { if (step < 0) home() else wizard() }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun svcOn(): Boolean {
        val s = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return s.contains("$packageName/")
    }

    private fun t(s: String, sz: Float, c: Int, top: Int = 0, center: Boolean = false) = TextView(this).apply {
        text = s; textSize = sz; setTextColor(c); setPadding(0, dp(top), 0, 0)
        if (sz >= 28f) typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        if (center) gravity = Gravity.CENTER
    }

    private fun btn(s: String, fill: Boolean, f: () -> Unit) = TextView(this).apply {
        text = s; textSize = 15f; gravity = Gravity.CENTER
        setTextColor(if (fill) bg else fg)
        background = GradientDrawable().apply {
            cornerRadius = dp(40).toFloat()
            if (fill) setColor(fg) else setStroke(dp(1), 0x33FFFFFF)
        }
        setPadding(0, dp(15), 0, dp(15))
        setOnClickListener { f() }
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) }
    }

    private fun grow() = Space(this).apply { layoutParams = LinearLayout.LayoutParams(-1, 0, 1f) }

    private fun show(v: LinearLayout.() -> Unit) {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(28), dp(56), dp(28), dp(32)); v()
        }
        val root = ScrollView(this).apply { setBackgroundColor(bg); isFillViewport = true; addView(col) }
        setContentView(root)
        root.alpha = 0f
        root.animate().alpha(1f).setDuration(280).start()
    }

    private fun go(a: String, uri: String? = null) {
        try { startActivity(Intent(a).apply { if (uri != null) data = Uri.parse(uri) }) }
        catch (e: Exception) { startActivity(Intent(Settings.ACTION_SETTINGS)) }
    }

    private fun home() {
        val orb = OrbView(this)
        val title = t("", 30f, fg, 0, true)
        val sub = t("", 15f, mute, 8, true)
        val count = t("", 13f, mute, 28, true)

        fun refresh() {
            val on = p.getBoolean("on", true)
            val sv = svcOn()
            orb.setState(sv && on, !sv)
            title.text = when { !sv -> "Tap to set up"; on -> "Lock is on"; else -> "Lock is off" }
            sub.text = when { !sv -> "Two minutes, once."; on -> "Reels can't pull you in."; else -> "Tap the orb to turn it back on." }
            val today = System.currentTimeMillis() / 86400000L
            val n = if (p.getLong("day", 0) == today) p.getInt("count", 0) else 0
            count.text = if (n == 0) "No Reels stopped yet today" else "$n Reels stopped today"
        }

        orb.layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
        orb.setOnClickListener {
            orb.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            orb.animate().scaleX(0.92f).scaleY(0.92f).setDuration(90).withEndAction {
                orb.animate().scaleX(1f).scaleY(1f).setDuration(220).start()
            }.start()
            if (!svcOn()) { step = 0; render() }
            else { p.edit().putBoolean("on", !p.getBoolean("on", true)).apply(); refresh() }
        }

        show {
            addView(t("BLOOM", 12f, mute).apply { letterSpacing = 0.4f; gravity = Gravity.CENTER })
            addView(orb)
            addView(title); addView(sub); addView(count)
            addView(grow().apply { layoutParams = LinearLayout.LayoutParams(-1, dp(24)) })
            addView(btn("Setup guide", false) { step = 0; render() })
        }
        refresh()
    }

    private fun wizard() {
        val steps = listOf(
            Step("Unlock the switch", "Android 13 and up. Open App info, tap the ⋮ menu at the top right, then choose Allow restricted settings. No ⋮ menu? Skip ahead.",
                "Open App info") { go(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName") },
            Step("Turn Bloom on", "Open Accessibility, find Bloom under Installed or Downloaded apps, switch it on, then tap Allow.",
                "Open Accessibility") { go(Settings.ACTION_ACCESSIBILITY_SETTINGS) },
            Step("Keep it awake", "Set Bloom's battery to Unrestricted. On Xiaomi, Oppo, Vivo or Realme, also allow Autostart.",
                "Open Battery settings") { go(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS) },
            Step("Try it", "Reels tab and Explore reels send you back. A reel from a chat plays, and swiping on returns you to the chat.",
                "Open Instagram") {
                    packageManager.getLaunchIntentForPackage("com.instagram.android")?.let { startActivity(it) }
                }
        )
        val s = steps[step]
        show {
            addView(LinearLayout(this@MainActivity).apply {
                for (i in steps.indices) addView(View(this@MainActivity).apply {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(if (i <= step) fg else 0x33FFFFFF)
                    }
                    layoutParams = LinearLayout.LayoutParams(dp(8), dp(8)).apply { rightMargin = dp(8) }
                })
            })
            addView(grow())
            addView(t("0${step + 1}", 14f, mute))
            addView(t(s.title, 34f, fg, 8))
            addView(t(s.desc, 16f, mute, 16))
            if (step == 1 && svcOn()) addView(t("✓ Bloom is on", 16f, green, 20))
            if (s.label != null && s.act != null) addView(btn(s.label, false) { s.act.invoke() }.apply {
                (layoutParams as LinearLayout.LayoutParams).topMargin = dp(28)
            })
            addView(grow())
            if (step < steps.size - 1) addView(btn("Next", true) { step++; render() })
            else addView(btn("Done", true) { step = -1; render() })
            if (step > 0) addView(btn("Back", false) { step--; render() })
        }
    }
}
