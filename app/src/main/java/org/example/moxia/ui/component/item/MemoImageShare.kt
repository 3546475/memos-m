package org.example.moxia.ui.component.item

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.moxia.R
import java.io.File
import java.io.FileOutputStream

/**
 * Composes the captured card into the image that gets previewed, saved and shared: the card on a
 * page coloured canvas with a margin, so the rounded corners have something to sit on.
 *
 * The capture arrives as a hardware bitmap, which a software canvas refuses to draw, so it is
 * flattened to an ARGB bitmap first.
 */
internal fun composeMemoCardImage(image: ImageBitmap, pageColor: Color): ImageBitmap {
    val captured = image.asAndroidBitmap()
    val card = if (captured.config == Bitmap.Config.HARDWARE) {
        captured.copy(Bitmap.Config.ARGB_8888, false)
    } else {
        captured
    }
    val margin = (card.width * 0.05f).toInt().coerceAtLeast(32)
    val output = Bitmap.createBitmap(
        card.width + margin * 2,
        card.height + margin * 2,
        Bitmap.Config.ARGB_8888
    )
    Canvas(output).apply {
        drawColor(pageColor.toArgb())
        drawBitmap(card, margin.toFloat(), margin.toFloat(), Paint(Paint.FILTER_BITMAP_FLAG))
    }
    return output.asImageBitmap()
}

/** Writes the previewed image to the provider's cache directory and opens the share sheet. */
internal suspend fun shareMemoImage(context: Context, image: ImageBitmap, memoName: String?) {
    val file = withContext(Dispatchers.IO) { writeCacheImage(context, image, memoName) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.common_share)))
}

/**
 * Saves the previewed image to the gallery: `Pictures/Moxia` through MediaStore on Android 10+,
 * an app-specific Pictures folder (indexed by the media scanner) on older releases, so neither
 * path needs a storage permission.
 */
internal suspend fun saveMemoImage(
    context: Context,
    image: ImageBitmap,
    memoName: String?
): Boolean = withContext(Dispatchers.IO) {
    val fileName = "memo-${fileStem(memoName)}-${System.currentTimeMillis()}.png"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        saveToMediaStore(context, image, fileName)
    } else {
        saveToAppPictures(context, image, fileName)
    }
}

private fun saveToMediaStore(context: Context, image: ImageBitmap, fileName: String): Boolean {
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(
            MediaStore.Images.Media.RELATIVE_PATH,
            "${Environment.DIRECTORY_PICTURES}/$GalleryFolder"
        )
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
    val written = runCatching {
        resolver.openOutputStream(uri)?.use { stream ->
            image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream)
        } ?: false
    }.getOrDefault(false)
    if (written) {
        // Clear the pending flag only once the bytes are really there, otherwise the entry shows up
        // in the gallery as a broken image.
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
    } else {
        runCatching { resolver.delete(uri, null, null) }
    }
    return written
}

private fun saveToAppPictures(context: Context, image: ImageBitmap, fileName: String): Boolean {
    val directory = File(
        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
        GalleryFolder
    ).apply { mkdirs() }
    val file = File(directory, fileName)
    val written = runCatching {
        FileOutputStream(file).use { stream ->
            image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
    }.getOrDefault(false)
    if (written) {
        // Without the scan the picture exists on disk but no gallery lists it.
        MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/png"), null)
    }
    return written
}

private fun writeCacheImage(context: Context, image: ImageBitmap, memoName: String?): File {
    // One file per share; older images are dropped so the cache cannot grow without bound.
    val directory = File(context.cacheDir, SharedDirectory).apply { mkdirs() }
    directory.listFiles()?.forEach { it.delete() }
    val file = File(directory, "memo-${fileStem(memoName)}.png")
    FileOutputStream(file).use { stream ->
        image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream)
    }
    return file
}

/** A file-name-safe stem from a memo name, e.g. `memos/abc123` -> `abc123`. */
private fun fileStem(memoName: String?): String =
    (memoName?.substringAfterLast('/') ?: "memo").take(24).ifBlank { "memo" }

private const val SharedDirectory = "shared"
private const val GalleryFolder = "Moxia"
