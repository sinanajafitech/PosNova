package com.cyebrcina.pos.feature.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cyebrcina.pos.core.components.AppCard
import com.cyebrcina.pos.core.components.PosTopBar
import com.cyebrcina.pos.core.components.SecondaryButton
import com.cyebrcina.pos.core.theme.PosColors
import com.cyebrcina.pos.core.theme.PosTextStyles
import com.cyebrcina.pos.core.theme.Spacing
import com.cyebrcina.pos.core.util.asCurrency
import com.cyebrcina.pos.data.remote.model.PaymentMethodBreakdown
import com.cyebrcina.pos.printer.model.PrintJobState
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d yyyy")

@Composable
fun ReportScreen(viewModel: ReportViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val report = state.report

    Scaffold(topBar = { PosTopBar(title = "Z-Report") }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = viewModel::previousDay) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous day", tint = PosColors.Neutral9)
                }
                Text(dateFormatter.format(state.date), style = PosTextStyles.bodyMediumSemibold, color = PosColors.Neutral12)
                IconButton(onClick = viewModel::nextDay, enabled = !state.isToday) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next day",
                        tint = if (state.isToday) PosColors.Neutral5 else PosColors.Neutral9,
                    )
                }
            }

            if (report == null) {
                if (state.errorMessage != null) {
                    Text(state.errorMessage.orEmpty(), style = PosTextStyles.bodySmallRegular, color = PosColors.Warning500, modifier = Modifier.padding(Spacing.sm))
                }
                return@Scaffold
            }

            LazyColumn(contentPadding = PaddingValues(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        StatCard("Orders", report.orderCount.toString(), Modifier.weight(1f))
                        StatCard("Gross Sales", report.grossSales.asCurrency(), Modifier.weight(1f))
                        StatCard("Net Sales", report.netSales.asCurrency(), Modifier.weight(1f))
                    }
                }
                item {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        TotalsRow("Avg. order value", report.avgOrderValue.asCurrency())
                        TotalsRow("Refunds", "-${report.refundsTotal.asCurrency()} (${report.refundsCount})")
                        TotalsRow("VAT", report.vatAmount.asCurrency())
                        TotalsRow("Net of VAT", report.netOfVat.asCurrency())
                    }
                }
                item { Text("Payment Breakdown", style = PosTextStyles.h6, color = PosColors.Neutral12) }
                items(report.paymentBreakdown) { PaymentBreakdownRow(it) }
                item {
                    when (state.printJobState) {
                        PrintJobState.SUCCESS -> Text("Report printed", style = PosTextStyles.bodySmallMedium, color = PosColors.Success500)
                        PrintJobState.FAILED -> Text("Couldn't print report", style = PosTextStyles.bodySmallMedium, color = PosColors.Warning500)
                        else -> {}
                    }
                }
                item {
                    SecondaryButton(
                        text = "Print Z-Report",
                        onClick = viewModel::printReport,
                        loading = state.printJobState == PrintJobState.PRINTING,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Filled.Print, contentDescription = null) },
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier) {
        Text(label, style = PosTextStyles.bodyXSmallRegular, color = PosColors.Neutral7)
        Spacer(Modifier.height(Spacing.xxxs))
        Text(value, style = PosTextStyles.h6, color = PosColors.Neutral12)
    }
}

@Composable
private fun TotalsRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = PosTextStyles.bodySmallRegular, color = PosColors.Neutral7)
        Text(value, style = PosTextStyles.bodySmallSemibold, color = PosColors.Neutral12)
    }
}

@Composable
private fun PaymentBreakdownRow(breakdown: PaymentMethodBreakdown) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(breakdown.provider, style = PosTextStyles.bodySmallSemibold, color = PosColors.Neutral12)
                Text("${breakdown.orders} orders", style = PosTextStyles.bodyXSmallRegular, color = PosColors.Neutral7)
            }
            Text(breakdown.amount.asCurrency(), style = PosTextStyles.bodySmallSemibold, color = PosColors.Primary500)
        }
    }
}
