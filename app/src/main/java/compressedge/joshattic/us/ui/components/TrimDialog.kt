package compressedge.joshattic.us.ui.components

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import compressedge.joshattic.us.model.QueueItem
import compressedge.joshattic.us.utils.formatDuration

/**
 * Trim editor for one queued video.
 *
 * Dragging the start handle seeks the preview to that position; dragging the end handle
 * seeks back to the start handle and plays from there, so you can see what the cut keeps.
 */
@OptIn(UnstableApi::class)
@Composable
fun TrimDialog(
    item: QueueItem,
    onDismiss: () -> Unit,
    onApply: (Long, Long) -> Unit
) {
    val context = LocalContext.current
    val duration = item.durationMs.coerceAtLeast(1L)
    val minGap = 500L

    var startMs by remember { mutableStateOf(item.trimStartMs.coerceIn(0L, duration)) }
    var endMs by remember {
        val initialEnd = if (item.trimEndMs > 0L) item.trimEndMs else duration
        val minEnd = (startMs + minGap).coerceAtMost(duration)
        mutableStateOf(initialEnd.coerceIn(minEnd, duration))
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(item.uri))
            prepare()
        }
    }
    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "裁剪 ✂",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = false
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("开始", style = MaterialTheme.typography.labelMedium)
                        Text(
                            formatDuration(startMs),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = startMs.toFloat(),
                        onValueChange = { value ->
                            startMs = value.toLong().coerceIn(0L, (endMs - minGap).coerceAtLeast(0L))
                            exoPlayer.seekTo(startMs)
                        },
                        onValueChangeFinished = { exoPlayer.play() },
                        valueRange = 0f..duration.toFloat()
                    )
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("结束", style = MaterialTheme.typography.labelMedium)
                        Text(
                            formatDuration(endMs),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = endMs.toFloat(),
                        onValueChange = { value ->
                            endMs = value.toLong().coerceIn((startMs + minGap).coerceAtMost(duration), duration)
                        },
                        onValueChangeFinished = {
                            // Preview the retained range from its start, as requested.
                            exoPlayer.seekTo(startMs)
                            exoPlayer.play()
                        },
                        valueRange = 0f..duration.toFloat()
                    )
                }

                Text(
                    text = "保留 ${formatDuration(startMs)} → ${formatDuration(endMs)} " +
                        "· ${formatDuration(endMs - startMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Button(onClick = { onApply(startMs, endMs) }) { Text("应用裁剪") }
                }
            }
        }
    }
}
