package com.cyebrcina.pos.feature.register

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cyebrcina.pos.core.components.AppTextField
import com.cyebrcina.pos.core.components.PosTopBar
import com.cyebrcina.pos.core.theme.PosColors
import com.cyebrcina.pos.core.theme.PosTextStyles
import com.cyebrcina.pos.core.theme.Spacing
import com.cyebrcina.pos.core.util.asCurrency
import com.cyebrcina.pos.core.util.asDateTime
import com.cyebrcina.pos.core.util.toInstantOrNull
import com.cyebrcina.pos.data.remote.model.CashRegisterSessionDto
import com.cyebrcina.pos.data.remote.model.ZReport
import com.cyebrcina.pos.feature.order.create.FlowPrimaryButton
import kotlin.math.abs

/**
 * A top-level tab (like Tables/Calls), not tucked inside Settings — matches the rest of the
 * "main POS" flow's visual language (OrderSuccessScreen's centered white card, FlowPrimaryButton,
 * AppTextField) rather than Settings' plain utilitarian cards, since opening/closing the
 * register is as central to a shift as taking an order.
 */
@Composable
fun RegisterScreen(viewModel: RegisterViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { PosTopBar(title = "Cash Register") }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(PosColors.Surface), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                state.error?.let { message ->
                    Text(
                        message,
                        style = PosTextStyles.bodySmallRegular,
                        color = PosColors.Warning500,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(500.dp).padding(bottom = Spacing.sm),
                    )
                }

                when {
                    state.justClosed != null -> ClosedCard(state.justClosed!!, onDismiss = viewModel::dismissClosedSummary)
                    state.session != null -> OpenCard(
                        session = state.session!!,
                        isSubmitting = state.isSubmitting,
                        onClose = viewModel::closeRegister,
                        onViewXReport = viewModel::viewXReport,
                    )
                    !state.isLoading -> StartCard(state.isSubmitting, onOpen = viewModel::openRegister)
                }
            }
        }

        if (state.isLoadingXReport || state.xReport != null || state.xReportError != null) {
            Dialog(onDismissRequest = viewModel::dismissXReport) {
                XReportCard(
                    report = state.xReport,
                    isLoading = state.isLoadingXReport,
                    error = state.xReportError,
                    printWarning = state.xReportPrintWarning,
                    onPrint = viewModel::printXReport,
                    onDismiss = viewModel::dismissXReport,
                )
            }
        }
    }
}

@Composable
private fun IconBadge(icon: ImageVector, tint: Color) {
    Box(Modifier.height(100.dp).width(100.dp).clip(CircleShape).background(tint), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = PosColors.White, modifier = Modifier.height(42.dp))
    }
}

@Composable
private fun StartCard(isSubmitting: Boolean, onOpen: (Double) -> Unit) {
    var floatAmount by remember { mutableStateOf("") }

    FlowCard {
        IconBadge(Icons.Filled.LocalAtm, PosColors.Blue500)
        Spacer(Modifier.height(Spacing.md))
        Text("Open Register", style = PosTextStyles.h3, color = PosColors.Neutral13)
        Spacer(Modifier.height(Spacing.xs))
        Text(
            "Count the starting cash in the drawer before the shift begins.",
            style = PosTextStyles.bodyMediumMedium,
            color = PosColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.lg))
        AppTextField(
            value = floatAmount,
            onValueChange = { floatAmount = it },
            label = "Starting Float",
            placeholder = "0.00",
            keyboardType = KeyboardType.Decimal,
        )
        Spacer(Modifier.height(Spacing.lg))
        FlowPrimaryButton(
            text = "Open Register",
            onClick = { floatAmount.toDoubleOrNull()?.let(onOpen) },
            modifier = Modifier.fillMaxWidth(),
            enabled = floatAmount.toDoubleOrNull() != null && !isSubmitting,
            loading = isSubmitting,
        )
    }
}

@Composable
private fun OpenCard(
    session: CashRegisterSessionDto,
    isSubmitting: Boolean,
    onClose: (Double) -> Unit,
    onViewXReport: () -> Unit,
) {
    var countedCash by remember { mutableStateOf("") }

    FlowCard {
        IconBadge(Icons.Filled.LocalAtm, PosColors.SuccessAccent)
        Spacer(Modifier.height(Spacing.md))
        Text("Register Open", style = PosTextStyles.h3, color = PosColors.Neutral13)
        Spacer(Modifier.height(Spacing.xs))
        Text(
            "Opened ${session.openedAt.toInstantOrNull()?.asDateTime() ?: session.openedAt}" +
                (session.openedByStaffName?.let { " by $it" } ?: ""),
            style = PosTextStyles.bodyMediumMedium,
            color = PosColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.lg))

        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(PosColors.Surface).padding(Spacing.sm)) {
            SummaryRow("Opening Float", session.openingFloat.asCurrency())
        }
        Spacer(Modifier.height(Spacing.sm))

        // Mid-shift checkpoint — cash/card totals so far, without closing anything. Distinct from
        // Close Register below, which is a real, destructive-to-the-open-session action.
        TextButton(onClick = onViewXReport) {
            Icon(Icons.Filled.QueryStats, contentDescription = null, tint = PosColors.Blue500, modifier = Modifier.height(18.dp))
            Spacer(Modifier.width(Spacing.xxs))
            Text("View X-Report (cash + card so far)", style = PosTextStyles.bodySmallSemibold, color = PosColors.Blue500)
        }
        Spacer(Modifier.height(Spacing.md))

        AppTextField(
            value = countedCash,
            onValueChange = { countedCash = it },
            label = "Counted Cash",
            placeholder = "0.00",
            keyboardType = KeyboardType.Decimal,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            "Count the cash in the drawer now — it's checked against this till's own cash sales since opening.",
            style = PosTextStyles.bodyXSmallRegular,
            color = PosColors.TextSecondary,
        )
        Spacer(Modifier.height(Spacing.lg))
        FlowPrimaryButton(
            text = "Close Register",
            onClick = { countedCash.toDoubleOrNull()?.let(onClose) },
            modifier = Modifier.fillMaxWidth(),
            enabled = countedCash.toDoubleOrNull() != null && !isSubmitting,
            loading = isSubmitting,
        )
    }
}

@Composable
private fun ClosedCard(session: CashRegisterSessionDto, onDismiss: () -> Unit) {
    val variance = session.variance ?: 0.0
    val balanced = abs(variance) < 0.01
    val accent = if (balanced) PosColors.SuccessAccent else PosColors.Warning500

    FlowCard {
        IconBadge(if (balanced) Icons.Filled.Check else Icons.Filled.Warning, accent)
        Spacer(Modifier.height(Spacing.md))
        Text("Register Closed", style = PosTextStyles.h3, color = PosColors.Neutral13)
        Spacer(Modifier.height(Spacing.xs))
        Text(
            if (balanced) "The drawer matches expected cash exactly." else "The drawer doesn't match expected cash — see below.",
            style = PosTextStyles.bodyMediumMedium,
            color = PosColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.lg))

        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(PosColors.Surface).padding(Spacing.sm)) {
            SummaryRow("Expected", session.expectedCash?.asCurrency() ?: "—")
            Spacer(Modifier.height(Spacing.xs))
            SummaryRow("Counted", session.countedCash?.asCurrency() ?: "—")
            Spacer(Modifier.height(Spacing.xs))
            HorizontalDivider(color = PosColors.Border)
            Spacer(Modifier.height(Spacing.xs))
            SummaryRow(
                when {
                    balanced -> "Balanced"
                    variance > 0 -> "Over"
                    else -> "Short"
                },
                abs(variance).asCurrency(),
                emphasize = true,
                valueColor = accent,
            )
        }

        Spacer(Modifier.height(Spacing.lg))
        FlowPrimaryButton(text = "Done", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
    }
}

/** Matches OrderSuccessScreen's centered-white-card treatment. */
@Composable
private fun FlowCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .width(500.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(PosColors.White)
            .padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

/** Read-only mid-shift checkpoint — today's running totals for this till, including the
 * cash/card split, without closing or resetting the open register session. */
@Composable
private fun XReportCard(
    report: ZReport?,
    isLoading: Boolean,
    error: String?,
    printWarning: String?,
    onPrint: () -> Unit,
    onDismiss: () -> Unit,
) {
    FlowCard {
        IconBadge(Icons.Filled.QueryStats, PosColors.Blue500)
        Spacer(Modifier.height(Spacing.md))
        Text("X-Report", style = PosTextStyles.h3, color = PosColors.Neutral13)
        Spacer(Modifier.height(Spacing.xs))
        Text(
            "Checkpoint only — doesn't close or reset anything.",
            style = PosTextStyles.bodyMediumMedium,
            color = PosColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.lg))

        when {
            isLoading -> CircularProgressIndicator(color = PosColors.Blue500)
            error != null -> Text(error, style = PosTextStyles.bodySmallRegular, color = PosColors.Warning500, textAlign = TextAlign.Center)
            report != null -> Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .clip(RoundedCornerShape(12.dp)).background(PosColors.Surface).padding(Spacing.sm),
            ) {
                SummaryRow("Orders", report.orderCount.toString())
                Spacer(Modifier.height(Spacing.xs))
                SummaryRow("Gross sales", report.grossSales.asCurrency())
                Spacer(Modifier.height(Spacing.xs))
                SummaryRow("Net sales", report.netSales.asCurrency(), emphasize = true)
                if (report.paymentBreakdown.isNotEmpty()) {
                    Spacer(Modifier.height(Spacing.xs))
                    HorizontalDivider(color = PosColors.Border)
                    Spacer(Modifier.height(Spacing.xs))
                    report.paymentBreakdown.forEach { row ->
                        SummaryRow(row.provider, "${row.amount.asCurrency()} (${row.orders})")
                        Spacer(Modifier.height(Spacing.xxs))
                    }
                }
            }
        }

        printWarning?.let { warning ->
            Spacer(Modifier.height(Spacing.sm))
            Text("Couldn't print: $warning", style = PosTextStyles.bodyXSmallRegular, color = PosColors.Warning500, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(Spacing.lg))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            if (report != null) {
                TextButton(onClick = onPrint, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Print, contentDescription = null, tint = PosColors.Blue500, modifier = Modifier.height(18.dp))
                    Spacer(Modifier.width(Spacing.xxs))
                    Text("Print", style = PosTextStyles.bodySmallSemibold, color = PosColors.Blue500)
                }
            }
            FlowPrimaryButton(text = "Done", onClick = onDismiss, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    emphasize: Boolean = false,
    valueColor: Color = PosColors.Neutral13,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = if (emphasize) PosTextStyles.bodyMediumSemibold else PosTextStyles.bodySmallMedium, color = PosColors.Neutral13)
        Text(value, style = if (emphasize) PosTextStyles.h6 else PosTextStyles.bodySmallSemibold, color = valueColor)
    }
}
