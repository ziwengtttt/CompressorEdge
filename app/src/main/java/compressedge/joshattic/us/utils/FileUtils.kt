package compressedge.joshattic.us.utils

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import java.util.Locale

fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 MB"
    val mb = size / (1024.0 * 1024.0)
    if (mb >= 1000) {
        return String.format(Locale.US, "%.1f GB", mb / 1024)
    }
    return String.format(Locale.US, "%.1f MB", mb)
}

/** Formats milliseconds as mm:ss or h:mm:ss. */
fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) {
        String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.US, "%02d:%02d", m, s)
    }
}

/**
 * Resolves a human readable display name for a picked video Uri.
 *
 * Third-party pickers (and some OEM galleries) return content Uris where
 * [OpenableColumns.DISPLAY_NAME] is either missing or replaced by a numeric MediaStore id.
 * The old code took that value verbatim, which is why an output was named e.g. "5108.mp4".
 */
fun resolveDisplayName(context: Context, uri: Uri): String? {
    // 1. Standard OpenableColumns lookup (MediaStore and SAF documents both expose this).
    runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    val name = cursor.getString(index)
                    if (isUsableDisplayName(name)) return name
                }
            }
        }
    }

    val lastSegment = uri.lastPathSegment
    val mediaId = lastSegment?.substringAfterLast(':')?.toLongOrNull()
        ?: lastSegment?.toLongOrNull()

    // 2. The path may carry a MediaStore id; look the real name up by id.
    if (mediaId != null) {
        runCatching {
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Video.Media.DISPLAY_NAME),
                "${MediaStore.Video.Media._ID} = ?",
                arrayOf(mediaId.toString()),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val name = cursor.getString(0)
                    if (isUsableDisplayName(name)) return name
                }
            }
        }
    }

    // 3. Last resort: a non-numeric path segment can still be a real filename.
    if (!lastSegment.isNullOrBlank() && lastSegment.toLongOrNull() == null &&
        !lastSegment.substringBeforeLast(".").matches(Regex("\\d+"))
    ) {
        return lastSegment
    }
    return null
}

/** True when a candidate is a real filename rather than a bare numeric id. */
fun isUsableDisplayName(name: String?): Boolean {
    if (name.isNullOrBlank()) return false
    val stem = name.substringBeforeLast(".")
    return stem.isNotBlank() && !stem.matches(Regex("\\d+"))
}
