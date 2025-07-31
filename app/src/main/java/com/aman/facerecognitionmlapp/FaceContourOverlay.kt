package com.aman.facerecognitionmlapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
/**
 * @Author: Amanpreet Kaur
 * @Date: 31-07-2025 16:03
 */
class FaceContourOverlay(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var faces: List<Face> = emptyList()

    fun updateFaces(faces: List<Face>) {
        this.faces = faces
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val paint = Paint().apply {
            color = Color.GREEN
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }

        for (face in faces) {
            // Draw all contour points
            val contours = listOf(
                FaceContour.FACE,
                FaceContour.LEFT_EYEBROW_TOP,
                FaceContour.LEFT_EYEBROW_BOTTOM,
                FaceContour.RIGHT_EYEBROW_TOP,
                FaceContour.RIGHT_EYEBROW_BOTTOM,
                FaceContour.LEFT_EYE,
                FaceContour.RIGHT_EYE,
                FaceContour.UPPER_LIP_TOP,
                FaceContour.UPPER_LIP_BOTTOM,
                FaceContour.LOWER_LIP_TOP,
                FaceContour.LOWER_LIP_BOTTOM,
                FaceContour.NOSE_BRIDGE,
                FaceContour.NOSE_BOTTOM
            )

            contours.forEach { contourType ->
                face.getContour(contourType)?.points?.let { points ->
                    for (point in points) {
                        canvas.drawCircle(point.x, point.y, 4f, paint)
                    }
                }
            }
        }
    }
}