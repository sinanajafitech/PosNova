package com.cyebrcina.pos.feature.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cyebrcina.pos.core.components.AppCard
import com.cyebrcina.pos.core.components.PosTopBar
import com.cyebrcina.pos.core.components.SecondaryButton
import com.cyebrcina.pos.core.theme.PosColors
import com.cyebrcina.pos.core.theme.PosTextStyles
import com.cyebrcina.pos.core.theme.Spacing
import com.cyebrcina.pos.core.util.asCurrency
import com.cyebrcina.pos.core.util.asDateTime
import com.cyebrcina.pos.core.util.toInstantOrNull
import com.cyebrcina.pos.data.remote.model.CashRegisterSessionDto
import com.cyebrcina.pos.feature.order.create.FlowPrimaryButton
import kotlin.math.abs

/**
 * Open the drawer for the shift with a counted starting float, or close it with a physical
 * cash count that gets reconciled against this till's own CASH sales since opening (see
 * RegisterViewModel / POST api/device/register/close). Reachable from Settings, not a
 * persistent tab, since it's a start/end-of-shift action rather than something used mid-shift.
 */
@Composable
fun RegisterScreen(onBack: () -> Unit, viewModel: RegisterViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { PosTopBar(title = "Cash Register", onBack = onBack, navIcon = Icons.AutoMirrored.Filled.ArrowBack) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(Spacing.sm).verticalScroll(rememberScrollState()),
        ) {
            state.error?.let { message ->
                Text(
                    message,
                    style = PosTextStyles.bodySmallRegular,
                    color = PosColors.Warning500,
                    modifier = Modifier.padding(bottom = Spacing.sm),
                )
            }

            state.justClosed?.let { closed ->
                ClosedSummaryCard(closed, onDismiss = viewModel::dismissClosedSummary)
                Spacer(Modifier.height(Spacing.lg))
            }

            when {
                state.session != null -> CloseRegisterCard(state.session!!, state.isSubmitting, onClose = viewModel::closeRegister)
                !state.isLoading -> OpenRegisterCard(state.isSubmitting, onOpen = viewModel::openRegister)
            }
        }
    }
}

@Composable
private fun OpenRegisterCard(isSubmitting: Boolean, onOpen: (Double) -> Unit) {
    var floatAmount by remember { mutableStateOf("") }

    Text("Open Register", style = PosTextStyles.h6, color = PosColors.Neutral12)
    Spacer(Modifier.height(Spacing.xxs))
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Count the starting cash in the drawer before the shift begins.",
            style = PosTextStyles.bodySmallRegular,
            color = PosColors.Neutral7,
        )
        Spacer(Modifier.height(Spacing.sm))
        OutlinedTextField(
            value = floatAmount,
            onValueChange = { floatAmount = it.filter { c -> c.isDigit() || c == '.' } },
            label = { Text("Starting float (£)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.sm))
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
private fun CloseRegisterCard(session: CashRegisterSessionDto, isSubmitting: Boolean, onClose: (Double) -> Unit) {
    var countedCash by remember { mutableStateOf("") }

    Text("Register Open", style = PosTextStyles.h6, color = PosColors.Neutral12)
    Spacer(Modifier.height(Spacing.xxs))
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Opening float", style = PosTextStyles.bodySmallRegular, color = PosColors.Neutral7)
                Text(session.openingFloat.asCurrency(), style = PosTextStyles.bodyMediumSemibold, color = PosColors.Neutral12)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Opened", style = PosTextStyles.bodySmallRegular, color = PosColors.Neutral7)
                Text(
                    session.openedAt.toInstantOrNull()?.asDateTime() ?: session.openedAt,
                    style = PosTextStyles.bodyMediumSemibold,
                    color = PosColors.Neutral12,
                )
            }
        }
        session.openedByStaffName?.let {
            Spacer(Modifier.height(Spacing.xxs))
            Text("Opened by $it", style = PosTextStyles.bodyXSmallRegular, color = PosColors.Neutral7)
        }
    }

    Spacer(Modifier.height(Spacing.lg))
    Text("Close Register", style = PosTextStyles.h6, color = PosColors.Neutral12)
    Spacer(Modifier.height(Spacing.xxs))
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Count the cash in the drawer now — it'll be checked against this till's own cash sales since opening.",
            style = PosTextStyles.bodySmallRegular,
            color = PosColors.Neutral7,
        )
        Spacer(Modifier.height(Spacing.sm))
        OutlinedTextField(
            value = countedCash,
            onValueChange = { countedCash = it.filter { c -> c.isDigit() || c == '.' } },
            label = { Text("Counted cash (£)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.sm))
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
private fun ClosedSummaryCard(session: CashRegisterSessionDto, onDismiss: () -> Unit) {
    val variance = session.variance ?: 0.0
    val varianceColor = if (abs(variance) < 0.01) PosColors.Success500 else PosColors.Warning500

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Text("Register Closed", style = PosTextStyles.h6, color = PosColors.Neutral12)
        Spacer(Modifier.height(Spacing.sm))
        SummaryRow("Expected", session.expectedCash?.asCurrency() ?: "—")
        SummaryRow("Counted", session.countedCash?.asCurrency() ?: "—")
        SummaryRow(
            when {
                abs(variance) < 0.01 -> "Balanced"
                variance > 0 -> "Over"
                else -> "Short"
            },
            abs(variance).asCurrency(),
            valueColor = varianceColor,
        )
        Spacer(Modifier.height(Spacing.sm))
        SecondaryButton(text = "Dismiss", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun SummaryRow(label: String, value: String, valueColor: Color = PosColors.Neutral12) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = PosTextStyles.bodySmallRegular, color = PosColors.Neutral7)
        Text(value, style = PosTextStyles.bodyMediumSemibold, color = valueColor)
    }
}
