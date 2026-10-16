package com.example.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.data.model.TizenPackageInfo
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.io.StringReader
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Validates and inspects Tizen .wgt packages (ZIP container with config.xml).
 */
object TizenPackageValidator {

    fun inspectWgt(context: Context, uri: Uri): Result<TizenPackageInfo> {
        return try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri)
                ?: return Result.failure(IllegalArgumentException("Cannot open file stream from selected URI"))

            // Also determine file name and size
            var fileName = "application.wgt"
            var fileSize = 0L

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                }
            }

            inspectWgtStream(inputStream, fileName, fileSize)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun inspectWgtFile(file: File): Result<TizenPackageInfo> {
        return try {
            if (!file.exists()) return Result.failure(IllegalArgumentException("File does not exist: ${file.absolutePath}"))
            file.inputStream().use { stream ->
                inspectWgtStream(stream, file.name, file.length())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun inspectWgtStream(inputStream: InputStream, fileName: String, reportedSize: Long): Result<TizenPackageInfo> {
        var configXmlBytes: ByteArray? = null
        var iconBytes: ByteArray? = null
        var hasSignature = false
        var hasAuthorSignature = false
        var calculatedSize = 0L

        try {
            ZipInputStream(inputStream).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    val entryName = entry.name.replace("\\", "/").trimStart('/')
                    when {
                        entryName.equals("config.xml", ignoreCase = true) -> {
                            configXmlBytes = zip.readBytes()
                        }
                        entryName.equals("author-signature.xml", ignoreCase = true) -> {
                            hasAuthorSignature = true
                        }
                        entryName.startsWith("signature", ignoreCase = true) && entryName.endsWith(".xml", ignoreCase = true) -> {
                            hasSignature = true
                        }
                        entryName.equals("icon.png", ignoreCase = true) || entryName.endsWith("/icon.png", ignoreCase = true) -> {
                            if (iconBytes == null) {
                                iconBytes = zip.readBytes()
                            }
                        }
                    }
                    calculatedSize += entry.compressedSize.coerceAtLeast(0)
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } catch (e: Exception) {
            return Result.failure(IllegalArgumentException("File is not a valid Tizen .wgt archive (ZIP corrupted or invalid format): ${e.message}"))
        }

        if (configXmlBytes == null) {
            return Result.failure(IllegalArgumentException("Invalid Tizen package: 'config.xml' not found in package root."))
        }

        val xmlString = String(configXmlBytes!!, Charsets.UTF_8)
        val parsedInfo = parseConfigXml(xmlString)
            ?: return Result.failure(IllegalArgumentException("Failed to parse config.xml inside the .wgt package."))

        var iconBitmap: Bitmap? = null
        if (iconBytes != null && iconBytes!!.isNotEmpty()) {
            try {
                iconBitmap = BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes!!.size)
            } catch (_: Exception) {}
        }

        val finalSize = if (reportedSize > 0) reportedSize else calculatedSize

        return Result.success(
            TizenPackageInfo(
                packageId = parsedInfo.packageId,
                name = parsedInfo.name,
                version = parsedInfo.version,
                description = parsedInfo.description,
                author = parsedInfo.author,
                appId = parsedInfo.appId,
                sizeBytes = finalSize,
                fileName = fileName,
                hasSignature = hasSignature,
                hasAuthorSignature = hasAuthorSignature,
                iconBitmap = iconBitmap
            )
        )
    }

    private data class ParsedXml(
        val packageId: String,
        val name: String,
        val version: String,
        val description: String,
        val author: String,
        val appId: String
    )

    private fun parseConfigXml(xmlContent: String): ParsedXml? {
        return try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xmlContent))

            var packageId = ""
            var version = "1.0.0"
            var name = "Tizen App"
            var description = ""
            var author = ""
            var appId = ""

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    val tagName = parser.name
                    when (tagName.lowercase()) {
                        "widget" -> {
                            packageId = parser.getAttributeValue(null, "id") ?: ""
                            version = parser.getAttributeValue(null, "version") ?: "1.0.0"
                        }
                        "name" -> {
                            name = parser.nextText().trim()
                        }
                        "description" -> {
                            description = parser.nextText().trim()
                        }
                        "author" -> {
                            author = parser.nextText().trim()
                        }
                        "application" -> {
                            val id = parser.getAttributeValue(null, "id")
                            if (!id.isNullOrBlank()) {
                                appId = id
                            }
                        }
                    }
                }
                eventType = parser.next()
            }

            if (appId.isBlank()) {
                appId = packageId
            }

            ParsedXml(
                packageId = packageId.ifBlank { "unknown.tizen.app" },
                name = name.ifBlank { packageId },
                version = version,
                description = description,
                author = author,
                appId = appId
            )
        } catch (_: Exception) {
            null
        }
    }
}
