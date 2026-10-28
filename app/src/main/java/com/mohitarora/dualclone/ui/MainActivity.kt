package com.mohitarora.dualclone.ui

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mohitarora.dualclone.core.designsystem.LocalWindowWidthClass
import com.mohitarora.dualclone.data.repository.ThemeRepository
import com.mohitarora.dualclone.ui.navigation.DualCloneNavGraph
import com.mohitarora.dualclone.ui.theme.DualCloneTheme
import com.mohitarora.dualclone.ui.theme.resolveDark
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var themeRepository: ThemeRepository

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            val config by themeRepository.config.collectAsStateWithLifecycle()
            val dark = config.mode.resolveDark()
            // NOTE: System bar icon colours follow the in-app mode, which can differ from the device setting.
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                )
                onDispose {}
            }
            val windowSize = calculateWindowSizeClass(this)
            CompositionLocalProvider(LocalWindowWidthClass provides windowSize.widthSizeClass) {
                DualCloneTheme(themeConfig = config, darkTheme = dark) {
                    DualCloneNavGraph()
                }
            }
        }
    }
}
