package com.cyebrcina.pos.core.navigation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cyebrcina.pos.core.theme.PosColors
import com.cyebrcina.pos.core.theme.PosTextStyles

/**
 * Adaptive shell for the post-auth "main" graph: a [NavigationRail] on POS-terminal / tablet
 * widths (matches the kit's desktop Sidebar), a bottom [NavigationBar] on phone widths (matches
 * the kit's mobile bottom nav) — one screen implementation per flow, reflowed by width rather
 * than duplicated per Figma desktop/mobile frame.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun MainScaffold(
    windowSizeClass: WindowSizeClass,
    currentSection: MainSection,
    onSectionSelected: (MainSection) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    val useRail = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

    if (useRail) {
        Row(modifier = modifier.fillMaxSize()) {
            NavigationRail(containerColor = PosColors.White) {
                MainSection.entries.forEach { section ->
                    NavigationRailItem(
                        selected = section == currentSection,
                        onClick = { onSectionSelected(section) },
                        icon = { Icon(section.icon, contentDescription = section.label) },
                        label = { Text(section.label, style = PosTextStyles.bodyXSmallMedium) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = PosColors.Blue500,
                            selectedTextColor = PosColors.Blue500,
                            indicatorColor = PosColors.Blue50,
                            unselectedIconColor = PosColors.Neutral7,
                            unselectedTextColor = PosColors.Neutral7,
                        ),
                    )
                }
            }
            content(Modifier.fillMaxSize())
        }
    } else {
        Scaffold(
            modifier = modifier,
            bottomBar = {
                NavigationBar(containerColor = PosColors.White) {
                    MainSection.entries.forEach { section ->
                        NavigationBarItem(
                            selected = section == currentSection,
                            onClick = { onSectionSelected(section) },
                            icon = { Icon(section.icon, contentDescription = section.label) },
                            label = { Text(section.label, style = PosTextStyles.bodyXSmallMedium) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PosColors.Blue500,
                                selectedTextColor = PosColors.Blue500,
                                indicatorColor = PosColors.Blue50,
                                unselectedIconColor = PosColors.Neutral7,
                                unselectedTextColor = PosColors.Neutral7,
                            ),
                        )
                    }
                }
            },
        ) { padding ->
            content(Modifier.fillMaxSize().padding(padding))
        }
    }
}
