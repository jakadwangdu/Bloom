package com.bloom.insta

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

class ReelsLockService : AccessibilityService() {
    private val ig = "com.instagram.android"
    private var viewerSince = 0L
    private var lastAct = 0L

    private fun find(root: AccessibilityNodeInfo, id: String) =
        root.findAccessibilityNodeInfosByViewId("$ig:id/$id").firstOrNull()

    private fun say(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()

    override fun onAccessibilityEvent(e: AccessibilityEvent) {
        try {
            if (!getSharedPreferences("bloom", 0).getBoolean("on", true)) { viewerSince = 0; return }
            val root = rootInActiveWindow ?: return
            val pager = find(root, "clips_viewer_view_pager")
            if (pager == null) { viewerSince = 0; return }
            val now = SystemClock.uptimeMillis()

            val tab = find(root, "clips_tab")
            if (tab != null && tab.isSelected) {
                if (now - lastAct > 1000) {
                    lastAct = now
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    say("Reels scrolling is off. Open a reel from a message.")
                }
                return
            }

            if (viewerSince == 0L) viewerSince = now
            if (e.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED &&
                now - viewerSince > 1500 && now - lastAct > 900) {
                lastAct = now
                pager.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
                say("That's the only video. No more scrolling.")
            }
        } catch (_: Exception) { /* never crash the service */ }
    }

    override fun onInterrupt() {}
}
