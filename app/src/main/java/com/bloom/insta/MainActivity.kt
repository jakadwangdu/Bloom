package com.bloom.insta

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

private val Bg = Color(0xFF000000)
private val Fg = Color(0xFFFFFFFF)
private val Mute = Color(0xFF8A8A8A)
private val Warm = Color(0xFFFFFFFF)
private val Violet = Color(0xFF9A9A9A)
private val Amber = Color(0xFFBDBDBD)
private val Green = Color(0xFFFFFFFF)

class MainActivity : ComponentActivity() {
    private var resumes by mutableIntStateOf(0)

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.statusBarColor = 0xFF000000.toInt()
        window.navigationBarColor = 0xFF000000.toInt()
        setContent { BloomApp(resumes) }
    }

    override fun onResume() { super.onResume(); resumes++ }
}

private fun svcOn(c: Context): Boolean {
    val s = Settings.Secure.getString(c.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
    return s.contains("${c.packageName}/")
}

private fun go(c: Context, action: String, uri: String? = null) {
    try { c.startActivity(Intent(action).apply { if (uri != null) data = Uri.parse(uri) }) }
    catch (e: Exception) { c.startActivity(Intent(Settings.ACTION_SETTINGS)) }
}

@Composable
fun BloomApp(tick: Int) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("bloom", 0) }
    var on by remember { mutableStateOf(prefs.getBoolean("on", true)) }
    var strict by remember { mutableStateOf(prefs.getBoolean("strict", true)) }
    var step by remember { mutableIntStateOf(-1) }
    val sv = remember(tick) { svcOn(ctx) }
    val count = remember(tick) {
        val d = System.currentTimeMillis() / 86400000L
        if (prefs.getLong("day", 0) == d) prefs.getInt("count", 0) else 0
    }
    BackHandler(step >= 0) { step = if (step > 0) step - 1 else -1 }

    Box(Modifier.fillMaxSize().background(Bg)) {
        Particles()
        Box(Modifier.fillMaxSize().systemBarsPadding().padding(28.dp)) {
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    if (targetState > initialState)
                        (slideInHorizontally(tween(350)) { it / 4 } + fadeIn(tween(350))) togetherWith
                            (slideOutHorizontally(tween(250)) { -it / 4 } + fadeOut(tween(250)))
                    else
                        (slideInHorizontally(tween(350)) { -it / 4 } + fadeIn(tween(350))) togetherWith
                            (slideOutHorizontally(tween(250)) { it / 4 } + fadeOut(tween(250)))
                },
                label = "page"
            ) { s ->
                if (s < 0) Home(on, sv, count, strict,
                    onToggle = {
                        if (!sv) step = 0
                        else { on = !on; prefs.edit().putBoolean("on", on).apply() }
                    },
                    onSetup = { step = 0 },
                    onStrict = { strict = !strict; prefs.edit().putBoolean("strict", strict).apply() })
                else Wizard(s, sv,
                    onNext = { step = s + 1 },
                    onBack = { step = s - 1 },
                    onDone = { step = -1 })
            }
        }
    }
}

@Composable
fun Home(on: Boolean, sv: Boolean, count: Int, strict: Boolean, onToggle: () -> Unit, onSetup: () -> Unit, onStrict: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val ax = remember { Animatable(0f) }
    val ay = remember { Animatable(0f) }
    val ring = remember { Animatable(0f) }
    val live = sv && on
    val open by animateFloatAsState(
        when { live -> 1f; !sv -> 0.35f; else -> 0f }, tween(900), label = "open")
    val spin by rememberInfiniteTransition(label = "spin")
        .animateFloat(0f, 360f, infiniteRepeatable(tween(40000, easing = LinearEasing)), label = "deg")
    val shown by animateIntAsState(count, tween(700), label = "count")

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("BLOOM", color = Mute, fontSize = 12.sp, letterSpacing = 6.sp)
            Text("$shown stopped today", color = Mute, fontSize = 12.sp)
        }
        Box(
            Modifier.weight(1f).fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch { ring.snapTo(0f); ring.animateTo(1f, tween(900)) }
                        onToggle()
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            scope.launch { ax.animateTo(0f, spring(0.45f, Spring.StiffnessLow)) }
                            scope.launch { ay.animateTo(0f, spring(0.45f, Spring.StiffnessLow)) }
                        },
                        onDrag = { change, d ->
                            change.consume()
                            scope.launch { ax.snapTo(ax.value + d.x * 0.5f) }
                            scope.launch { ay.snapTo(ay.value + d.y * 0.5f) }
                        })
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val c = center + Offset(ax.value, ay.value)
                val r = size.minDimension * 0.40f
                val n = 12
                val len = r * (0.16f + 0.84f * open)
                for (i in 0 until n) {
                    rotate(spin + i * 360f / n, pivot = c) {
                        drawOval(Fg.copy(alpha = 0.15f + 0.55f * open),
                            topLeft = Offset(c.x - r * 0.12f, c.y - len),
                            size = Size(r * 0.24f, len), style = Stroke(1.5.dp.toPx()))
                    }
                }
                drawCircle(Fg, radius = 3.dp.toPx() + 5.dp.toPx() * open, center = c)
                if (ring.value in 0.001f..0.999f)
                    drawCircle(Fg.copy(alpha = (1f - ring.value) * 0.5f),
                        radius = r * (0.3f + ring.value * 0.9f), center = c, style = Stroke(1.dp.toPx()))
            }
        }
        AnimatedContent(targetState = if (!sv) 0 else if (on) 1 else 2, label = "status") { k ->
            Text(when (k) { 0 -> "Setup"; 1 -> "On"; else -> "Off" },
                color = Fg, fontSize = 72.sp, fontWeight = FontWeight.Light, letterSpacing = (-2).sp)
        }
        Text(when { !sv -> "Tap the bloom to set up."; on -> "Reels can't pull you in."; else -> "Tap the bloom to turn on." },
            color = Mute, fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp, bottom = 28.dp))
        LineRow("Strict mode", if (strict) "On" else "Off", onStrict)
        LineRow("Setup guide", "\u2192", onSetup)
    }
}

@Composable
fun LineRow(label: String, value: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(
        interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x33FFFFFF)))
        Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Fg, fontSize = 16.sp)
            Text(value, color = Mute, fontSize = 16.sp)
        }
    }
}

private class Step(val title: String, val desc: String, val label: String?, val act: (() -> Unit)?)

@Composable
fun Wizard(step: Int, sv: Boolean, onNext: () -> Unit, onBack: () -> Unit, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val steps = remember {
        listOf(
            Step("Unlock the switch",
                "Android 13 and up. Open App info, tap the ⋮ menu at the top right, then choose Allow restricted settings. No ⋮ menu? Skip ahead.",
                "Open App info") { go(ctx, Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${ctx.packageName}") },
            Step("Turn Bloom on",
                "Open Accessibility, find Bloom under Installed or Downloaded apps, switch it on, then tap Allow.",
                "Open Accessibility") { go(ctx, Settings.ACTION_ACCESSIBILITY_SETTINGS) },
            Step("Keep it awake",
                "Set Bloom's battery to Unrestricted. On Xiaomi, Oppo, Vivo or Realme, also allow Autostart.",
                "Open Battery settings") { go(ctx, Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS) },
            Step("Try it",
                "Reels tab and Explore reels send you back. A reel from a chat plays, and swiping on returns you to the chat.",
                "Open Instagram") {
                    ctx.packageManager.getLaunchIntentForPackage("com.instagram.android")?.let { ctx.startActivity(it) }
                }
        )
    }
    val s = steps[step]
    Column(Modifier.fillMaxSize()) {
        Row {
            steps.indices.forEach { i ->
                val w by animateDpAsState(if (i == step) 24.dp else 8.dp, label = "dot")
                Box(Modifier.padding(end = 8.dp).width(w).height(8.dp).clip(CircleShape)
                    .background(if (i <= step) Fg else Color(0x33FFFFFF)))
            }
        }
        Spacer(Modifier.weight(1f))
        Text("0${step + 1}", color = Mute, fontSize = 14.sp)
        Text(s.title, color = Fg, fontSize = 34.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(top = 8.dp))
        Text(s.desc, color = Mute, fontSize = 16.sp, lineHeight = 24.sp, modifier = Modifier.padding(top = 16.dp))
        if (step == 1 && sv) Text("✓ Bloom is on", color = Green, fontSize = 16.sp, modifier = Modifier.padding(top = 20.dp))
        if (s.label != null && s.act != null) {
            Spacer(Modifier.height(16.dp))
            Pill(s.label, false, s.act)
        }
        Spacer(Modifier.weight(1f))
        if (step < steps.size - 1) Pill("Next", true, onNext) else Pill("Done", true, onDone)
        if (step > 0) Pill("Back", false, onBack)
    }
}

@Composable
fun Pill(label: String, fill: Boolean, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by animateFloatAsState(if (pressed) 0.96f else 1f, label = "press")
    val shape = RoundedCornerShape(50)
    Box(
        Modifier.fillMaxWidth().padding(top = 12.dp).scale(sc).clip(shape)
            .then(if (fill) Modifier.background(Fg) else Modifier.border(1.dp, Color(0x33FFFFFF), shape))
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) { Text(label, color = if (fill) Bg else Fg, fontSize = 15.sp) }
}

/** Slow drifting dust behind everything. Integer speeds keep the loop seamless. */
@Composable
fun Particles() {
    val pts = remember {
        val r = java.util.Random(7)
        List(36) { floatArrayOf(r.nextFloat(), r.nextFloat(), 0.6f + r.nextFloat() * 1.4f, (1 + r.nextInt(3)).toFloat()) }
    }
    val t by rememberInfiniteTransition(label = "dust")
        .animateFloat(0f, 1f, infiniteRepeatable(tween(60000, easing = LinearEasing)), label = "dt")
    Canvas(Modifier.fillMaxSize()) {
        pts.forEach { p ->
            val y = (((p[1] - t * p[3]) % 1f) + 1f) % 1f
            drawCircle(Fg.copy(alpha = 0.05f + p[2] * 0.06f), radius = p[2].dp.toPx(),
                center = Offset(p[0] * size.width, y * size.height))
        }
    }
}
