package au.com.simplecityapps.shuttle.imageloading.glide.loader.local

import android.content.Context
import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.InputStream
import java.util.regex.Pattern

private const val ENCODED_SLASH = "%2F"

fun findLocalArtwork(
    context: Context,
    path: String,
): InputStream? {
    val standardArtworkPattern by lazy {
        Pattern.compile(
            "(\\.?(folder|cover|album|albumart|front|artwork)).*\\.(jpg|jpeg|png|webp)",
            Pattern.CASE_INSENSITIVE,
        )
    }
    val parentDocumentFile =
        if (DocumentsContract.isDocumentUri(context, path.toUri())) {
            val parent = path.substringBeforeLast(ENCODED_SLASH, "")
            if (parent.isNotEmpty()) {
                DocumentFile.fromTreeUri(context, parent.toUri())
            } else {
                null
            }
        } else {
            File(path).parentFile?.let { parent ->
                DocumentFile.fromFile(parent)
            }
        }

    val imageFiles = parentDocumentFile?.listFiles()
        ?.filter {
            it.type?.startsWith("image") == true &&
                it.length() > 1024
        }

    val standardArtworkFiles = imageFiles?.filter {
        standardArtworkPattern.matcher(it.name ?: "").matches()
    }

    // Largest image with standard name, otherwise the largest image with any name
    val finalArtworkFile = if (standardArtworkFiles?.isNotEmpty() == true) {
        standardArtworkFiles.maxByOrNull { it.length() }
    } else {
        imageFiles?.maxByOrNull { it.length() }
    }

    return finalArtworkFile?.let { documentFile ->
        context.contentResolver.openInputStream(documentFile.uri)
    }
}
