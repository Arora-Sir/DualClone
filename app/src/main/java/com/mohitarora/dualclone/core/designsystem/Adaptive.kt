package com.mohitarora.dualclone.core.designsystem

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.compositionLocalOf

/** Width class of the window, provided once by MainActivity. Compact is the safe default for previews and tests. */
val LocalWindowWidthClass = compositionLocalOf { WindowWidthSizeClass.Compact }
