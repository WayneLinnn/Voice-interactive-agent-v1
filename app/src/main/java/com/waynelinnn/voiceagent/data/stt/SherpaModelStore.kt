package com.waynelinnn.voiceagent.data.stt

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream

sealed interface SttModelState {
    data object Missing : SttModelState
    data class Downloading(val progress: Float) : SttModelState
    data object Ready : SttModelState
    data class Failed(val message: String) : SttModelState
}

/**
 * Downloads SenseVoice int8 (zh/en/…) into app filesDir for Sherpa-ONNX.
 */
@Singleton
class SherpaModelStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val mutex = Mutex()
    private val downloadClient = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.MINUTES)
        .writeTimeout(2, java.util.concurrent.TimeUnit.MINUTES)
        .callTimeout(15, java.util.concurrent.TimeUnit.MINUTES)
        .build()

    private val _state = MutableStateFlow<SttModelState>(
        if (isModelPresent()) SttModelState.Ready else SttModelState.Missing,
    )
    val state: StateFlow<SttModelState> = _state.asStateFlow()

    val modelDir: File
        get() = File(context.filesDir, "sherpa/$MODEL_DIR_NAME")

    val modelFile: File
        get() = File(modelDir, "model.int8.onnx")

    val tokensFile: File
        get() = File(modelDir, "tokens.txt")

    fun isModelPresent(): Boolean {
        if (!modelFile.exists() || !tokensFile.exists()) return false
        // Guard against corrupted adb/PowerShell copies (UTF-16 BOM, wrong size).
        if (modelFile.length() < MIN_MODEL_BYTES) return false
        if (tokensFile.length() < MIN_TOKENS_BYTES) return false
        return runCatching {
            tokensFile.inputStream().use { input ->
                val bom0 = input.read()
                val bom1 = input.read()
                // Reject UTF-16 LE/BE BOM produced by broken Windows redirects.
                if (bom0 == 0xFF && bom1 == 0xFE) return@use false
                if (bom0 == 0xFE && bom1 == 0xFF) return@use false
                true
            }
        }.getOrDefault(false)
    }

    suspend fun ensureReady(): Boolean = mutex.withLock {
        if (isModelPresent()) {
            _state.value = SttModelState.Ready
            return true
        }
        // Corrupt leftovers block Ready UI; wipe and re-download.
        if (modelFile.exists() || tokensFile.exists()) {
            runCatching { modelFile.delete() }
            runCatching { tokensFile.delete() }
            _state.value = SttModelState.Missing
        }
        return downloadAndExtract()
    }

    private suspend fun downloadAndExtract(): Boolean = withContext(Dispatchers.IO) {
        try {
            _state.value = SttModelState.Downloading(0f)
            val archive = File(context.cacheDir, "sense-voice-int8.tar.bz2")
            download(MODEL_URL, archive) { progress ->
                _state.value = SttModelState.Downloading(progress)
            }
            modelDir.mkdirs()
            extractNeededFiles(archive, modelDir)
            archive.delete()
            val ok = isModelPresent()
            _state.value = if (ok) {
                SttModelState.Ready
            } else {
                SttModelState.Failed("Model files missing after extract")
            }
            ok
        } catch (error: Exception) {
            _state.value = SttModelState.Failed(error.message ?: "Model download failed")
            false
        }
    }

    private fun download(url: String, dest: File, onProgress: (Float) -> Unit) {
        val request = Request.Builder().url(url).build()
        downloadClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("HTTP ${response.code} downloading STT model")
            }
            val body = response.body ?: error("Empty body")
            val total = body.contentLength().coerceAtLeast(1L)
            body.byteStream().use { input ->
                FileOutputStream(dest).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var readTotal = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        readTotal += read
                        onProgress((readTotal.toFloat() / total.toFloat()).coerceIn(0f, 1f))
                    }
                }
            }
        }
    }

    private fun extractNeededFiles(archive: File, destDir: File) {
        archive.inputStream().use { fileIn ->
            BZip2CompressorInputStream(fileIn).use { bzIn ->
                TarArchiveInputStream(bzIn).use { tarIn ->
                    while (true) {
                        val entry = tarIn.nextEntry ?: break
                        val name = File(entry.name).name
                        if (entry.isDirectory) continue
                        if (name != "model.int8.onnx" && name != "tokens.txt") continue
                        val outFile = File(destDir, name)
                        FileOutputStream(outFile).use { output ->
                            tarIn.copyTo(output)
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val MODEL_DIR_NAME = "sherpa-onnx-sense-voice-zh-en-ja-ko-yue-int8-2024-07-17"
        const val MODEL_URL =
            "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/" +
                "sherpa-onnx-sense-voice-zh-en-ja-ko-yue-int8-2024-07-17.tar.bz2"
        private const val MIN_MODEL_BYTES = 100L * 1024L * 1024L
        private const val MIN_TOKENS_BYTES = 10L * 1024L
    }
}
