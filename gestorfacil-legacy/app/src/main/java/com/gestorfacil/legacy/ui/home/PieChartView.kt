package com.gestorfacil.legacy.ui.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class PieChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    data class Slice(val label: String, val amount: Float, val color: Int)

    var slices: List<Slice> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    private val rect = RectF()

    override fun onDraw(canvas: Canvas) {
        if (slices.isEmpty()) return

        val total = slices.sumOf { it.amount.toDouble() }.toFloat()
        if (total <= 0) return

        val size = minOf(width, height).toFloat()
        val strokeWidth = size * 0.28f
        val padding = strokeWidth / 2f + 4f
        rect.set(padding, padding, width - padding, height - padding)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth
        paint.strokeCap = Paint.Cap.BUTT

        var startAngle = -90f
        for (slice in slices) {
            val sweep = (slice.amount / total) * 360f
            paint.color = slice.color
            canvas.drawArc(rect, startAngle, sweep, false, paint)
            startAngle += sweep
        }

        paint.style = Paint.Style.FILL
        paint.textSize = size * 0.18f
        paint.textAlign = Paint.Align.CENTER
        paint.color = 0xFF64748B.toInt()
        val label = "Bs. ${String.format("%.0f", total)}"
        val y = height / 2f + paint.textSize / 3f
        canvas.drawText(label, width / 2f, y, paint)
    }
}
