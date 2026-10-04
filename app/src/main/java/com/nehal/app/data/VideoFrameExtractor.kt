package com.nehal.app.data

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class VideoFrameExtractor(private val context: Context) {

    suspend fun extractAllFrames(videoUri: Uri): List<FrameItem> = withContext(Dispatchers.IO) {
        val framesDir = File(context.cacheDir, "extracted_frames").apply {
            deleteRecursively()
            mkdirs()
        }

        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, videoUri)

        val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        val frames = mutableListOf<FrameItem>()

        // Grab frames at regular intervals (e.g. every 500ms or 1s so it doesn't run out of memory)
        val intervalUs = 500_000L // 500ms in microseconds
        val totalUs = durationMs * 1000L
        var currentUs = 0L
        var index = 0

        while (currentUs < totalUs) {
            val bitmap = retriever.getFrameAtTime(currentUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            if (bitmap != null) {
                val file = File(framesDir, "frame_$index.jpg")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                frames.add(FrameItem(id = index, file = file, isSelected = false))
                index++
            }
            currentUs += intervalUs
        }

        retriever.release()
        frames
    }
}
