package com.cyebrcina.pos.feature.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cyebrcina.pos.R
import com.cyebrcina.pos.core.theme.PosColors
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SplashScreen(
    onNavigateToQueue: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                SplashEvent.NavigateToQueue -> onNavigateToQueue()
                SplashEvent.NavigateToLogin -> onNavigateToLogin()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PosColors.Neutral13),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.fire_hut_logo),
            contentDescription = null,
            modifier = Modifier.size(280.dp),
        )
    }
}
