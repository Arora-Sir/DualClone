package com.mohitarora.dualclone.ui.screen.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Holds the seven tap unlock for this process, so the More screen sees what Settings unlocked. */
object DiagnosticsGate {
    var unlocked by mutableStateOf(false)
}
