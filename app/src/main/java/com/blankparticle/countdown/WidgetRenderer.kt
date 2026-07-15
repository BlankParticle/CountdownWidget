package com.blankparticle.countdown

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.cos
import kotlin.math.sin

/**
 * Draws the widget as white alpha masks, one per theme color. The Glance
 * layer tints each with a GlanceTheme ColorProvider, so the launcher resolves
 * the real Material You dark/light dynamic colors — same tokens as system
 * widgets. Glance can't draw custom shapes, hence the Canvas rendering.
 */
object WidgetRenderer {

    /** Masks tinted with widgetBackground / onSurface / primary. */
    class Layers(val shape: Bitmap, val content: Bitmap, val progress: Bitmap?)

    private val dateFormat = java.time.format.DateTimeFormatter.ofPattern("d MMM yy")

    fun render(
        context: Context,
        sizePx: Int,
        targetEpochDay: Long?,
        createdEpochDay: Long?,
        title: String? = null
    ): Layers {
        val cx = sizePx / 2f
        val cy = sizePx / 2f

        // Scalloped flower: radius oscillates with 12 petals around the circle
        val amp = sizePx * 0.030f
        val outer = sizePx / 2f - 1f
        val base = outer - amp
        val flower = Path()
        for (step in 0..720) {
            val theta = Math.toRadians(step * 0.5)
            val r = base + amp * cos(12 * theta).toFloat()
            val x = cx + r * cos(theta).toFloat()
            val y = cy + r * sin(theta).toFloat()
            if (step == 0) flower.moveTo(x, y) else flower.lineTo(x, y)
        }
        flower.close()

        val shape = newBitmap(sizePx).also {
            Canvas(it).drawPath(flower, fillPaint())
        }

        val content = newBitmap(sizePx)
        val canvas = Canvas(content)
        val font = roundedFont(context)

        if (targetEpochDay == null) {
            val paint = textPaint(font, sizePx * 0.075f)
            canvas.drawText("Tap to", cx, cy - sizePx * 0.03f, paint)
            canvas.drawText("set a date", cx, cy + sizePx * 0.075f, paint)
            return Layers(shape, content, null)
        }

        val today = LocalDate.now()
        val target = LocalDate.ofEpochDay(targetEpochDay)
        val days = ChronoUnit.DAYS.between(today, target)

        // Progress ring: fraction of time elapsed since the date was set
        val stroke = sizePx * 0.045f
        val ringR = (outer - 2 * amp) - stroke / 2f - sizePx * 0.065f
        val ringRect = RectF(cx - ringR, cy - ringR, cx + ringR, cy + ringR)
        // Bar starts full and empties as the target date approaches
        val remaining = when {
            days <= 0 -> 0f
            createdEpochDay == null || targetEpochDay <= createdEpochDay -> 1f
            else -> ((targetEpochDay - today.toEpochDay()).toFloat() /
                    (targetEpochDay - createdEpochDay)).coerceIn(0f, 1f)
        }

        // Track: subtle, so bake reduced alpha into the mask
        canvas.drawArc(ringRect, 0f, 360f, false, strokePaint(stroke, alpha = 0.25f))

        val progress = if (remaining > 0f) {
            newBitmap(sizePx).also {
                Canvas(it).drawArc(ringRect, -90f, 360f * remaining, false, strokePaint(stroke))
            }
        } else null

        val (big, label) = when {
            days > 0 -> days.toString() to if (days == 1L) "Day left" else "Days left"
            days == 0L -> "0" to "Today!"
            else -> (-days).toString() to if (days == -1L) "Day ago" else "Days ago"
        }
        val smallSize = sizePx * 0.062f
        val maxWidth = ringR * 1.5f

        // Big day count, centered on its exact glyph bounds so the gaps to the
        // texts above and below are equal (font metrics include extra space)
        // Varela Round ships one weight, so embolden synthetically
        val bigPaint = textPaint(font, sizePx * 0.27f).apply { isFakeBoldText = true }
        val width = bigPaint.measureText(big)
        if (width > maxWidth) bigPaint.textSize *= maxWidth / width
        val bounds = android.graphics.Rect()
        bigPaint.getTextBounds(big, 0, big.length, bounds)
        canvas.drawText(big, cx, cy - (bounds.top + bounds.bottom) / 2f, bigPaint)

        // Top line: custom title if set, otherwise the target date
        val topPaint = textPaint(font, smallSize, alpha = 0.7f)
        canvas.drawText(
            ellipsize(title?.takeIf { it.isNotBlank() } ?: target.format(dateFormat),
                topPaint, maxWidth),
            cx, cy - sizePx * 0.145f, topPaint
        )
        canvas.drawText(
            label,
            cx, cy + sizePx * 0.18f,
            textPaint(font, smallSize, alpha = 0.7f)
        )

        return Layers(shape, content, progress)
    }

    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var result = text
        while (result.isNotEmpty() && paint.measureText("$result…") > maxWidth) {
            result = result.dropLast(1)
        }
        return "$result…"
    }

    private fun roundedFont(context: Context): Typeface =
        context.resources.getFont(R.font.varela_round)

    private fun newBitmap(sizePx: Int): Bitmap =
        Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)

    private fun maskColor(alpha: Float): Int =
        Color.argb((alpha * 255).toInt(), 255, 255, 255)

    private fun fillPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private fun strokePaint(width: Float, alpha: Float = 1f) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = width
            strokeCap = Paint.Cap.ROUND
            color = maskColor(alpha)
        }

    private fun textPaint(font: Typeface, size: Float, alpha: Float = 1f) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = maskColor(alpha)
            textSize = size
            textAlign = Paint.Align.CENTER
            typeface = font
        }
}
