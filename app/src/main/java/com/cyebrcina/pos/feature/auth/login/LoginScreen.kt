package com.cyebrcina.pos.feature.auth.login

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cyebrcina.pos.core.components.AppTextField
import com.cyebrcina.pos.core.components.PrimaryButton
import com.cyebrcina.pos.core.theme.PosColors
import com.cyebrcina.pos.core.theme.PosTextStyles
import com.cyebrcina.pos.core.theme.Spacing
import com.cyebrcina.pos.feature.auth.common.AuthScaffold
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                LoginEvent.LoggedIn -> onLoggedIn()
            }
        }
    }

    AuthScaffold(title = "Till Sign In", subtitle = "Enter this device's Fire Hut credentials") {
        Spacer(Modifier.height(Spacing.lg))
        AppTextField(
            value = state.username,
            onValueChange = viewModel::onUsernameChange,
            label = "Username",
            placeholder = "till-front-counter",
        )
        Spacer(Modifier.height(Spacing.sm))
        AppTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = "Password",
            placeholder = "••••••••",
            isPassword = true,
        )
        if (state.errorMessage != null) {
            Spacer(Modifier.height(Spacing.xxs))
            Text(text = state.errorMessage.orEmpty(), style = PosTextStyles.bodyXSmallMedium, color = PosColors.Warning500)
        }
        Spacer(Modifier.height(Spacing.sm))
        PrimaryButton(
            text = "Sign in",
            onClick = viewModel::submit,
            loading = state.isLoading,
            enabled = state.username.isNotBlank() && state.password.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
