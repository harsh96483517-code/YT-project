package com.tensei.ytxtube

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.io.IOException
import kotlin.concurrent.thread

/**
 * Runs downloads through yt-dlp (bundled with youtubedl-android). yt-dlp is updated from inside the
 * app (Settings -> Downloader) and automatically once a day, so YouTube changes are picked up
 * without waiting for a new app release. If yt-dlp is not ready or fails, [Downloads] falls back
 * to the built-in downloader.
 */
object YtDlpEngine {
    private const val TAG = "YtDlp"
    private const val PREFS = "ytdlp"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_NIGHTLY = "nightly"
    private const val KEY_LAST_UPDATE = "last_update"
    private const val UPDATE_INTERVAL_MS = 24L * 60 * 60 * 1000

    @Volatile
    var ready: Boolean = false
        private set

    var enabled by mutableStateOf(true)
        private set
    var nightly by mutableStateOf(false)
        private set
    var updating by mutableStateOf(false)
        private set
    var versionName by mutableStateOf<String?>(null)
        private set
    var message by mutableStateOf<String?>(null)
        private set

    fun init(context: Context) {
        val app = context.applicationContext
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        enabled = prefs.getBoolean(KEY_ENABLED, true)
        nightly = prefs.getBoolean(KEY_NIGHTLY, false)
        // Unpacking Python on first launch takes a few seconds: never on the main thread.
        thread(name = "ytdlp-init") {
            try {
                YoutubeDL.getInstance().init(app)
                FFmpeg.getInstance().init(app)
                ready = true
                versionName = runCatching { YoutubeDL.getInstance().versionName(app) }.getOrNull()
                val last = prefs.getLong(KEY_LAST_UPDATE, 0L)
                if (System.currentTimeMillis() - last > UPDATE_INTERVAL_MS) runUpdate(app, silent = true)
            } catch (t: Throwable) {
                Log.w(TAG, "yt-dlp could not start, the built-in downloader will be used", t)
            }
        }
    }

    /** Renamed from setEnabled: the `enabled` property already generates that JVM setter. */
    fun applyEnabled(value: Boolean) {
        enabled = value
        YTXTubeApplication.appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, value).apply()
    }

    fun applyNightly(value: Boolean) {
        nightly = value
        YTXTubeApplication.appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_NIGHTLY, value).putLong(KEY_LAST_UPDATE, 0L).apply()
    }

    /** Manual "update now" from Settings. */
    fun update(context: Context) {
        if (!ready || updating) return
        val app = context.applicationContext
        thread(name = "ytdlp-update") { runUpdate(app, silent = false) }
    }

    fun statusLine(): String = when {
        updating -> "Updating yt-dlp…"
        message != null -> message.orEmpty()
        versionName != null -> "Installed: ${versionName.orEmpty()} · tap to check for updates"
        ready -> "Tap to check for the latest version"
        else -> "Getting yt-dlp ready…"
    }

    private fun runUpdate(app: Context, silent: Boolean) {
        if (updating) return
        updating = true
        if (!silent) message = null
        try {
            val channel = if (nightly) YoutubeDL.UpdateChannel.NIGHTLY else YoutubeDL.UpdateChannel.STABLE
            val status = YoutubeDL.getInstance().updateYoutubeDL(app, channel)
            app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putLong(KEY_LAST_UPDATE, System.currentTimeMillis()).apply()
            versionName = runCatching { YoutubeDL.getInstance().versionName(app) }.getOrNull()
            if (!silent) {
                message = if (status?.toString()?.contains("DONE") == true) {
                    "Updated to the latest yt-dlp"
                } else {
                    "Already on the latest yt-dlp"
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "yt-dlp update failed", t)
            if (!silent) message = "Update failed. Check your internet connection and try again."
        } finally {
            updating = false
        }
    }

    /**
     * Downloads [task] to its final file ([DownloadTask.file]). [onPercent] receives 0..100 for the
     * stream yt-dlp is currently fetching. Cancelling the coroutine (Pause) stops the yt-dlp process;
     * its partial files stay on disk so a later Resume continues instead of starting over.
     */
    suspend fun download(task: DownloadTask, onPercent: (Float) -> Unit) {
        val target = task.file
        target.parentFile?.mkdirs()
        val base = target.absolutePath.removeSuffix("." + target.extension)

        val request = YoutubeDLRequest("https://www.youtube.com/watch?v=${task.videoId}")
        request.addOption("--no-playlist")
        request.addOption("--no-mtime")
        request.addOption("-o", base.replace("%", "%%") + ".%(ext)s")
        if (task.height == 0) {
            request.addOption("-f", "ba[ext=m4a]/ba/b")
            request.addOption("-x")
            request.addOption("--audio-format", "m4a")
        } else {
            val h = task.height
            request.addOption("-f", "bv*[height<=$h]+ba/b[height<=$h]/b")
            request.addOption("--merge-output-format", "mp4")
            request.addOption("--remux-video", "mp4")
        }

        val processId = "ytdlp-" + task.key
        try {
            coroutineScope {
                val job = async(Dispatchers.IO) {
                    YoutubeDL.getInstance().execute(
                        request = request,
                        processId = processId,
                        callback = { progress, _, _ -> onPercent(progress.coerceIn(0f, 100f)) },
                    )
                }
                try {
                    job.await()
                } catch (e: CancellationException) {
                    YoutubeDL.getInstance().destroyProcessById(processId)
                    throw e
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val reason = (e.message ?: "yt-dlp failed")
                .lineSequence().lastOrNull { it.contains("ERROR", ignoreCase = true) }
                ?: (e.message ?: "yt-dlp failed").lineSequence().firstOrNull().orEmpty()
            throw IOException(reason.take(180), e)
        }

        if (!target.exists() || target.length() <= 0L) {
            // yt-dlp may have picked another container: adopt the finished file if there is one.
            val alt = target.parentFile?.listFiles()?.firstOrNull {
                it.isFile && it.nameWithoutExtension == target.nameWithoutExtension &&
                    !it.name.endsWith(".part") && it.length() > 0L
            }
            if (alt != null && alt != target) alt.renameTo(target)
        }
        if (!target.exists() || target.length() <= 0L) throw IOException("yt-dlp finished but produced no file")
    }
}
