/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2023 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.catrobat.catroid.formulaeditor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.style.ImageSpan
import android.util.LruCache
import androidx.core.graphics.drawable.RoundedBitmapDrawable
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory

private const val COLOR_SQUARE_PADDING_LEFT = 4
private const val COLOR_SQUARE_PADDING_TOP = 0
private const val COLOR_SQUARE_ROUNDED_CORNER_DIVIDER = 4
private const val SWATCH_CACHE_SIZE = 64

class VisualizeColorString(
    context: Context,
    colorString: String,
    bitmapSize: Float
) {

    companion object {
        private val swatchCache = LruCache<Long, Bitmap>(SWATCH_CACHE_SIZE)

        private fun swatchKey(colorValue: Int, size: Int): Long =
            (colorValue.toLong() shl 32) or (size.toLong() and 0xFFFFFFFFL)
    }

    var drawable: RoundedBitmapDrawable
    var imageSpan: VisualizeColorImageSpan
    var colorValue = 0

    init {
        colorValue = getColorValueFromColorString(colorString)

        val size = bitmapSize.toInt().coerceAtLeast(16)
        val key = swatchKey(colorValue, size)
        var bitmap = swatchCache.get(key)
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            drawCheckerboard(canvas, size)

            val colorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colorValue
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), colorPaint)
            swatchCache.put(key, bitmap)
        }

        drawable = RoundedBitmapDrawableFactory.create(context.resources, bitmap)
        drawable.cornerRadius = bitmapSize / COLOR_SQUARE_ROUNDED_CORNER_DIVIDER
        drawable.setBounds(
            COLOR_SQUARE_PADDING_LEFT, COLOR_SQUARE_PADDING_TOP,
            drawable.intrinsicWidth + COLOR_SQUARE_PADDING_LEFT,
            drawable.intrinsicHeight + COLOR_SQUARE_PADDING_TOP
        )
        imageSpan = VisualizeColorImageSpan(drawable, colorValue)
    }

    private fun drawCheckerboard(canvas: Canvas, size: Int) {
        val paintWhite = Paint().apply { color = 0xFFFFFFFF.toInt() }
        val paintGray = Paint().apply { color = 0xFFCCCCCC.toInt() }

        val numCells = 4
        val cellSize = size.toFloat() / numCells

        for (row in 0 until numCells) {
            for (col in 0 until numCells) {
                val paint = if ((row + col) % 2 == 0) paintWhite else paintGray
                canvas.drawRect(
                    col * cellSize,
                    row * cellSize,
                    (col + 1) * cellSize,
                    (row + 1) * cellSize,
                    paint
                )
            }
        }
    }

    private fun getColorValueFromColorString(colorString: String): Int {
        val clean = colorString.trim('\'', '"', ' ', '\t')
        if (clean.isEmpty()) return 0

        return try {
            if (clean.startsWith("#")) {
                parseHexColor(clean)
            } else {
                val hex = clean.replace(Regex("[^A-Za-z0-9]"), "")
                parseHexColor("#$hex")
            }
        } catch (e: Exception) {
            0
        }
    }

    private fun parseHexColor(hexString: String): Int {
        val hex = hexString.substring(1)
        return when (hex.length) {
            6 -> Color.parseColor("#FF$hex")
            8 -> {
                try {
                    Color.parseColor(hexString)
                } catch (e: IllegalArgumentException) {
                    val argb = hex.substring(6, 8) + hex.substring(0, 6)
                    Color.parseColor("#$argb")
                }
            }
            else -> 0
        }
    }
}

class VisualizeColorImageSpan(
    drawable: RoundedBitmapDrawable,
    val colorValue: Int
) : ImageSpan(drawable, ALIGN_BOTTOM)
