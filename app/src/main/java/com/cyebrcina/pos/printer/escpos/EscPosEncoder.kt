package com.cyebrcina.pos.printer.escpos

import com.cyebrcina.pos.printer.model.PrintAlign
import com.cyebrcina.pos.printer.model.PrintCommand
import com.cyebrcina.pos.printer.model.PrintDocument
import com.cyebrcina.pos.printer.model.PrintTextSize
import com.cyebrcina.pos.printer.model.PrinterPaperSize
import java.io.ByteArrayOutputStream

/**
 * Turns a [PrintDocument] into raw ESC/POS bytes — extracted from
 * [com.cyebrcina.pos.printer.imin.IminPrinterService] (which used this for its
 * Bluetooth/USB paths) so a second transport (the network kitchen printer,
 * [com.cyebrcina.pos.printer.network.NetworkPrinterClient]) can produce byte-identical
 * output without duplicating the formatting logic.
 */
object EscPosEncoder {

    /**
     * ESC/POS thermal printers use a single-byte codepage, not UTF-8 — a raw `String.toByteArray()`
     * (UTF-8) encodes "£" as two bytes (0xC2 0xA3) that don't map to anything in the printer's
     * codepage, so it prints garbage instead of the currency symbol. CP437 (IBM437, selected below
     * via `ESC t 0`) is the ESC/POS standard default codepage and maps "£" to the single byte 0x9C.
     */
    private val escPosCharset = charset("Cp437")

    fun encode(document: PrintDocument, paperSize: PrinterPaperSize): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(byteArrayOf(0x1B, 0x40)) // Init
        out.write(byteArrayOf(0x1B, 0x74, 0x00)) // Select character code table: PC437 (USA, Standard Europe)
        document.forEach { command ->
            when (command) {
                is PrintCommand.Text -> {
                    val align = when (command.align) {
                        PrintAlign.LEFT -> 0
                        PrintAlign.CENTER -> 1
                        PrintAlign.RIGHT -> 2
                    }
                    out.write(byteArrayOf(0x1B, 0x61, align.toByte()))
                    out.write(byteArrayOf(0x1B, 0x45, if (command.bold) 1 else 0))
                    val size = when (command.size) {
                        PrintTextSize.SMALL -> 0x00
                        PrintTextSize.NORMAL -> 0x00
                        PrintTextSize.LARGE -> 0x11
                        PrintTextSize.XLARGE -> 0x22
                    }
                    out.write(byteArrayOf(0x1D, 0x21, size.toByte()))
                    out.write((command.text + "\n").toByteArray(escPosCharset))
                }
                is PrintCommand.Row -> {
                    out.write(byteArrayOf(0x1B, 0x61, 0))
                    val rightWidth = 12
                    val leftWidth = paperSize.charsPerLine - rightWidth
                    val line = command.left.padEnd(leftWidth) + command.right.padStart(rightWidth) + "\n"
                    out.write(line.toByteArray(escPosCharset))
                }
                is PrintCommand.HighlightBox -> {
                    out.write(byteArrayOf(0x1B, 0x61, 0)) // left align
                    out.write(byteArrayOf(0x1D, 0x42, 0x01)) // GS B 1 — reverse (white-on-black) on
                    out.write(byteArrayOf(0x1B, 0x45, 1)) // bold on
                    out.write(byteArrayOf(0x1D, 0x21, 0x11)) // GS ! — double width+height
                    val rightWidth = 12
                    val leftWidth = paperSize.charsPerLine - rightWidth
                    val line = command.left.padEnd(leftWidth) + command.right.padStart(rightWidth) + "\n"
                    out.write(line.toByteArray(escPosCharset))
                    out.write(byteArrayOf(0x1D, 0x21, 0x00)) // reset size
                    out.write(byteArrayOf(0x1B, 0x45, 0)) // bold off
                    out.write(byteArrayOf(0x1D, 0x42, 0x00)) // reverse off
                }
                is PrintCommand.Divider -> out.write(("-".repeat(paperSize.charsPerLine) + "\n").toByteArray())
                is PrintCommand.FeedLines -> repeat(command.lines) { out.write("\n".toByteArray()) }
                PrintCommand.Cut -> out.write(byteArrayOf(0x1D, 0x56, 0x42, 0x00))
                else -> {}
            }
        }
        return out.toByteArray()
    }

    /** ESC p m t1 t2 — the standard cash-drawer kick command. `m` selects which of the two pins
     * on the RJ11/RJ12 connector to pulse (0 = pin 2, 1 = pin 5) — different printer/drawer
     * combos wire the drawer to either one, and there's no way to query which from software, so
     * this sends both. A printer/drawer only wired to one pin simply ignores the pulse meant for
     * the other — harmless, not a double-kick a customer would ever notice. */
    val cashDrawerKick: ByteArray = cashDrawerKickPulse(pin = 0) + cashDrawerKickPulse(pin = 1)

    private fun cashDrawerKickPulse(pin: Int): ByteArray =
        byteArrayOf(0x1B, 0x70, pin.toByte(), 0x19, 0xFA.toByte())
}
