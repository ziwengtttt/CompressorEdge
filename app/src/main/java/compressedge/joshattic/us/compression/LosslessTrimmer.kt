package compressedge.joshattic.us.compression

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/**
 * Lossless trim: stream-copies audio/video samples into a new container without any decode
 * or encode. Cuts land on keyframes (see [MediaExtractor.SEEK_TO_PREVIOUS_SYNC]), which is
 * exactly the trade-off the batch trim feature accepts.
 */
object LosslessTrimmer {

    suspend fun trim(
        context: Context,
        inputUri: Uri,
        outputPath: String,
        startMs: Long,
        endMs: Long
    ): Long = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        val pfd = context.contentResolver.openFileDescriptor(inputUri, "r")
            ?: error("Cannot open input file")
        try {
            extractor.setDataSource(pfd.fileDescriptor)
            val muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            try {
                val trackMap = HashMap<Int, Int>()
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                    if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                        extractor.selectTrack(i)
                        trackMap[i] = muxer.addTrack(format)
                    }
                }
                if (trackMap.isEmpty()) error("No audio/video track found in source")

                muxer.start()

                val startUs = startMs.coerceAtLeast(0L) * 1000L
                val endUs = if (endMs > 0L) endMs * 1000L else Long.MAX_VALUE
                // Fast seek: land on the keyframe at or before the requested start.
                extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

                val buffer = ByteBuffer.allocateDirect(2 * 1024 * 1024)
                val info = MediaCodec.BufferInfo()
                var firstPts = -1L

                while (true) {
                    val pts = extractor.sampleTime
                    if (pts < 0L) break
                    if (pts > endUs) break

                    buffer.clear()
                    val size = extractor.readSampleData(buffer, 0)
                    if (size < 0) break

                    if (firstPts < 0L) firstPts = pts
                    info.set(0, size, pts - firstPts, extractor.sampleFlags)
                    val outTrack = trackMap[extractor.sampleTrackIndex]
                    if (outTrack != null) {
                        muxer.writeSampleData(outTrack, buffer, info)
                    }
                    if (!extractor.advance()) break
                }

                muxer.stop()
            } finally {
                runCatching { muxer.release() }
            }
            File(outputPath).length()
        } finally {
            runCatching { extractor.release() }
            runCatching { pfd.close() }
        }
    }
}
