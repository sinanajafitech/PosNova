package com.cyebrcina.pos.printer.graphic

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.cyebrcina.pos.printer.model.PrintAlign
import com.cyebrcina.pos.printer.model.PrintCommand
import com.cyebrcina.pos.printer.model.PrintDocument
import com.cyebrcina.pos.printer.model.PrintTextSize
import com.cyebrcina.pos.printer.model.PrinterPaperSize
import kotlin.math.max

/**
 * POS/graphic print mode: renders a [PrintDocument] (the same command list ESC/text mode
 * consumes — see [com.cyebrcina.pos.printer.escpos.EscPosEncoder]) to a monochrome-ready
 * [Bitmap], for [com.cyebrcina.pos.printer.escpos.EscPosRasterEncoder] to send as a standard
 * ESC/POS raster image (`GS v 0`) instead of text commands. Same app-drawn layout on every
 * printer, rather than depending on the printer firmware's own font.
 */
object ReceiptBitmapRenderer {

    private const val PADDING_X = 16
    private const val LINE_SPACING = 6f

    private fun textSizePx(size: PrintTextSize): Float = when (size) {
        PrintTextSize.SMALL -> 24f
        PrintTextSize.NORMAL -> 32f
        PrintTextSize.LARGE -> 44f
        PrintTextSize.XLARGE -> 56f
    }

    fun render(document: PrintDocument, paperSize: PrinterPaperSize): Bitmap {
        val width = paperSize.bitmapWidthPx
        val contentWidth = width - PADDING_X * 2

        data class Line(val layout: StaticLayout?, val dividerHeight: Int, val topMargin: Int)

        val lines = mutableListOf<Line>()

        // Alignment is handled by StaticLayout's own Layout.Alignment below, not Paint.textAlign
        // (which StaticLayout ignores) — this only sets font size/weight/color.
        fun paintFor(size: PrintTextSize, bold: Boolean): TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = textSizePx(size)
            isFakeBoldText = bold
        }

        fun layoutFor(text: String, paint: TextPaint, alignment: Layout.Alignment): StaticLayout =
            StaticLayout.Builder.obtain(text, 0, text.length, paint, contentWidth)
                .setAlignment(alignment)
                .setLineSpacing(0f, 1f)
                .build()

        document.forEach { command ->
            when (command) {
                is PrintCommand.Text -> {
                    val paint = paintFor(command.size, command.bold)
                    val layoutAlign = when (command.align) {
                        PrintAlign.LEFT -> Layout.Alignment.ALIGN_NORMAL
                        PrintAlign.CENTER -> Layout.Alignment.ALIGN_CENTER
                        PrintAlign.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
                    }
                    lines += Line(layoutFor(command.text, paint, layoutAlign), 0, LINE_SPACING.toInt())
                }
                is PrintCommand.Row -> {
                    val paint = paintFor(PrintTextSize.NORMAL, command.bold)
                    // Rendered as one line with a wide gap — StaticLayout doesn't do two-column
                    // layout natively, so pad with spaces to roughly right-align, matching the
                    // fixed-width feel of the ESC/text Row command.
                    val charsPerLine = (contentWidth / (paint.textSize * 0.55f)).toInt()
                    val rightWidth = (charsPerLine * 0.28f).toInt().coerceAtLeast(6)
                    val text = command.left.padEnd(max(0, charsPerLine - rightWidth)) + command.right
                    lines += Line(layoutFor(text, paint, Layout.Alignment.ALIGN_NORMAL), 0, LINE_SPACING.toInt())
                }
                is PrintCommand.HighlightBox -> {
                    val paint = paintFor(PrintTextSize.LARGE, bold = true)
                    val text = "${command.left}    ${command.right}"
                    lines += Line(layoutFor(text, paint, Layout.Alignment.ALIGN_NORMAL), 0, (LINE_SPACING * 2).toInt())
                }
                PrintCommand.Divider -> lines += Line(null, dividerHeight = 2, topMargin = 12)
                is PrintCommand.FeedLines -> lines += Line(null, dividerHeight = 0, topMargin = (textSizePx(PrintTextSize.NORMAL) * command.lines).toInt())
                is PrintCommand.QrCode -> Unit // Not used by ReceiptBuilder today — no QR-generation dependency pulled in for an unused path.
                PrintCommand.Cut -> Unit // Bitmap has no concept of a cut — the physical cut command is added back in by the ESC/POS raster wrapper.
            }
        }

        val totalHeight = lines.sumOf { line ->
            line.topMargin + (line.layout?.height ?: line.dividerHeight)
        } + 24 // bottom margin

        val bitmap = Bitmap.createBitmap(width, max(1, totalHeight), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        var y = 0f
        val dividerPaint = Paint().apply { color = Color.BLACK; strokeWidth = 2f }

        lines.forEach { line ->
            y += line.topMargin
            if (line.layout != null) {
                canvas.save()
                canvas.translate(PADDING_X.toFloat(), y)
                line.layout.draw(canvas)
                canvas.restore()
                y += line.layout.height
            } else if (line.dividerHeight > 0) {
                canvas.drawRect(Rect(PADDING_X, y.toInt(), width - PADDING_X, y.toInt() + line.dividerHeight), dividerPaint)
                y += line.dividerHeight
            }
        }

        return bitmap
    }
}
