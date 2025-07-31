package com.aman.facerecognitionmlapp

/**
 * @Author: Amanpreet Kaur
 * @Date: 31-07-2025 16:57
 */
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator

class MirrorCurveView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var sweepX = 0f
    private var sweepY = 0f
    private var animator: ValueAnimator? = null
    private val tailFraction = 1f / 3f

    private var isVertical = true
    private var isPaused = false
    private var pausedValue = 0f

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startSweep()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopSweep()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (isVertical) {
            val tailWidth = width * tailFraction
            val startX = (sweepX - tailWidth).coerceAtLeast(0f)
            val endX = sweepX

            val shader = LinearGradient(
                startX, 0f, endX, 0f,
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.parseColor("#33FFFFFF"),
                    Color.parseColor("#88FFFFFF"),
                    Color.WHITE
                ),
                floatArrayOf(0f, 0.4f, 0.85f, 1f),
                Shader.TileMode.CLAMP
            )
            glowPaint.shader = shader
            canvas.drawRect(startX, 0f, endX, height.toFloat(), glowPaint)
        } else {
            val tailHeight = height * tailFraction
            val startY = (sweepY - tailHeight).coerceAtLeast(0f)
            val endY = sweepY

            val shader = LinearGradient(
                0f, startY, 0f, endY,
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.parseColor("#33FFFFFF"),
                    Color.parseColor("#88FFFFFF"),
                    Color.WHITE
                ),
                floatArrayOf(0f, 0.4f, 0.85f, 1f),
                Shader.TileMode.CLAMP
            )
            glowPaint.shader = shader
            canvas.drawRect(0f, startY, width.toFloat(), endY, glowPaint)
        }
    }

    private fun startSweep(startFrom: Float = 0f) {
        stopSweep()

        if (isVertical) {
            animator = ValueAnimator.ofFloat(startFrom, width.toFloat()).apply {
                duration = ((1f - startFrom / width) * 2000L).toLong()
                interpolator = LinearInterpolator()
                addUpdateListener {
                    sweepX = it.animatedValue as Float
                    invalidate()
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (!isPaused) {
                            isVertical = false
                            sweepY = 0f
                            startSweep()
                        }
                    }
                })
                start()
            }
        } else {
            animator = ValueAnimator.ofFloat(startFrom, height.toFloat()).apply {
                duration = ((1f - startFrom / height) * 2000L).toLong()
                interpolator = LinearInterpolator()
                addUpdateListener {
                    sweepY = it.animatedValue as Float
                    invalidate()
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (!isPaused) {
                            isVertical = true
                            sweepX = 0f
                            startSweep()
                        }
                    }
                })
                start()
            }
        }
    }

    private fun stopSweep() {
        animator?.cancel()
        animator = null
    }

    fun pauseSweep() {
        isPaused = true
        animator?.let {
            pausedValue = if (isVertical) sweepX else sweepY
            it.cancel()
        }
    }

    fun resumeSweep() {
        if (!isPaused) return
        isPaused = false
        startSweep(pausedValue)
    }

    fun restartSweep() {
        stopSweep()
        isVertical = true
        sweepX = 0f
        sweepY = 0f
        isPaused = false
        startSweep()
    }
}

/*class MirrorCurveView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var sweepX = 0f
    private var sweepY = 0f
    private var animator: ValueAnimator? = null
    private val tailFraction = 1f / 3f // Tail covers 1/3 of the view

    private var isVertical = true // Start with left-to-right sweep

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startSweep()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopSweep()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (isVertical) {
            val tailWidth = width * tailFraction
            val startX = (sweepX - tailWidth).coerceAtLeast(0f)
            val endX = sweepX

            val shader = LinearGradient(
                startX, 0f, endX, 0f,
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.parseColor("#33FFFFFF"),
                    Color.parseColor("#88FFFFFF"),
                    Color.WHITE
                ),
                floatArrayOf(0f, 0.4f, 0.85f, 1f),
                Shader.TileMode.CLAMP
            )
            glowPaint.shader = shader
            canvas.drawRect(startX, 0f, endX, height.toFloat(), glowPaint)
        } else {
            val tailHeight = height * tailFraction
            val startY = (sweepY - tailHeight).coerceAtLeast(0f)
            val endY = sweepY

            val shader = LinearGradient(
                0f, startY, 0f, endY,
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.parseColor("#33FFFFFF"),
                    Color.parseColor("#88FFFFFF"),
                    Color.WHITE
                ),
                floatArrayOf(0f, 0.4f, 0.85f, 1f),
                Shader.TileMode.CLAMP
            )
            glowPaint.shader = shader
            canvas.drawRect(0f, startY, width.toFloat(), endY, glowPaint)
        }
    }

    private fun startSweep() {
        stopSweep()
        if (isVertical) {
            animator = ValueAnimator.ofFloat(0f, width.toFloat()).apply {
                duration = 2000L
                interpolator = LinearInterpolator()
                addUpdateListener {
                    sweepX = it.animatedValue as Float
                    invalidate()
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        isVertical = false
                        sweepY = 0f
                        startSweep()
                    }
                })
                start()
            }
        } else {
            animator = ValueAnimator.ofFloat(0f, height.toFloat()).apply {
                duration = 2000L
                interpolator = LinearInterpolator()
                addUpdateListener {
                    sweepY = it.animatedValue as Float
                    invalidate()
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        isVertical = true
                        sweepX = 0f
                        startSweep()
                    }
                })
                start()
            }
        }
    }

    private fun stopSweep() {
        animator?.cancel()
        animator = null
    }

    fun restartSweep() {
        stopSweep()
        startSweep()
    }
}*/
/*
class MirrorCurveView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var sweepX = 0f
    private var animator: ValueAnimator? = null
    private val tailFraction = 1f / 3f // 1/3 of width as tail

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startSweep()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopSweep()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val tailWidth = width * tailFraction
        val startX = (sweepX - tailWidth).coerceAtLeast(0f)
        val endX = sweepX

        // Create a full-rect linear gradient shader with fading tail
        val shader = LinearGradient(
            startX, 0f, endX, 0f,
            intArrayOf(
                Color.TRANSPARENT,
                Color.parseColor("#44CFFFFF"), // faded glow
                Color.parseColor("#AA00FFFF"), // strong glow
                Color.CYAN // head
            ),
            floatArrayOf(0f, 0.4f, 0.85f, 1f),
            Shader.TileMode.CLAMP
        )
        glowPaint.shader = shader

        // Draw the tail and the line as a glowing rect
        canvas.drawRect(startX, 0f, endX, height.toFloat(), glowPaint)
    }

    private fun startSweep() {
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2500L
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                sweepX = width * (it.animatedValue as Float)
                invalidate()
            }
            start()
        }
    }

    private fun stopSweep() {
        animator?.cancel()
        animator = null
    }

    fun restartSweep() {
        stopSweep()
        startSweep()
    }
}*/
