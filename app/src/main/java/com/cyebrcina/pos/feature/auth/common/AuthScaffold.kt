package com.cyebrcina.pos.feature.auth.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cyebrcina.pos.core.theme.PosColors
import com.cyebrcina.pos.core.theme.PosTextStyles
import com.cyebrcina.pos.core.theme.Spacing

/**
 * Shared frame for every auth screen (Splash/Login/PIN/Register/Forgot/Reset): centered card
 * on a teal brand background, matching the kit's Onboarding frames. Works at any width — a
 * narrow card on phones, the same card centered with side padding on the D4's wider display.
 */
@Composable
fun AuthScaffold(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PosColors.Primary50),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .padding(Spacing.lg)
                .verticalScroll(rememberScrollState())
                .background(PosColors.White, RoundedCornerShape(24.dp))
                .padding(Spacing.xl),
        ) {
            Text(text = title, style = PosTextStyles.h3, color = PosColors.Neutral12)
            Spacer(Modifier.height(Spacing.xxxs))
            Text(text = subtitle, style = PosTextStyles.bodySmallRegular, color = PosColors.Neutral7)
            Spacer(Modifier.height(Spacing.lg))
            content()
        }
    }
}
