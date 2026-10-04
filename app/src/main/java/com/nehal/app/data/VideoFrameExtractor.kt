package com.nehal.app.data

import android.content.Context
import android.net.Uri
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
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

        // Copy content Uri to temporary cache file so FFmpeg has direct filesystem access
        val tempVideoFile = File(context.cacheDir, "input_temp_video.mp4")
        context.contentResolver.openInputStream(videoUri)?.use { input ->
            FileOutputStream(tempVideoFile).use { output ->
                input.copyTo(output)
            }
        }

        val outputPattern = File(framesDir, "frame_%05d.jpg").absolutePath
        val command = "-y -i \"${tempVideoFile.absolutePath}\" \"$outputPattern\""

        val session = FFmpegKit.execute(command)

        // Clean up temporary source video
        tempVideoFile.delete()

        if (ReturnCode.isSuccess(session.returnCode)) {
            framesDir.listFiles { _, name -> name.endsWith(".jpg") }
                ?.sortedBy { it.name }
                ?.mapIndexed { index, file ->
                    FrameItem(
                        id = index,
                        file = file,
                        isSelected = false
                    )
                } ?: emptyList()
        } else {
            throw IllegalStateException("FFmpeg failed: ${session.failStackTrace ?: "Unknown error"}")
        }
    }
}
