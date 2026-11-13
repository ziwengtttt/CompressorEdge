package compressedge.joshattic.us.model

import android.net.Uri

data class QueueItem(
    val uri: Uri,
    val originalSize: Long = 0L,
    val originalWidth: Int = 0,
    val originalHeight: Int = 0,
    val originalBitrate: Int = 0,
    val originalAudioBitrate: Int = 0,
    val originalFps: Float = 30f,
    val originalVideoMime: String? = null,
    val durationMs: Long = 0L,
    val originalName: String? = null,
    // Trim range in milliseconds. trimEndMs == 0 means "until the end of the video".
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L,
    // When true the clip is stream-copied instead of re-encoded (no quality loss, no heat).
    val losslessTrim: Boolean = false,
    
    // Status
    val isCompressing: Boolean = false,
    val isCompleted: Boolean = false,
    val progress: Float = 0f,
    val compressedUri: Uri? = null,
    val compressedSize: Long = 0L,
    val currentOutputSize: Long = 0L,
    val error: String? = null,

    // Specific Overrides (null means fallback to global state)
    val targetSizePercentageOverride: Float? = null,
    val targetResolutionHeightOverride: Int? = null,
    val targetFpsOverride: Int? = null,
    val audioBitrateOverride: Int? = null,
    val removeAudioOverride: Boolean? = null,
    val audioVolumeOverride: Float? = null
) {
    val isVertical: Boolean get() = originalHeight > originalWidth

    /** Duration actually exported after trimming. Falls back to the source duration. */
    val trimmedDurationMs: Long
        get() {
            if (durationMs <= 0L) return 0L
            val start = trimStartMs.coerceIn(0L, durationMs)
            val end = if (trimEndMs > 0L) trimEndMs.coerceIn(0L, durationMs) else durationMs
            return (end - start).coerceAtLeast(0L)
        }

    val hasTrim: Boolean
        get() = trimStartMs > 0L || (trimEndMs in 1 until durationMs)
}
