package com.bloom.insta

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

class ReelsLockService : AccessibilityService() {
    private val ig = "com.instagram.android"
    private var state = "other"        // last screen seen outside the reel viewer: "dm" or "other"
    private var inViewer = false
    private var allowed = false        // this viewer session was opened from a chat
    private var since = 0L
    private var lastAct = 0L
    private var ignoreUntil = 0L
    private var strikes = 0
    private var strikeAt = 0L
    private var locked: String? = null // identity of the one allowed reel

    private fun nodes(r: AccessibilityNodeInfo, id: String): List<AccessibilityNodeInfo> =
        r.findAccessibilityNodeInfosByViewId("$ig:id/$id") ?: emptyList()
    private fun find(r: AccessibilityNodeInfo, id: String) = nodes(r, id).firstOrNull()
    private fun has(r: AccessibilityNodeInfo, id: String) = find(r, id) != null

    private fun inChat(r: AccessibilityNodeInfo): Boolean =
        has(r, "row_thread_composer_edittext") ||
        has(r, "inbox_refreshable_thread_list_recyclerview") ||
        (r.findAccessibilityNodeInfosByText("Message") ?: emptyList()).any { it.className == "android.widget.EditText" }

    private fun sig(r: AccessibilityNodeInfo): String =
        listOf("clips_author_username", "clips_caption_component").joinToString("|") { id ->
            nodes(r, id).firstOrNull { it.isVisibleToUser }?.text?.toString() ?: ""
        }

    private fun hit(msg: String) {
        val p = getSharedPreferences("bloom", 0)
        val d = System.currentTimeMillis() / 86400000L
        if (p.getLong("day", 0) != d) p.edit().putLong("day", d).putInt("count", 0).apply()
        p.edit().putInt("count", p.getInt("count", 0) + 1).apply()
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onAccessibilityEvent(e: AccessibilityEvent) {
        try {
            if (!getSharedPreferences("bloom", 0).getBoolean("on", true)) { inViewer = false; return }
            val root = rootInActiveWindow ?: return
            val now = SystemClock.uptimeMillis()
            val pager = find(root, "clips_viewer_view_pager")

            if (pager == null) {            // not watching a reel: remember where we are
                inViewer = false; allowed = false; strikes = 0; locked = null
                if (inChat(root)) state = "dm" else if (has(root, "tab_bar")) state = "other"
                return
            }
            if (!inViewer) {                // a reel just opened: only chats may open one
                inViewer = true; since = now; strikes = 0; locked = null
                allowed = state == "dm"
            }

            val tab = find(root, "clips_tab")
            if (!allowed || tab?.isSelected == true) {   // Reels tab, Explore, profiles, etc.
                if (now - lastAct > 1000) {
                    lastAct = now
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    hit("Reels here are off. Open one from a chat.")
                }
                return
            }

            val s = sig(root)
            val known = s.replace("|", "").isNotEmpty()
            if (locked == null && known) locked = s
            if (now < ignoreUntil || now - since < 1200) return

            val moved = if (known && locked != null) s != locked
                        else e.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
            if (!moved) return

            if (now - strikeAt > 5000) strikes = 0
            strikes++; strikeAt = now
            if (strikes >= 2) {             // second try: leave the viewer entirely
                strikes = 0; lastAct = now
                performGlobalAction(GLOBAL_ACTION_BACK)
                hit("Back to your chat.")
            } else {
                ignoreUntil = now + 600
                pager.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
                hit("That's the only video.")
            }
        } catch (_: Exception) { /* never crash the service */ }
    }

    override fun onInterrupt() {}
}
