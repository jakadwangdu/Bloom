package com.bloom.insta

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

/**
 * Fail-open rules: Bloom only acts when it is sure.
 *  - small previews (e.g. inside a chat) are never touched
 *  - chats, typing and sending are never touched
 *  - at most 2 exit attempts per reel session, so it can never loop out of the app
 */
class ReelsLockService : AccessibilityService() {
    private val ig = "com.instagram.android"
    private var state = "unknown"      // dm | blocked | unknown  (last screen outside the viewer)
    private var inViewer = false
    private var allowed = true
    private var since = 0L
    private var lastAct = 0L
    private var ignoreUntil = 0L
    private var acts = 0
    private var strikes = 0
    private var strikeAt = 0L
    private var locked: String? = null

    private fun nodes(r: AccessibilityNodeInfo, id: String): List<AccessibilityNodeInfo> =
        r.findAccessibilityNodeInfosByViewId("$ig:id/$id") ?: emptyList()
    private fun find(r: AccessibilityNodeInfo, id: String) = nodes(r, id).firstOrNull()
    private fun has(r: AccessibilityNodeInfo, id: String) = find(r, id) != null

    private fun inChat(r: AccessibilityNodeInfo): Boolean =
        has(r, "row_thread_composer_edittext") ||
        has(r, "inbox_refreshable_thread_list_recyclerview") ||
        (r.findAccessibilityNodeInfosByText("Message") ?: emptyList()).any { it.className == "android.widget.EditText" }

    private fun fullScreen(n: AccessibilityNodeInfo): Boolean {
        if (!n.isVisibleToUser) return false
        val r = Rect(); n.getBoundsInScreen(r)
        val m = resources.displayMetrics
        return r.height() > m.heightPixels * 0.6 && r.width() > m.widthPixels * 0.8
    }

    private fun sig(r: AccessibilityNodeInfo): String =
        listOf("clips_author_username", "clips_caption_component").joinToString("|") { id ->
            nodes(r, id).firstOrNull { it.isVisibleToUser }?.text?.toString() ?: ""
        }

    private fun clickUp(n: AccessibilityNodeInfo): Boolean {
        var x: AccessibilityNodeInfo? = n
        while (x != null) {
            if (x.isClickable) return x.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            x = x.parent
        }
        return false
    }

    /** Leave gently: tap Home tab, else the Back arrow, else system Back (once). */
    private fun leave(r: AccessibilityNodeInfo) {
        val home = find(r, "feed_tab")
        if (home != null && clickUp(home)) return
        val back = (r.findAccessibilityNodeInfosByText("Back") ?: emptyList())
            .firstOrNull { it.contentDescription?.toString() == "Back" }
        if (back != null && clickUp(back)) return
        performGlobalAction(GLOBAL_ACTION_BACK)
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

            if (pager == null || !fullScreen(pager)) {   // chat, feed, previews: never interfere
                inViewer = false; acts = 0; strikes = 0; locked = null
                if (inChat(root)) state = "dm"
                else if (has(root, "tab_bar")) {
                    val tabs = listOf("feed_tab", "search_tab", "clips_tab", "profile_tab")
                    if (tabs.any { id -> nodes(root, id).any { it.isSelected } }) state = "blocked"
                }
                return
            }

            if (!inViewer) {
                inViewer = true; since = now; strikes = 0; acts = 0; locked = null
                allowed = state != "blocked"
            }

            val reelsTab = find(root, "clips_tab")?.isSelected == true
            if (!allowed || reelsTab) {
                if (acts < 2 && now - lastAct > 2500) {
                    acts++; lastAct = now
                    leave(root)
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
            if (strikes >= 2 && acts < 2) {
                strikes = 0; acts++; lastAct = now
                performGlobalAction(GLOBAL_ACTION_BACK)
                hit("Back to your chat.")
            } else if (strikes < 2) {
                ignoreUntil = now + 600
                pager.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
                hit("That's the only video.")
            }
        } catch (_: Exception) { /* never crash the service */ }
    }

    override fun onInterrupt() {}
}
