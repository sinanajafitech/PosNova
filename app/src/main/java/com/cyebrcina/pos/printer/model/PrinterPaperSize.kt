package com.cyebrcina.pos.printer.model

/**
 * Admin-set printer paper width (`ReceiptPrefs.paperSize` / kitchen-ticket `prefs.paperSize` in
 * `openapi.yaml`) — overrides the app's own column-width formatting in
 * [com.cyebrcina.pos.printer.ReceiptBuilder]. Names match the API's enum values exactly
 * (`MM_58`/`MM_80`) so they parse straight off `PrinterPaperSize.entries.find { it.name == raw }`.
 */
enum class PrinterPaperSize(val charsPerLine: Int, val bitmapWidthPx: Int) {
    // bitmapWidthPx: the usual printable-area pixel width at ~203dpi for
    // this paper size — used only by POS/graphic-mode rendering
    // (ReceiptBitmapRenderer); ESC/text mode uses charsPerLine instead.
    MM_58(32, 384),
    MM_80(48, 576),
}

/** Falls back to [PrinterPaperSize.MM_58] (the previous hardcoded assumption) if the server hasn't sent a value yet. */
fun String?.toPrinterPaperSize(): PrinterPaperSize =
    PrinterPaperSize.entries.find { it.name == this } ?: PrinterPaperSize.MM_58
