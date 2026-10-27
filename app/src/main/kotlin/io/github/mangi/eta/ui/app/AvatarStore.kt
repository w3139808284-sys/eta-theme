package io.github.mangi.eta.ui.app

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import io.github.mangi.eta.core.AndroidAgentLogger
import io.github.mangi.eta.core.safeLogType
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 头像文件仓库：把用户选中的图片复制进 App 私有目录，避免长期持有 content:// 授权。
 *
 * 复用壁纸已验证的导入方式（复制 + 可解码校验 + 失败回滚），
 * 但独立成目录，互不影响。
 */
object AvatarStore {
    private const val DIRECTORY = "avatar"
    private const val USER_FILE = "user"
    private const val AI_FILE = "ai"

    enum class Target(val fileName: String) {
        USER(USER_FILE),
        AI(AI_FILE),
    }

    fun directory(context: Context): File =
        File(context.filesDir, DIRECTORY).apply { if (!isDirectory) mkdirs() }

    fun file(context: Context, target: Target): File = File(directory(context), target.fileName)

    fun isSet(context: Context, target: Target): Boolean =
        file(context, target).let { it.isFile && it.length() > 0L }

    /** 复制失败或不是可解码图片时返回 null，不抛出。 */
    suspend fun import(context: Context, target: Target, uri: Uri): String? =
        withContext(Dispatchers.IO) {
            try {
                val targetFile = file(context, target)
                val temp = File(directory(context), "${target.fileName}.tmp")
                val copied = context.contentResolver.openInputStream(uri)?.use { input ->
                    temp.outputStream().use { output -> input.copyTo(output) }
                    true
                } ?: false
                if (!copied) {
                    temp.delete()
                    return@withContext null
                }
                if (targetFile.exists()) targetFile.delete()
                if (!temp.renameTo(targetFile)) {
                    temp.copyTo(targetFile, overwrite = true)
                    temp.delete()
                }
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(targetFile.absolutePath, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                    targetFile.delete()
                    return@withContext null
                }
                targetFile.absolutePath
            } catch (error: Throwable) {
                AndroidAgentLogger.error("Avatar import failed: type=${error.safeLogType()}")
                null
            }
        }

    fun clear(context: Context, target: Target) {
        runCatching { file(context, target).delete() }
        runCatching { File(directory(context), "${target.fileName}.tmp").delete() }
    }

    /**
     * 保存已裁剪的正方形位图，返回文件绝对路径。
     *
     * 长边限制在 [MAX_SAVE_PX]，避免把相册原图体积带进私有目录。
     */
    suspend fun saveCropped(
        context: Context,
        target: Target,
        bitmap: android.graphics.Bitmap,
    ): String? = withContext(Dispatchers.IO) {
        try {
            val scaled = scaleDown(bitmap, MAX_SAVE_PX)
            val targetFile = file(context, target)
            val temp = File(directory(context), "${target.fileName}.tmp")
            temp.outputStream().use { out ->
                scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, out)
            }
            if (scaled !== bitmap) scaled.recycle()
            if (targetFile.exists()) targetFile.delete()
            if (!temp.renameTo(targetFile)) {
                temp.copyTo(targetFile, overwrite = true)
                temp.delete()
            }
            targetFile.absolutePath
        } catch (error: Throwable) {
            AndroidAgentLogger.error("Avatar save failed: type=${error.safeLogType()}")
            null
        }
    }

    private fun scaleDown(bitmap: android.graphics.Bitmap, maxPx: Int): android.graphics.Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= maxPx) return bitmap
        val ratio = maxPx.toFloat() / longest
        return android.graphics.Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt().coerceAtLeast(1),
            (bitmap.height * ratio).toInt().coerceAtLeast(1),
            true,
        )
    }

    private const val MAX_SAVE_PX = 512
}
