package com.cyebrcina.pos.printer.model

/**
 * How a [PrintDocument] is turned into printer output.
 * - [ESC]: plain ESC/POS text commands (the original/only mode) — the printer renders its own
 *   built-in font. Fast, tiny payload, but no real image content and font choice is whatever the
 *   printer firmware has.
 * - [POS]: the document is rendered to a bitmap first ([com.cyebrcina.pos.printer.graphic.ReceiptBitmapRenderer])
 *   then sent as a raster image (standard ESC/POS `GS v 0` command,
 *   [com.cyebrcina.pos.printer.escpos.EscPosRasterEncoder]) — same app-drawn font/weight/spacing
 *   on every printer, and can include real image content, at the cost of a larger payload and a
 *   render step. Only implemented for Bluetooth/USB/network (real ESC/POS) targets — the Imin
 *   built-in printer has no verified bitmap-print primitive in its SDK wrapper
 *   ([com.cyebrcina.pos.printer.imin.IminBuiltInPrinter]) and always prints in ESC/text mode
 *   regardless of this setting.
 */
enum class PrintMode { ESC, POS }

/** Falls back to [PrintMode.ESC] if the server hasn't sent a value yet. */
fun String?.toPrintMode(): PrintMode = PrintMode.entries.find { it.name == this } ?: PrintMode.ESC
