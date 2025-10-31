package org.catrobat.catroid.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import org.catrobat.catroid.codeanalysis.Severity

class AnalyzedBrickLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BrickLayout(context, attrs, defStyleAttr) {

    private var severity: Severity? = null
    private val linePaint = Paint()

    init {
        setWillNotDraw(false)
        linePaint.style = Paint.Style.STROKE
        linePaint.strokeWidth = 6f
    }

    fun setAnalysisSeverity(severity: Severity?) {
        this.severity = severity
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        severity?.let {
            linePaint.color = if (it == Severity.ERROR) 0xFFFF4444.toInt() else 0xFFFFBB33.toInt()
            canvas.drawLine(0f, height - linePaint.strokeWidth / 2, width.toFloat(), height - linePaint.strokeWidth / 2, linePaint)
        }
    }
}