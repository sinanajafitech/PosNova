package com.cyebrcina.pos.core.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.cyebrcina.pos.core.theme.PosColors
import com.cyebrcina.pos.core.theme.PosTextStyles

/** Destructive-action confirmation (delete order/transaction/inventory item, void, etc). */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, style = PosTextStyles.h6) },
        text = { Text(text = message, style = PosTextStyles.bodySmallRegular, color = PosColors.Neutral8) },
        confirmButton = {
            PrimaryButton(
                text = confirmLabel,
                onClick = onConfirm,
                size = AppButtonSize.MEDIUM,
            )
        },
        dismissButton = {
            SecondaryButton(
                text = "Cancel",
                onClick = onDismiss,
                size = AppButtonSize.MEDIUM,
            )
        },
    )
}
