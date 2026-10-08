package com.bloom.insta

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/** A breathing orb: warm and alive when the lock is on, dim when off. */
class OrbView(c: Context) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ev = ArgbEvaluator()
    private var t = 0f
    private var level = 1f
    private var tint = 0xFFFF8A5C.toInt()
    private val loop = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 5000; repeatCount = ValueAnimator.INFINITE; interpolator = LinearInterpolator()
        addUpdateListener { t = it.animatedValue as Float; invalidate() }
    }

    fun setState(on: Boolean, amber: Boolean) {
        tint = if (amber) 0xFFFFB347.toInt() else 0xFFFF8A5C.toInt()
        ValueAnimator.ofFloat(level, if (on) 1f else 0f).apply {
            duration = 700
            addUpdateListener { level = it.animatedValue as Float; invalidate() }
        }.start()
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); loop.start() }
    override fun onDetachedFromWindow() { loop.cancel(); super.onDetachedFromWindow() }

    private fun alpha(c: Int, a: Int) = (c and 0x00FFFFFF) or (a.coerceIn(0, 255) shl 24)

    override fun onDraw(cv: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val base = min(width, height) * 0.30f
        val breathe = 1f + 0.05f * sin(t * 2f * PI.toFloat()) * (0.4f + 0.6f * level)
        val a = ev.evaluate(level, 0xFF3A3840.toInt(), tint) as Int
        val b = ev.evaluate(level, 0xFF1E1D22.toInt(), 0xFFC86BFF.toInt()) as Int
        val gr = base * 2.6f * breathe
        p.shader = RadialGradient(cx, cy, gr, intArrayOf(alpha(a, (110 * level + 14).toInt()), Color.TRANSPARENT), null, Shader.TileMode.CLAMP)
        cv.drawCircle(cx, cy, gr, p)
        p.shader = RadialGradient(cx - base * 0.3f, cy - base * 0.35f, base * 1.4f * breathe, intArrayOf(a, b), null, Shader.TileMode.CLAMP)
        cv.drawCircle(cx, cy, base * breathe, p)
        p.shader = null
    }
}
