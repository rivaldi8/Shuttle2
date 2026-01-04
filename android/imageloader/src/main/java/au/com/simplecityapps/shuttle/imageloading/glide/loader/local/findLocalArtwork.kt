package au.com.simplecityapps.shuttle.imageloading.glide.loader.local

import android.content.Context
import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.InputStream
import java.util.regex.Pattern

fun findLocalArtwork(
    context: Context,
    path: String,
): InputStream? {
    val pattern by lazy {
        Pattern.compile(
            "(\\.?(folder|cover|album|albumart|front|artwork)).*\\.(jpg|jpeg|png|webp)",
            Pattern.CASE_INSENSITIVE,
        )
    }
    val parentDocumentFile =
        if (DocumentsContract.isDocumentUri(context, path.toUri())) {
            val parent = path.substringBeforeLast("%2F", "")
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

    return parentDocumentFile?.listFiles()
        ?.filter {
            it.type?.startsWith("image") == true &&
                it.length() > 1024 &&
                pattern.matcher(it.name ?: "").matches()
        }
        ?.maxByOrNull { it.length() }
        ?.let { documentFile ->
            // noinspection Recycle. To be closed by the client (LocalArtworkDataFetcher)
            context.contentResolver.openInputStream(documentFile.uri)
        }
}
