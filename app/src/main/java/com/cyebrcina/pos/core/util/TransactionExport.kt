package com.cyebrcina.pos.core.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.cyebrcina.pos.data.remote.model.DeviceOrder
import java.io.File

/**
 * Builds a CSV from currently-filtered transactions and opens the system share sheet — the
 * real implementation of Figma's "Export" button (node 12111:93379). Written to the app's cache
 * dir and shared via [androidx.core.content.FileProvider] (see `res/xml/file_paths.xml` /
 * AndroidManifest's `<provider>`) rather than requesting broad storage permissions for a single
 * export action.
 */
fun exportTransactionsCsv(context: Context, orders: List<DeviceOrder>) {
    val csv = buildString {
        appendLine("ID,Date & Time,Customer Name,Status,Total Payment,Orders")
        orders.forEach { order ->
            val itemsSummary = order.items.joinToString("; ") { "${it.quantity}x ${it.productName}" }
            append(csvField(order.number)).append(',')
            append(csvField(order.createdAt)).append(',')
            append(csvField(order.customerName)).append(',')
            append(csvField("Completed")).append(',')
            append(csvField(order.total.asCurrency())).append(',')
            appendLine(csvField(itemsSummary))
        }
    }

    val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
    val file = File(exportsDir, "transactions.csv")
    file.writeText(csv)

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Export Transactions"))
}

private fun csvField(value: String): String = "\"${value.replace("\"", "\"\"")}\""
