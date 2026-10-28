package com.mohitarora.dualclone.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mohitarora.dualclone.R
import com.mohitarora.dualclone.core.designsystem.DcShapes
import com.mohitarora.dualclone.core.designsystem.DcSpacing
import com.mohitarora.dualclone.core.designsystem.DcTone
import com.mohitarora.dualclone.core.designsystem.components.DcBadge
import com.mohitarora.dualclone.core.designsystem.components.DcButton
import com.mohitarora.dualclone.core.designsystem.components.DcButtonStyle
import com.mohitarora.dualclone.core.designsystem.dcPressScale
import com.mohitarora.dualclone.data.model.AppItem
import com.mohitarora.dualclone.ui.viewmodel.SortOrder
import java.text.DateFormat
import java.util.Date

/** Two line card. Line one is the app name at full width, line two holds the clone badge and package. */
/** System apps and profile owners such as Island are never clones, so they get no actions and cannot be selected. */
private val AppItem.isNotCloneable: Boolean get() = isProfileSystemApp || isProfileOwner

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppCard(
    app: AppItem,
    isBulkMode: Boolean,
    isSelected: Boolean,
    actionsEnabled: Boolean,
    cloneBadge: String?,
    sortOrder: SortOrder,
    onToggleSelect: (String) -> Unit,
    onLongClick: (String) -> Unit,
    onClone: (AppItem) -> Unit,
    onRemove: (AppItem) -> Unit,
    onDisabledAction: () -> Unit,
    onAppOpsClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val selectedState = stringResource(if (isSelected) R.string.state_selected else R.string.state_not_selected)
    val dateFormat = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }
    val datePill = when (sortOrder) {
        SortOrder.RECENTLY_UPDATED -> stringResource(R.string.card_updated, dateFormat.format(Date(app.lastUpdatedTime)))
        SortOrder.INSTALL_TIME -> stringResource(R.string.card_installed, dateFormat.format(Date(app.installTime)))
        SortOrder.NAME_ASC, SortOrder.NAME_DESC -> null
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DcSpacing.md, vertical = DcSpacing.xs)
            .dcPressScale(interaction)
            .clip(DcShapes.card)
            .combinedClickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                role = if (isBulkMode) Role.Checkbox else Role.Button,
                onClickLabel = stringResource(if (isBulkMode) R.string.card_toggle_selection else R.string.card_open_special_access),
                onClick = { if (isBulkMode) { if (!app.isNotCloneable) onToggleSelect(app.packageName) } else if (app.isCloned) onAppOpsClick(app.packageName) },
                onLongClick = { if (!app.isNotCloneable) onLongClick(app.packageName) }
            )
            .semantics {
                selected = isSelected
                if (isBulkMode) stateDescription = selectedState
            },
        shape = DcShapes.card,
        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DcSpacing.md, vertical = DcSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isBulkMode && !app.isNotCloneable) {
                Icon(
                    imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(end = DcSpacing.sm).size(28.dp)
                )
            }
            AppIcon(packageName = app.packageName, contentDescription = null)
            Spacer(Modifier.width(DcSpacing.md))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DcSpacing.sm)) {
                    if (app.isNotCloneable) {
                        DcBadge(text = stringResource(if (app.isProfileOwner) R.string.badge_profile_owner else R.string.badge_system), tone = DcTone.Neutral)
                    } else if (app.isCloned && cloneBadge != null) {
                        DcBadge(text = cloneBadge, tone = DcTone.Primary)
                    }
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                if (datePill != null) {
                    Text(
                        text = datePill,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (!isBulkMode && !app.isNotCloneable) {
                Spacer(Modifier.width(DcSpacing.sm))
                // NOTE: A disabled button gives no click, so the wrapper turns the tap into an explanation.
                Box(
                    modifier = Modifier.clickable(enabled = !actionsEnabled && !app.isOperating, onClick = onDisabledAction),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = app.isOperating to app.isCloned,
                        transitionSpec = {
                            (fadeIn(tween(220)) + scaleIn(initialScale = 0.85f))
                                .togetherWith(fadeOut(tween(150)) + scaleOut(targetScale = 0.85f))
                        },
                        label = "ActionButtonTransition"
                    ) { (isOperating, isCloned) ->
                        when {
                            isOperating -> Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.5.dp)
                            }
                            isCloned -> DcButton(
                                text = stringResource(R.string.action_remove),
                                onClick = { onRemove(app) },
                                style = DcButtonStyle.DestructiveOutlined,
                                enabled = actionsEnabled
                            )
                            else -> DcButton(
                                text = stringResource(R.string.action_clone),
                                onClick = { onClone(app) },
                                style = DcButtonStyle.Tonal,
                                enabled = actionsEnabled
                            )
                        }
                    }
                }
                IconButton(onClick = { onAppOpsClick(app.packageName) }, enabled = app.isCloned) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.card_special_access_description, app.appName),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
