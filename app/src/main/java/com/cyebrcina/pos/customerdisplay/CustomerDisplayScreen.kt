package com.cyebrcina.pos.customerdisplay

import android.widget.VideoView
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.cyebrcina.pos.core.image.imageModel
import com.cyebrcina.pos.core.theme.PosColors
import com.cyebrcina.pos.core.theme.PosTextStyles
import com.cyebrcina.pos.core.theme.Spacing
import com.cyebrcina.pos.core.util.asCurrency

@Composable
fun CustomerDisplayScreen(state: CustomerDisplayState, branding: CustomerDisplayBranding) {
    Box(modifier = Modifier.fillMaxSize().background(PosColors.Primary50)) {
        BackgroundLayer(branding)

        // The idle promo video takes over the whole screen instead of the branding/logo idle
        // content whenever one's configured — anything actually happening (an order coming in,
        // a cart being built) always takes priority over it.
        if (state is CustomerDisplayState.Idle && branding.idlePromoVideoUrl != null) {
            IdlePromoVideo(branding.idlePromoVideoUrl)
        } else {
            AnimatedContent(targetState = state, label = "customer-display") { target ->
                when (target) {
                    is CustomerDisplayState.Idle -> BrandingContent(branding)
                    is CustomerDisplayState.NewOrderReceived -> NewOrderContent(target)
                    is CustomerDisplayState.BuildingOrder -> BuildingOrderContent(target, branding)
                }
            }
        }
    }
}

@Composable
private fun BackgroundLayer(branding: CustomerDisplayBranding) {
    val model = imageModel(branding.backgroundUrl) ?: return
    AsyncImage(
        model = model,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize().alpha((branding.backgroundOpacity / 100f).coerceIn(0f, 1f)),
    )
}

/** No ExoPlayer/media3 dependency in this project — [VideoView] (a thin wrapper around
 * [android.media.MediaPlayer]) is part of the Android SDK itself and is enough for a looping,
 * muted attract-mode clip. */
@Composable
private fun IdlePromoVideo(url: String) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            VideoView(context).apply {
                setVideoURI(android.net.Uri.parse(url))
                setOnPreparedListener { player ->
                    player.isLooping = true
                    player.setVolume(0f, 0f)
                    start()
                }
                // A promo clip failing to load shouldn't wedge the display — fall back to the
                // branding idle screen by just not starting playback; the Box behind this
                // AndroidView is transparent, so a failed video leaves the idle background
                // showing through rather than a black screen.
                setOnErrorListener { _, _, _ -> true }
            }
        },
    )
}

@Composable
private fun StoreLogo(logoUrl: String?, size: androidx.compose.ui.unit.Dp = 120.dp) {
    Box(
        modifier = Modifier.size(size).background(PosColors.White, RoundedCornerShape(32.dp)),
        contentAlignment = Alignment.Center,
    ) {
        val model = imageModel(logoUrl)
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(size * 0.7f),
            )
        } else {
            Icon(Icons.Filled.PointOfSale, contentDescription = null, tint = PosColors.Primary500, modifier = Modifier.size(size * 0.47f))
        }
    }
}

@Composable
private fun BrandingContent(branding: CustomerDisplayBranding) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        StoreLogo(branding.logoUrl)
        Spacer(Modifier.height(Spacing.lg))
        Text(branding.storeName.ifBlank { "Fire Hut Pizza & Wraps" }, style = PosTextStyles.h2, color = PosColors.Primary500)
    }
}

@Composable
private fun NewOrderContent(target: CustomerDisplayState.NewOrderReceived) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(120.dp).background(PosColors.Success50, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = PosColors.Success500, modifier = Modifier.size(64.dp))
        }
        Spacer(Modifier.height(Spacing.lg))
        Text("Order Received", style = PosTextStyles.h1, color = PosColors.Neutral12)
        Spacer(Modifier.height(Spacing.xxs))
        Text(target.orderNumber, style = PosTextStyles.h5, color = PosColors.Neutral8)
    }
}

/** Mirrors the cart as staff build a till order in [com.cyebrcina.pos.feature.order.create.NewOrderScreen]. */
@Composable
private fun BuildingOrderContent(target: CustomerDisplayState.BuildingOrder, branding: CustomerDisplayBranding) {
    Column(modifier = Modifier.fillMaxSize().padding(Spacing.xl)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            StoreLogo(branding.logoUrl, size = 48.dp)
            Text(branding.storeName.ifBlank { "Fire Hut Pizza & Wraps" }, style = PosTextStyles.h4, color = PosColors.Primary500)
        }
        Spacer(Modifier.height(Spacing.lg))

        if (target.items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.ShoppingCart, contentDescription = null, tint = PosColors.Primary200, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(Spacing.sm))
                    Text("Building your order…", style = PosTextStyles.h6, color = PosColors.Neutral8)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(vertical = Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(target.items) { line ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${line.quantity}× ${line.name}", style = PosTextStyles.bodyLargeMedium, color = PosColors.Neutral12)
                        Text(line.lineTotal.asCurrency(), style = PosTextStyles.bodyLargeSemibold, color = PosColors.Neutral12)
                    }
                }
            }
            HorizontalDivider(color = PosColors.Primary100)
            Spacer(Modifier.height(Spacing.sm))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("TOTAL", style = PosTextStyles.h5, color = PosColors.Neutral12)
                Text(target.total.asCurrency(), style = PosTextStyles.h5, color = PosColors.Primary500)
            }
        }
    }
}
