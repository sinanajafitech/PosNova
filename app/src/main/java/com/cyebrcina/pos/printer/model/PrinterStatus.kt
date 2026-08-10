package com.cyebrcina.pos.printer.model

enum class PrinterStatus {
    UNKNOWN,
    READY,
    BUSY,
    NO_PAPER,
    DISCONNECTED,
    ERROR,
}

/** State of a single [com.cyebrcina.pos.printer.PrinterService.print] call, for screen UI. */
enum class PrintJobState { IDLE, PRINTING, SUCCESS, FAILED }

/** Print/format primitives a [com.cyebrcina.pos.printer.PrinterService] renders to the thermal printer. */
enum class PrintAlign { LEFT, CENTER, RIGHT }

enum class PrintTextSize { SMALL, NORMAL, LARGE, XLARGE }

sealed interface PrintCommand {
    data class Text(
        val text: String,
        val align: PrintAlign = PrintAlign.LEFT,
        val size: PrintTextSize = PrintTextSize.NORMAL,
        val bold: Boolean = false,
    ) : PrintCommand

    /** Two-column row — e.g. an item name on the left, its price on the right. */
    data class Row(val left: String, val right: String, val bold: Boolean = false) : PrintCommand

    data object Divider : PrintCommand

    data class QrCode(val content: String, val sizeDp: Int = 160) : PrintCommand

    data class FeedLines(val lines: Int = 1) : PrintCommand

    data object Cut : PrintCommand
}

typealias PrintDocument = List<PrintCommand>
