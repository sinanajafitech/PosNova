package com.cyebrcina.pos.feature.staffselect

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
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
import com.cyebrcina.pos.core.components.EmptyState
import com.cyebrcina.pos.core.components.InitialsAvatar
import com.cyebrcina.pos.core.components.NumericKeypad
import com.cyebrcina.pos.core.components.PinDots
import com.cyebrcina.pos.core.components.PosTopBar
import com.cyebrcina.pos.core.theme.PosColors
import com.cyebrcina.pos.core.theme.PosTextStyles
import com.cyebrcina.pos.core.theme.Spacing
import com.cyebrcina.pos.data.remote.model.DeviceStaffMember
import com.cyebrcina.pos.feature.order.create.FlowPrimaryButton

private const val PIN_DOTS_LENGTH = 8

/**
 * Shown once per app session, right after login (both a fresh sign-in and a silent splash
 * re-auth) — a staff member picks their name from the grid, then confirms it's them with their
 * PIN, before the till's dashboard is reachable. Identify-only (see
 * [SelectStaffViewModel] doc) — doesn't touch shift/clock status, just tells the rest of the
 * app who's using this till right now for order/register attribution.
 */
@Composable
fun SelectStaffScreen(onStaffConfirmed: () -> Unit, viewModel: SelectStaffViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { PosTopBar(title = "Select Staff") }) { padding ->
        Row(Modifier.fillMaxSize().padding(padding)) {
            Box(Modifier.weight(1f).fillMaxHeight()) {
                when {
                    state.isLoadingStaff -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PosColors.Blue500)
                    }
                    state.loadError != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            icon = Icons.Filled.Person,
                            title = "Couldn't load staff",
                            description = state.loadError.orEmpty(),
                            action = {
                                FlowPrimaryButton(text = "Retry", onClick = viewModel::loadStaff)
                            },
                        )
                    }
                    state.staff.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            icon = Icons.Filled.Person,
                            title = "No staff configured",
                            description = "Add staff members in Admin before using this till.",
                        )
                    }
                    else -> LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        contentPadding = PaddingValues(Spacing.lg),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(state.staff, key = { it.id }) { member ->
                            StaffCard(
                                member = member,
                                selected = member.id == state.selectedStaffId,
                                onClick = { viewModel.onStaffSelected(member.id) },
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.width(360.dp).fillMaxHeight().padding(Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    state.selectedStaff?.name ?: "Select your name",
                    style = PosTextStyles.h4,
                    color = PosColors.Neutral13,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    "Enter your PIN",
                    style = PosTextStyles.bodyMediumRegular,
                    color = PosColors.TextSecondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Spacing.lg))

                PinDots(length = PIN_DOTS_LENGTH, filled = state.pin.length)
                Spacer(Modifier.height(Spacing.sm))
                state.pinError?.let {
                    Text(it, style = PosTextStyles.bodySmallSemibold, color = PosColors.Danger, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(Spacing.sm))
                }

                Spacer(Modifier.height(Spacing.lg))
                NumericKeypad(
                    onDigit = viewModel::onDigit,
                    onBackspace = viewModel::onBackspace,
                    modifier = Modifier.width(280.dp),
                )

                Spacer(Modifier.height(Spacing.lg))
                FlowPrimaryButton(
                    text = "Confirm",
                    onClick = { viewModel.onConfirm(onStaffConfirmed) },
                    enabled = state.canConfirm,
                    loading = state.isVerifying,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun StaffCard(member: DeviceStaffMember, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) PosColors.Blue50 else PosColors.White)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) PosColors.Blue500 else PosColors.Border,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        InitialsAvatar(name = member.name, size = 56.dp)
        Spacer(Modifier.height(Spacing.xs))
        Text(member.name, style = PosTextStyles.bodyMediumSemibold, color = PosColors.Neutral13, textAlign = TextAlign.Center)
        member.role?.let {
            Text(it, style = PosTextStyles.bodyXSmallRegular, color = PosColors.TextSecondary, textAlign = TextAlign.Center)
        }
    }
}
