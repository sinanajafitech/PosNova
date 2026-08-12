package com.cyebrcina.pos.feature.report

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cyebrcina.pos.core.components.PosTopBar
import com.cyebrcina.pos.core.components.StatCard
import com.cyebrcina.pos.core.theme.PosColors
import com.cyebrcina.pos.core.theme.PosTextStyles
import com.cyebrcina.pos.core.theme.Spacing
import com.cyebrcina.pos.core.util.asCurrency
import com.cyebrcina.pos.data.remote.model.PaymentMethodBreakdown
import com.cyebrcina.pos.data.remote.model.ZReport
import com.cyebrcina.pos.data.repository.ReportChannel
import com.cyebrcina.pos.feature.order.create.FlowPrimaryButton
import com.cyebrcina.pos.printer.model.PrintJobState
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d yyyy")
private val dayLabelFormatter = DateTimeFormatter.ofPattern("EEE")

/**
 * Matches Figma's "Report" screen (node 12111:94291): stat cards with trend chips, a bar-chart
 * analytics panel, and a secondary breakdown panel. The Figma design's stat cards
 * (Total Sales/Total Product Sales/Total Customers/Net Profit) and its "Customer Activity" chart
 * assume data this API doesn't expose (item counts, customer counts, historical time series
 * beyond a single day) — remapped to the real fields `ZReport` actually has: Total Sales,
 * Orders, Refunds, Net Sales, a real 7-day gross-sales bar chart (fetched day-by-day, since the
 * API only serves one date per call), and a real Payment Breakdown panel in place of "Customer
 * Activity". Trend chips use a real day-over-day comparison, not a fabricated percentage.
 */
@Composable
fun ReportScreen(viewModel: ReportViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val report = state.report

    Scaffold(topBar = { PosTopBar(title = "Report") }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).background(PosColors.Surface)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Report", style = PosTextStyles.h3, color = PosColors.Neutral13)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    DatePeriodSelector(
                        label = dateFormatter.format(state.date),
                        onPrevious = viewModel::previousDay,
                        onNext = viewModel::nextDay,
                        nextEnabled = !state.isToday,
                    )
                    FlowPrimaryButton(
                        text = "Print Report",
                        onClick = viewModel::printReport,
                        loading = state.printJobState == PrintJobState.PRINTING,
                        leadingIcon = { Icon(Icons.Filled.Print, contentDescription = null, tint = PosColors.White, modifier = Modifier.size(18.dp)) },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                ChannelChip("All", state.channel == ReportChannel.ALL) { viewModel.setChannel(ReportChannel.ALL) }
                ChannelChip("Online", state.channel == ReportChannel.ONLINE) { viewModel.setChannel(ReportChannel.ONLINE) }
                ChannelChip("Till", state.channel == ReportChannel.TILL) { viewModel.setChannel(ReportChannel.TILL) }
            }
            Spacer(Modifier.height(Spacing.sm))

            if (report == null) {
                if (state.errorMessage != null) {
                    Text(state.errorMessage.orEmpty(), style = PosTextStyles.bodySmallRegular, color = PosColors.Warning500, modifier = Modifier.padding(Spacing.sm))
                }
                return@Scaffold
            }

            LazyColumn(contentPadding = PaddingValues(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        StatCard(
                            icon = Icons.Filled.AttachMoney,
                            iconBg = PosColors.Blue500,
                            title = "Total Sales",
                            value = report.grossSales.asCurrency(),
                            trend = state.trend(report.grossSales) { it.grossSales },
                            modifier = Modifier.weight(1f),
                        )
                        StatCard(
                            icon = Icons.Filled.Receipt,
                            iconBg = PosColors.SuccessAccent,
                            title = "Orders",
                            value = report.orderCount.toString(),
                            trend = state.trend(report.orderCount.toDouble()) { it.orderCount.toDouble() },
                            modifier = Modifier.weight(1f),
                        )
                        StatCard(
                            icon = Icons.Filled.Undo,
                            iconBg = PosColors.Amber,
                            title = "Refunds",
                            value = report.refundsTotal.asCurrency(),
                            subtitle = "${report.refundsCount} orders",
                            trend = state.trend(report.refundsTotal) { it.refundsTotal },
                            positiveIsBad = true,
                            modifier = Modifier.weight(1f),
                        )
                        StatCard(
                            icon = Icons.Filled.CreditCard,
                            iconBg = PosColors.Cyan,
                            title = "Net Sales",
                            value = report.netSales.asCurrency(),
                            trend = state.trend(report.netSales) { it.netSales },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        ReportAnalyticsCard(last7Days = state.last7Days, modifier = Modifier.weight(5f))
                        PaymentBreakdownCard(breakdown = report.paymentBreakdown, modifier = Modifier.weight(4f))
                    }
                }

                item {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(PosColors.White).padding(Spacing.sm),
                    ) {
                        Text("Summary", style = PosTextStyles.h6, color = PosColors.Neutral12)
                        Spacer(Modifier.height(Spacing.xs))
                        TotalsRow("Avg. order value", report.avgOrderValue.asCurrency())
                        TotalsRow("VAT", report.vatAmount.asCurrency())
                        TotalsRow("Net of VAT", report.netOfVat.asCurrency())
                    }
                }

                item {
                    when (state.printJobState) {
                        PrintJobState.SUCCESS -> Text("Report printed", style = PosTextStyles.bodySmallMedium, color = PosColors.Success500)
                        PrintJobState.FAILED -> Text("Couldn't print report", style = PosTextStyles.bodySmallMedium, color = PosColors.Warning500)
                        else -> {}
                    }
                }
            }
        }
    }
}

/** ALL / ONLINE / TILL — the "two versions" split requested for closing out online vs till
 * sales separately, plus the original combined view. */
@Composable
private fun ChannelChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(if (selected) PosColors.Blue500 else PosColors.SurfaceAlt)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
    ) {
        Text(label, style = PosTextStyles.bodyXSmallSemibold, color = if (selected) PosColors.White else PosColors.Neutral12)
    }
}

@Composable
private fun DatePeriodSelector(label: String, onPrevious: () -> Unit, onNext: () -> Unit, nextEnabled: Boolean) {
    Row(
        modifier = Modifier.height(48.dp).clip(RoundedCornerShape(percent = 50)).background(PosColors.SurfaceAlt).padding(horizontal = Spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous day", tint = PosColors.Neutral9, modifier = Modifier.size(18.dp))
        }
        Text(label, style = PosTextStyles.bodySmallSemibold, color = PosColors.Neutral12)
        IconButton(onClick = onNext, enabled = nextEnabled) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Next day",
                tint = if (nextEnabled) PosColors.Neutral9 else PosColors.Neutral5,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ReportAnalyticsCard(last7Days: List<DayPoint>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.height(320.dp).clip(RoundedCornerShape(20.dp)).background(PosColors.White).padding(Spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(PosColors.Blue500), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.TrendingUp, contentDescription = null, tint = PosColors.White, modifier = Modifier.size(18.dp))
            }
            Text("Report Analytics", style = PosTextStyles.h6, color = PosColors.Neutral13)
        }
        Spacer(Modifier.height(Spacing.sm))
        Text("Gross sales, last 7 days", style = PosTextStyles.bodyXSmallRegular, color = PosColors.TextSecondary)
        Spacer(Modifier.height(Spacing.sm))

        if (last7Days.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading…", style = PosTextStyles.bodySmallRegular, color = PosColors.TextSecondary)
            }
        } else {
            val maxSales = last7Days.maxOf { it.report?.grossSales ?: 0.0 }.coerceAtLeast(1.0)
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                last7Days.forEach { point ->
                    Column(modifier = Modifier.weight(1f).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                        val sales = point.report?.grossSales ?: 0.0
                        val fraction = (sales / maxSales).toFloat().coerceIn(0f, 1f)
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.6f)
                                    .fillMaxHeightFraction(fraction)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(if (point.report != null) PosColors.Blue500 else PosColors.Border),
                            )
                        }
                        Spacer(Modifier.height(Spacing.xxxs))
                        Text(dayLabelFormatter.format(point.date), style = PosTextStyles.bodyXSmallRegular, color = PosColors.TextSecondary, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

private fun Modifier.fillMaxHeightFraction(fraction: Float): Modifier =
    this.then(Modifier.fillMaxHeight(fraction.coerceIn(0.02f, 1f)))

@Composable
private fun PaymentBreakdownCard(breakdown: List<PaymentMethodBreakdown>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.height(320.dp).clip(RoundedCornerShape(20.dp)).background(PosColors.White).padding(Spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(PosColors.SuccessAccent), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.CreditCard, contentDescription = null, tint = PosColors.White, modifier = Modifier.size(18.dp))
            }
            Text("Payment Breakdown", style = PosTextStyles.h6, color = PosColors.Neutral13)
        }
        Spacer(Modifier.height(Spacing.sm))

        if (breakdown.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No payments yet", style = PosTextStyles.bodySmallRegular, color = PosColors.TextSecondary)
            }
        } else {
            val total = breakdown.sumOf { it.amount }.coerceAtLeast(0.01)
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                breakdown.forEach { line ->
                    val fraction = (line.amount / total).toFloat().coerceIn(0f, 1f)
                    Column(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(line.provider, style = PosTextStyles.bodySmallSemibold, color = PosColors.Neutral12)
                            Text("${line.amount.asCurrency()} · ${line.orders}", style = PosTextStyles.bodyXSmallRegular, color = PosColors.TextSecondary)
                        }
                        Spacer(Modifier.height(Spacing.xxxs))
                        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(percent = 50)).background(PosColors.Surface)) {
                            Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().clip(RoundedCornerShape(percent = 50)).background(PosColors.Blue500))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TotalsRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = PosTextStyles.bodySmallRegular, color = PosColors.TextSecondary)
        Text(value, style = PosTextStyles.bodySmallSemibold, color = PosColors.Neutral12)
    }
    Spacer(Modifier.height(Spacing.xxxs))
}
