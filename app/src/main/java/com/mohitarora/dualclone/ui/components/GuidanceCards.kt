package com.mohitarora.dualclone.ui.components

import com.mohitarora.dualclone.core.designsystem.rememberReduceMotion
import com.mohitarora.dualclone.core.designsystem.DcMotion
import kotlinx.coroutines.delay
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.mohitarora.dualclone.R
import com.mohitarora.dualclone.core.designsystem.DcSpacing
import com.mohitarora.dualclone.core.designsystem.DcTone
import com.mohitarora.dualclone.core.designsystem.components.DcButton
import com.mohitarora.dualclone.core.designsystem.components.DcButtonRow
import com.mohitarora.dualclone.core.designsystem.components.DcButtonStyle
import com.mohitarora.dualclone.core.designsystem.components.DcInfoCard
import com.mohitarora.dualclone.data.model.ShizukuState
import com.mohitarora.dualclone.ui.external.ExternalIntents

private val CardPadding = Modifier.padding(horizontal = DcSpacing.lg, vertical = DcSpacing.xs)

/** Shown when Samsung Secure Folder is tapped. It explains why cloning is not possible and opens the app itself. */
@Composable
fun KnoxGuidanceCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    DcInfoCard(
        title = stringResource(R.string.knox_title),
        body = stringResource(R.string.knox_body),
        tone = DcTone.Warning,
        icon = Icons.Default.Lock,
        modifier = modifier.then(CardPadding)
    ) {
        DcButton(
            text = stringResource(R.string.knox_open),
            onClick = { ExternalIntents.openSecureFolder(context) },
            style = DcButtonStyle.Tonal
        )
    }
}

/** Shown when no clone target exists. The settings button only appears on Samsung devices. */
@Composable
fun EmptyProfileCard(isSamsung: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    DcInfoCard(
        title = stringResource(R.string.noprofile_title),
        body = stringResource(if (isSamsung) R.string.noprofile_body_samsung else R.string.noprofile_body_other),
        tone = DcTone.Info,
        icon = Icons.Default.Info,
        modifier = modifier.then(CardPadding)
    ) {
        if (isSamsung) {
            DcButton(
                text = stringResource(R.string.noprofile_open_settings),
                onClick = { ExternalIntents.openDualMessengerSettings(context) },
                style = DcButtonStyle.Tonal
            )
        }
    }
}

/** Time a problem state must hold before its card appears. Cold start passes through short lived states. */
private const val GUIDANCE_GRACE_MILLIS = 1500L

/**
 * Inline guidance when Shizuku is not ready, replacing the old tiny badge as the only hint.
 * Cold start passes through Checking and sometimes a brief Unavailable before the binder arrives, and a card for those
 * states used to pop in and vanish within two seconds. The header badge already shows Checking, so the card waits
 * until a real problem state has held for a grace period, then fades in. It also fades out when Shizuku becomes ready.
 */
@Composable
fun ShizukuGuidanceCard(
    state: ShizukuState,
    onOpenSetup: () -> Unit,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reduceMotion = rememberReduceMotion()
    var settled by remember { mutableStateOf<ShizukuState?>(null) }
    var last by remember { mutableStateOf<ShizukuState>(ShizukuState.Unavailable) }
    LaunchedEffect(state) {
        if (state is ShizukuState.Ready || state is ShizukuState.Checking) {
            settled = null
        } else {
            delay(GUIDANCE_GRACE_MILLIS)
            last = state
            settled = state
        }
    }
    AnimatedVisibility(
        visible = settled != null,
        enter = if (reduceMotion) EnterTransition.None else fadeIn(tween(DcMotion.standardMillis)) + expandVertically(),
        exit = if (reduceMotion) ExitTransition.None else fadeOut(tween(DcMotion.fastMillis)) + shrinkVertically()
    ) {
        ShizukuGuidanceBody(last, onOpenSetup, onRequestPermission, modifier)
    }
}

@Composable
private fun ShizukuGuidanceBody(
    state: ShizukuState,
    onOpenSetup: () -> Unit,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val (title, body) = when (state) {
        is ShizukuState.PermissionRequired -> R.string.guidance_permission_title to R.string.guidance_permission_body
        is ShizukuState.User95Missing -> R.string.guidance_dual_title to R.string.guidance_dual_body
        else -> R.string.guidance_stopped_title to R.string.guidance_stopped_body
    }
    DcInfoCard(
        title = stringResource(title),
        body = stringResource(body),
        tone = DcTone.Warning,
        icon = Icons.Default.Warning,
        modifier = modifier.then(CardPadding)
    ) {
        DcButtonRow {
            if (state is ShizukuState.PermissionRequired) {
                DcButton(
                    text = stringResource(R.string.guidance_grant),
                    onClick = onRequestPermission,
                    modifier = Modifier.slot()
                )
            } else {
                DcButton(
                    text = stringResource(R.string.profiles_shizuku_open),
                    onClick = { ExternalIntents.openShizuku(context) },
                    modifier = Modifier.slot()
                )
            }
            DcButton(
                text = stringResource(R.string.profiles_shizuku_setup),
                onClick = onOpenSetup,
                style = DcButtonStyle.Outlined,
                modifier = Modifier.slot()
            )
        }
    }
}
