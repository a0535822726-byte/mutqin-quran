package com.deeni.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Color
import android.view.View
import kotlin.math.min

class QiblaCompassView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var relativeAngle = 0f
    private val gold = Color.rgb(218, 183, 111)
    private val cream = Color.rgb(246, 241, 226)
    private val muted = Color.rgb(174, 194, 184)

    fun setDirection(qiblaBearing: Float, deviceAzimuth: Float) {
        relativeAngle = qiblaBearing - deviceAzimuth
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = min(width, height) * 0.39f
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        paint.color = gold
        canvas.drawCircle(cx, cy, radius, paint)
        paint.strokeWidth = 2f
        paint.color = muted
        for (i in 0 until 36) {
            canvas.save()
            canvas.rotate(i * 10f, cx, cy)
            canvas.drawLine(cx, cy - radius + 5f, cx, cy - radius + if (i % 3 == 0) 27f else 15f, paint)
            canvas.restore()
        }
        paint.style = Paint.Style.FILL
        paint.color = cream
        paint.textSize = 30f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("N", cx, cy - radius - 18f, paint)
        canvas.save()
        canvas.rotate(relativeAngle, cx, cy)
        paint.color = gold
        val arrow = Path().apply {
            moveTo(cx, cy - radius * 0.82f)
            lineTo(cx - 24f, cy - radius * 0.48f)
            lineTo(cx, cy - radius * 0.57f)
            lineTo(cx + 24f, cy - radius * 0.48f)
            close()
        }
        canvas.drawPath(arrow, paint)
        paint.color = cream
        canvas.drawCircle(cx, cy, 9f, paint)
        paint.color = gold
        paint.textSize = 24f
        canvas.drawText("القبلة", cx, cy + radius * 0.70f, paint)
        canvas.restore()
    }
}
