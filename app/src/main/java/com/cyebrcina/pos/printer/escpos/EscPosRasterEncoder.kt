package com.cyebrcina.pos.printer.escpos

import android.graphics.Bitmap
import android.graphics.Color
import java.io.ByteArrayOutputStream

/**
 * Converts a [Bitmap] (from [com.cyebrcina.pos.printer.graphic.ReceiptBitmapRenderer]) to
 * `GS v 0` — the standard ESC/POS raster bit image command virtually every thermal printer that
 * accepts raw ESC/POS bytes supports, same as the cut/cash-drawer-kick commands already in
 * [EscPosEncoder]. This is what makes POS/graphic print mode work on any Bluetooth/USB/network
 * ESC/POS printer without needing a printer-specific bitmap API.
 */
object EscPosRasterEncoder {

    /** Luminance below this (0-255) is printed as black. Plain thresholding rather than
     * dithering — receipts are text/line-art, not photos, so this is sharper for that content. */
    private const val BLACK_THRESHOLD = 160

    fun encode(bitmap: Bitmap): ByteArray {
        val widthBytes = (bitmap.width + 7) / 8
        val out = ByteArrayOutputStream()

        out.write(byteArrayOf(0x1B, 0x40)) // Init, same as EscPosEncoder — leaves the printer in a known state first
        out.write(byteArrayOf(0x1D, 0x76, 0x30, 0x00)) // GS v 0, mode 0 (normal)
        out.write(byteArrayOf((widthBytes and 0xFF).toByte(), ((widthBytes shr 8) and 0xFF).toByte()))
        out.write(byteArrayOf((bitmap.height and 0xFF).toByte(), ((bitmap.height shr 8) and 0xFF).toByte()))

        val row = ByteArray(widthBytes)
        for (y in 0 until bitmap.height) {
            row.fill(0)
            for (x in 0 until bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                val isBlack = if (Color.alpha(pixel) < 128) {
                    false // transparent counts as background/white, not black
                } else {
                    luminance(pixel) < BLACK_THRESHOLD
                }
                if (isBlack) {
                    row[x / 8] = (row[x / 8].toInt() or (0x80 shr (x % 8))).toByte()
                }
            }
            out.write(row)
        }

        out.write(byteArrayOf(0x1D, 0x56, 0x42, 0x00)) // Cut, same command EscPosEncoder uses
        return out.toByteArray()
    }

    private fun luminance(pixel: Int): Int {
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)
        // Standard Rec. 601 luma weights.
        return (r * 299 + g * 587 + b * 114) / 1000
    }
}
