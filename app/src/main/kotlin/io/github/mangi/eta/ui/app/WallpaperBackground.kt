package io.github.mangi.eta.ui.app

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.mangi.eta.core.AndroidAgentLogger
import io.github.mangi.eta.core.safeLogType
import io.github.mangi.eta.data.model.WallpaperScaleMode
import io.github.mangi.eta.data.model.WallpaperSettings
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/**
 * 壁纸文件仓库：把用户从相册选中的图片复制到 App 私有目录，避免长期持有 content:// 授权，
 * 也避免升级或系统清理后壁纸丢失。文件名固定为 current，用修改时间判断是否需要重新解码。
 */
internal object WallpaperStore {
    private const val DIRECTORY = "wallpaper"
    private const val FILE_NAME = "current"
    private const val MAX_DECODE_PX = 2048

    /** 壁纸文件版本号；导入/清除后递增，驱动上层重新解码。 */
    private val revisionState = MutableStateFlow(0L)
    val revisionFlow: StateFlow<Long> = revisionState

    fun directory(context: Context): File =
        File(context.filesDir, DIRECTORY).apply { if (!isDirectory) mkdirs() }

    fun file(context: Context): File = File(directory(context), FILE_NAME)

    fun isSet(context: Context): Boolean = file(context).let { it.isFile && it.length() > 0L }

    fun refresh(context: Context) {
        revisionState.value = revisionState.value + 1L
    }

    fun currentRevision(context: Context): Long = revisionState.value

    /** 复制失败或不是可解码图片时返回 false，不抛出，调用方只负责提示。 */
    suspend fun import(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val target = file(context)
            val temp = File(directory(context), "$FILE_NAME.tmp")
            val copied = context.contentResolver.openInputStream(uri)?.use { input ->
                temp.outputStream().use { output -> input.copyTo(output) }
                true
            } ?: false
            if (!copied) {
                temp.delete()
                return@withContext false
            }
            if (target.exists()) target.delete()
            if (!temp.renameTo(target)) {
                temp.copyTo(target, overwrite = true)
                temp.delete()
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(target.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                target.delete()
                return@withContext false
            }
            refresh(context)
            true
        } catch (error: Throwable) {
            AndroidAgentLogger.error("Wallpaper import failed: type=${error.safeLogType()}")
            false
        }
    }

    fun clear(context: Context) {
        runCatching { file(context).delete() }
        runCatching { File(directory(context), "$FILE_NAME.tmp").delete() }
        refresh(context)
    }

    suspend fun load(context: Context): ImageBitmap? = withContext(Dispatchers.IO) {
        val file = file(context)
        if (!file.isFile) return@withContext null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_DECODE_PX) sample *= 2
        runCatching {
            BitmapFactory.decodeFile(
                file.absolutePath,
                BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                },
            )
        }.getOrNull()?.asImageBitmap()
    }
}

/** 解析当前壁纸位图；壁纸关闭时返回 null，不占用内存。 */
@Composable
internal fun rememberWallpaperBitmap(enabled: Boolean): ImageBitmap? {
    val context = LocalContext.current
    val revision by WallpaperStore.revisionFlow.collectAsState()
    val bitmap by produceState<ImageBitmap?>(
        initialValue = null,
        enabled,
        revision,
    ) {
        value = if (enabled) WallpaperStore.load(context) else null
    }
    return bitmap
}

/**
 * 聊天页壁纸背景：只画图片 + 遮罩，不包裹任何 content。
 * 基础遮罩固定为 colorScheme.surface.copy(alpha = 0.25f) 常驻，用于保证正文对比度；
 * 用户可调的 dim 遮罩作为独立层叠加，二者互不替代。
 */
@Composable
internal fun ChatWallpaperBackdrop(
    wallpaper: WallpaperSettings,
    bitmap: ImageBitmap,
    surfaceColor: Color,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (wallpaper.blurRadius > 0f) {
                        Modifier.blur(radius = wallpaper.blurRadius.dp)
                    } else {
                        Modifier
                    },
                ),
            contentScale = when (wallpaper.scaleMode) {
                WallpaperScaleMode.CROP -> ContentScale.Crop
                WallpaperScaleMode.FIT -> ContentScale.Fit
            },
        )
        // 基础遮罩：surface 色 25% 透明度常驻，不随主题开关、不可关闭。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(surfaceColor.copy(alpha = 0.25f)),
        )
        // 用户可调 dim 遮罩：独立叠加，保留原有设置读写逻辑。
        if (wallpaper.dim > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = wallpaper.dim.coerceIn(0f, 0.85f))),
            )
        }
    }
}
