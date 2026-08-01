package net.prezz.mpr.model.external.cache

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

import net.prezz.mpr.ui.ApplicationActivator
import android.content.Context
import android.database.Cursor
import android.util.Log

class CoverCache {

    private val context: Context
    private val coverCacheDatabaseHelper: CoverCacheDatabaseHelper

    init {
        context = ApplicationActivator.context
        coverCacheDatabaseHelper = CoverCacheDatabaseHelper(context)
    }

    fun getCoverUrl(artist: String?, album: String?): String? {
        try {
            val cursor = coverCacheDatabaseHelper.getCoverUrl(artist, album)
            try {
                if (cursor.moveToFirst()) {
                    val url = cursor.getString(0)
                    return url
                }
            } finally {
                cursor.close()
                coverCacheDatabaseHelper.close()
            }
        } catch (ex: Exception) {
            Log.e(CoverCache::class.java.name, "Error getting cover url", ex)
        }

        return null
    }

    fun insertCoverUrl(artist: String?, album: String?, coverUrl: String?) {
        try {
            coverCacheDatabaseHelper.insertCoverUrl(artist, album, coverUrl)
        } catch (ex: Exception) {
            Log.e(CoverCache::class.java.name, "Error inserting cover url", ex)
        } finally {
            coverCacheDatabaseHelper.close()
        }
    }

    fun deleteCoverUrl(artist: String?, album: String?) {
        try {
            coverCacheDatabaseHelper.deleteCoverUrl(artist, album)
        } catch (ex: Exception) {
            Log.e(CoverCache::class.java.name, "Error deleting cover url", ex)
        } finally {
            coverCacheDatabaseHelper.close()
        }
    }

    @Throws(IOException::class)
    fun getCoverImage(coverUrl: String?): ByteArray? {
        try {
            val filename = getCoverFile(coverUrl)
            if (filename != null) {
                return readFile(File(filename))
            }
        } catch (ex: Exception) {
            Log.e(CoverCache::class.java.name, "Error loading cover image", ex)
        }

        return null
    }

    @Throws(IOException::class)
    fun insertCoverImage(coverUrl: String?, imageData: ByteArray?) {
        try {
            val cacheDir = context.cacheDir
            val coverFile = File(cacheDir, UUID.randomUUID().toString())
            writeFile(coverFile, imageData)

            coverCacheDatabaseHelper.insertCoverFile(coverUrl, coverFile.absolutePath)
        } catch (ex: Exception) {
            Log.e(CoverCache::class.java.name, "Error saving cover image", ex)
        } finally {
            coverCacheDatabaseHelper.close()
        }
    }

    fun deleteCoverImageIfLastUsage(coverUrl: String?) {
        try {
            var useCount = 0
            val cursor = coverCacheDatabaseHelper.getUrlUseCount(coverUrl)
            try {
                if (cursor.moveToFirst()) {
                    useCount = cursor.getInt(0)
                }
            } finally {
                cursor.close()
            }

            if (useCount <= 1) {
                val filename = getCoverFile(coverUrl)
                coverCacheDatabaseHelper.deleteCoverFile(coverUrl)
                if (filename != null) {
                    val file = File(filename)
                    file.delete()
                }
            }
        } catch (ex: Exception) {
            Log.e(CoverCache::class.java.name, "Error deleting cover image", ex)
        } finally {
            coverCacheDatabaseHelper.close()
        }
    }

    @Throws(IOException::class)
    private fun getCoverFile(coverUrl: String?): String? {
        val cursor = coverCacheDatabaseHelper.getCoverFile(coverUrl)
        try {
            if (cursor.moveToFirst()) {
                val filename = cursor.getString(0)
                return filename
            }
        } finally {
            cursor.close()
            coverCacheDatabaseHelper.close()
        }

        return null
    }

    @Throws(IOException::class)
    private fun writeFile(coverFile: File, imageData: ByteArray?) {
        val outputStream = FileOutputStream(coverFile)
        try {
            outputStream.write(imageData)
            outputStream.flush()
        } finally {
            outputStream.close()
        }
    }

    @Throws(IOException::class)
    private fun readFile(coverFile: File): ByteArray? {
        if (coverFile.exists()) {
            val inputStream = FileInputStream(coverFile)
            try {
                val outputStream = ByteArrayOutputStream()

                val buffer = ByteArray(2048)
                while (true) {
                    val len = inputStream.read(buffer, 0, buffer.size)
                    if (len == -1) break
                    outputStream.write(buffer, 0, len)
                }

                outputStream.flush()
                return outputStream.toByteArray()
            } finally {
                inputStream.close()
            }
        }
        return null
    }
}
