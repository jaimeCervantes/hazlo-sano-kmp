package com.hazlosano.feature.movement.tracker.ui.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.core.graphics.createBitmap

object MapBitmaps {
    const val ICON_ARROW = "icon-arrow"
    const val ICON_HEADING = "icon-heading"

    fun createArrowBitmap(): Bitmap {
        val size = 40
        val bitmap = createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 5f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            alpha = 240
        }

        val path = Path().apply {
            moveTo(14f, 10f)
            lineTo(26f, 20f)
            lineTo(14f, 30f)
        }
        canvas.drawPath(path, paint)

        return bitmap
    }

    fun createHeadingBeamBitmap(colorInt: Int): Bitmap {
        val size = 120
        val bitmap = createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorInt
            style = Paint.Style.FILL
        }

        val path = Path().apply {
            moveTo(size / 2f, size / 2f)
            arcTo(
                (size / 2f) - 50f, (size / 2f) - 50f,
                (size / 2f) + 50f, (size / 2f) + 50f,
                -120f, 60f, false
            )
            close()
        }
        canvas.drawPath(path, paint)

        return bitmap
    }
}
