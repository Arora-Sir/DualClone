package com.mohitarora.dualclone.ui.components

import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.core.graphics.drawable.toBitmap
import com.mohitarora.dualclone.core.designsystem.DcShapes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val ICON_PX = 96

private object IconCache {
    val cache = LruCache<String, ImageBitmap>(256)
}

/** Launcher icon decoded on the IO dispatcher and cached, so scrolling never touches PackageManager on the main thread. */
@Composable
fun AppIcon(
    packageName: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val context = LocalContext.current
    val bitmap by produceState(initialValue = IconCache.cache.get(packageName), packageName) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                val pm = context.packageManager
                val drawable = runCatching { pm.getApplicationIcon(packageName) }.getOrDefault(pm.defaultActivityIcon)
                drawable.toBitmap(ICON_PX, ICON_PX).asImageBitmap().also { IconCache.cache.put(packageName, it) }
            }
        }
    }
    val shape = DcShapes.field
    val loaded = bitmap
    if (loaded != null) {
        Image(
            bitmap = loaded,
            contentDescription = contentDescription,
            modifier = modifier.size(size).clip(shape)
        )
    } else {
        Box(modifier = modifier.size(size).clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHighest))
    }
}
