package org.wit.yiding

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class HorizontalBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val barPaint = Paint().apply {
        color = Color.parseColor("#6200EE")
        style = Paint.Style.FILL
    }
    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 36f
        textAlign = Paint.Align.LEFT
    }
    private var data: Map<String, Int> = emptyMap()
    private var maxStreak: Int = 1
    private var showStreak: Boolean = true

//绘制图表
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty()) return

        val barHeight = 50f
        val startX = 150f
        var startY = 80f
        val scale = (width - 200f) / maxStreak

        canvas.drawLine(100f, 50f, 100f, startY + data.size * 80f, Paint().apply {
            color = Color.BLACK
            strokeWidth = 3f
        })

        val title = if (showStreak) "Habit Persistence Days" else "Total Habit Tracking Days"
        canvas.drawText(title, 20f, 40f, textPaint.apply {
            textSize = 40f
            color = Color.DKGRAY
        })

        canvas.drawText("0", 100f, startY + data.size * 80f + 30f, textPaint.apply {
            textSize = 28f
        })
        canvas.drawText(maxStreak.toString(), width - 50f, startY + data.size * 80f + 30f, textPaint.apply {
            textSize = 28f
        })

        data.forEach { (habitName, days) ->
            canvas.drawText(habitName, 20f, startY + barHeight / 2 + 10, textPaint.apply {
                textSize = 32f
            })

            val barLength = days * scale
            canvas.drawRect(
                startX, startY,
                startX + barLength, startY + barHeight,
                barPaint
            )

            canvas.drawText(
                "$days day",
                startX + barLength + 20f,
                startY + barHeight / 2 + 10,
                textPaint.apply { color = Color.DKGRAY }
            )

            startY += 80f
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(
            MeasureSpec.getSize(widthMeasureSpec),
            (data.size * 80 + 150).toInt().coerceAtLeast(350)
        )
    }
}