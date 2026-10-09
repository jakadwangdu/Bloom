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

private val Bg = Color(0xFF0B0A0D)
private val Fg = Color(0xFFF2EEE8)
private val Mute = Color(0xFF8D8878)
private val Warm = Color(0xFFFF8A5C)
private val Violet = Color(0xFFC86BFF)
private val Amber = Color(0xFFFFB347)
private val Green = Color(0xFF6BE38F)

class MainActivity : ComponentActivity() {
    private var resumes by mutableIntStateOf(0)

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.statusBarColor = 0xFF0B0A0D.toInt()
        window.navigationBarColor = 0xFF0B0A0D.toInt()
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
                if (s < 0) Home(on, sv, count,
                    onToggle = {
                        if (!sv) step = 0
                        else { on = !on; prefs.edit().putBoolean("on", on).apply() }
                    },
                    onSetup = { step = 0 })
                else Wizard(s, sv,
                    onNext = { step = s + 1 },
                    onBack = { step = s - 1 },
                    onDone = { step = -1 })
            }
        }
    }
}

@Composable
fun Home(on: Boolean, sv: Boolean, count: Int, onToggle: () -> Unit, onSetup: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val ax = remember { Animatable(0f) }
    val ay = remember { Animatable(0f) }
    val ring = remember { Animatable(0f) }
    val live = sv && on
    val level by animateFloatAsState(
        when { live -> 1f; !sv -> 0.45f; else -> 0f }, tween(800), label = "level")
    val t by rememberInfiniteTransition(label = "breath")
        .animateFloat(0f, 1f, infiniteRepeatable(tween(5000, easing = LinearEasing)), label = "t")
    val shown by animateIntAsState(count, tween(700), label = "count")

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("BLOOM", color = Mute, fontSize = 12.sp, letterSpacing = 6.sp)
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
                val base = size.minDimension * 0.30f
                val br = 1f + 0.05f * sin(t * 2f * PI.toFloat()) * (0.4f + 0.6f * level)
                val a = lerp(Color(0xFF3A3840), if (sv) Warm else Amber, level)
                val b = lerp(Color(0xFF1E1D22), Violet, level)
                val gr = base * 2.6f * br
                drawCircle(
                    Brush.radialGradient(listOf(a.copy(alpha = 0.06f + 0.43f * level), Color.Transparent), center = c, radius = gr),
                    radius = gr, center = c)
                if (ring.value in 0.001f..0.999f)
                    drawCircle(a.copy(alpha = (1f - ring.value) * 0.6f),
                        radius = base * (1f + ring.value * 1.6f), center = c, style = Stroke(2.dp.toPx()))
                drawCircle(
                    Brush.radialGradient(listOf(a, b), center = c - Offset(base * 0.3f, base * 0.35f), radius = base * 1.4f * br),
                    radius = base * br, center = c)
            }
        }
        AnimatedContent(targetState = if (!sv) 0 else if (on) 1 else 2, label = "status") { k ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(when (k) { 0 -> "Tap to set up"; 1 -> "Lock is on"; else -> "Lock is off" },
                    color = Fg, fontSize = 30.sp, fontWeight = FontWeight.Light)
                Text(when (k) { 0 -> "Two minutes, once."; 1 -> "Reels can't pull you in."; else -> "Tap the orb to turn it back on." },
                    color = Mute, fontSize = 15.sp, modifier = Modifier.padding(top = 8.dp), textAlign = TextAlign.Center)
            }
        }
        Text(if (shown == 0) "No Reels stopped yet today" else "$shown Reels stopped today",
            color = Mute, fontSize = 13.sp, modifier = Modifier.padding(top = 24.dp, bottom = 24.dp))
        Pill("Setup guide", false, onSetup)
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
