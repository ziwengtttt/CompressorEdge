package compressedge.joshattic.us.ui.components

import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import compressedge.joshattic.us.utils.formatFileSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private val VIDEO_EXTENSIONS =
    setOf("mp4", "mkv", "mov", "avi", "3gp", "webm", "m4v", "ts", "flv", "wmv")

private fun File.isVideoFile(): Boolean =
    !isDirectory && extension.lowercase() in VIDEO_EXTENSIONS

/**
 * In-app folder browser used with the "All files access" permission. Unlike the system
 * document picker it can select every video in a folder at once.
 */
@Composable
fun FileBrowserDialog(
    onDismiss: () -> Unit,
    onConfirm: (List<Uri>) -> Unit
) {
    val root = remember { Environment.getExternalStorageDirectory() }
    var currentDir by remember { mutableStateOf(root) }
    var entries by remember { mutableStateOf<List<File>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(currentDir) {
        loading = true
        entries = withContext(Dispatchers.IO) {
            currentDir.listFiles()
                ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                ?: emptyList()
        }
        loading = false
    }

    val videosHere = entries.filter { it.isVideoFile() }
    val allSelected = videosHere.isNotEmpty() && videosHere.all { it.absolutePath in selected }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        val parent = currentDir.parentFile
                        if (parent != null && currentDir.absolutePath != root.absolutePath) {
                            currentDir = parent
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "上一级")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (currentDir.absolutePath == root.absolutePath) "内部存储" else currentDir.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = currentDir.absolutePath,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    TextButton(onClick = onDismiss) { Text("取消") }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "已选 ${selected.size}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        enabled = videosHere.isNotEmpty(),
                        onClick = {
                            val paths = videosHere.map { it.absolutePath }.toSet()
                            selected = if (allSelected) selected - paths else selected + paths
                        }
                    ) {
                        Text(if (allSelected) "取消全选" else "全选视频")
                    }
                }

                HorizontalDivider()

                Box(modifier = Modifier.weight(1f)) {
                    if (loading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(entries, key = { it.absolutePath }) { file ->
                                FileRow(
                                    file = file,
                                    selected = file.absolutePath in selected,
                                    onOpenDir = { currentDir = file },
                                    onToggle = {
                                        selected = if (file.absolutePath in selected) {
                                            selected - file.absolutePath
                                        } else {
                                            selected + file.absolutePath
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        enabled = selected.isNotEmpty(),
                        onClick = { onConfirm(selected.map { Uri.fromFile(File(it)) }) }
                    ) {
                        Text("添加 ${selected.size} 个视频")
                    }
                }
            }
        }
    }
}

@Composable
private fun FileRow(
    file: File,
    selected: Boolean,
    onOpenDir: () -> Unit,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (file.isDirectory) onOpenDir() else onToggle() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.Movie,
            contentDescription = null,
            tint = if (file.isDirectory) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(file.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            if (!file.isDirectory) {
                Text(
                    formatFileSize(file.length()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (!file.isDirectory) {
            Checkbox(checked = selected, onCheckedChange = { onToggle() })
        }
    }
}
