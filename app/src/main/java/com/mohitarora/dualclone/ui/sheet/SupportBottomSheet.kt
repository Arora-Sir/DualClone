package com.mohitarora.dualclone.ui.sheet

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mohitarora.dualclone.R
import com.mohitarora.dualclone.core.designsystem.DcShapes
import com.mohitarora.dualclone.core.designsystem.DcSpacing
import com.mohitarora.dualclone.core.designsystem.components.DcButton
import com.mohitarora.dualclone.core.designsystem.components.DcButtonRow
import com.mohitarora.dualclone.core.designsystem.components.DcButtonStyle
import com.mohitarora.dualclone.core.designsystem.components.DcCard
import com.mohitarora.dualclone.core.designsystem.components.DcSheet
import com.mohitarora.dualclone.core.designsystem.rememberDcHaptics
import com.mohitarora.dualclone.ui.external.ExternalIntents
import com.mohitarora.dualclone.ui.external.ProjectLinks
import kotlinx.coroutines.launch

/** Support tiers. Each tier is one equal height button row and the UPI ID comes from [ProjectLinks]. */
@Composable
fun SupportBottomSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = rememberDcHaptics()
    val localHost = remember { SnackbarHostState() }
    var qrEnlarged by rememberSaveable { mutableStateOf(false) }

    val copiedMessage = stringResource(R.string.support_upi_copied)
    val noUpiMessage = stringResource(R.string.support_upi_no_app)

    DcSheet(onDismiss = onDismiss, title = stringResource(R.string.support_title)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(DcSpacing.sm))
            Text(
                text = stringResource(R.string.support_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = stringResource(R.string.support_upi_heading),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() }
        )
        DcCard(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(DcSpacing.sm)
            ) {
                Box(
                    modifier = Modifier
                        .size(if (qrEnlarged) 240.dp else 112.dp)
                        .clip(DcShapes.field)
                        .background(Color.White)
                        .clickable(role = Role.Button) { qrEnlarged = !qrEnlarged }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.support_upi_qr),
                        contentDescription = stringResource(
                            if (qrEnlarged) R.string.support_upi_qr_large_description else R.string.support_upi_qr_description
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Icon(
                    painter = painterResource(R.drawable.ic_brand_upi),
                    contentDescription = stringResource(R.string.support_upi_logo_description),
                    tint = Color.Unspecified,
                    modifier = Modifier
                        .width(68.dp)
                        .size(width = 68.dp, height = 18.dp)
                )
                Text(
                    text = ProjectLinks.UPI_ID,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.support_upi_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        DcButtonRow {
            DcButton(
                text = stringResource(R.string.support_upi_pay),
                onClick = {
                    if (!ExternalIntents.payWithUpi(context)) {
                        ExternalIntents.copyUpiId(context)
                        haptics.success()
                        scope.launch { localHost.showSnackbar(noUpiMessage) }
                    }
                },
                modifier = Modifier.slot()
            )
            DcButton(
                text = stringResource(R.string.support_upi_copy),
                onClick = {
                    ExternalIntents.copyUpiId(context)
                    haptics.success()
                    scope.launch { localHost.showSnackbar(copiedMessage) }
                },
                style = DcButtonStyle.Outlined,
                modifier = Modifier.slot()
            )
        }

        Text(
            text = stringResource(R.string.support_international_heading),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(top = DcSpacing.sm)
                .semantics { heading() }
        )
        DcButtonRow {
            DcButton(
                text = stringResource(R.string.support_paypal),
                onClick = { ExternalIntents.openUrl(context, ProjectLinks.PAYPAL) },
                style = DcButtonStyle.Outlined,
                icon = { Icon(painterResource(R.drawable.ic_brand_paypal), contentDescription = null) },
                modifier = Modifier.slot()
            )
            DcButton(
                text = stringResource(R.string.support_sponsors),
                onClick = { ExternalIntents.openUrl(context, ProjectLinks.SPONSORS) },
                style = DcButtonStyle.Tonal,
                icon = { Icon(painterResource(R.drawable.ic_brand_github), contentDescription = null) },
                modifier = Modifier.slot()
            )
        }
        SnackbarHost(localHost)
    }
}
