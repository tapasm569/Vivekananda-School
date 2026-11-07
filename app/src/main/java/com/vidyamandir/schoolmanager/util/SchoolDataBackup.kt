package com.vidyamandir.schoolmanager.util

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.vidyamandir.schoolmanager.data.AppDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object SchoolDataBackup {
    private const val DB_NAME = "vivekananda_vidyamandir.db"
    private const val PROFILE_ENTRY = "school_profile.properties"
    private const val MAX_SIZE = 150L * 1024L * 1024L

    fun createBackup(context: Context, uri: Uri) {
        val dbFile = context.getDatabasePath(DB_NAME)
        require(dbFile.exists()) { "School database not found" }

        val db = AppDatabase.getDatabase(
            context,
            kotlinx.coroutines.CoroutineScope(
                kotlinx.coroutines.Dispatchers.IO
            )
        )

        db.openHelper.writableDatabase
            .query("PRAGMA wal_checkpoint(FULL)").use { }

        val properties = Properties()
        val prefs = context.getSharedPreferences(
            "vv_school_profile", Context.MODE_PRIVATE
        )

        prefs.all.forEach { (key, value) ->
            when (value) {
                is String -> properties.setProperty("s:$key", value)
                is Int -> properties.setProperty("i:$key", value.toString())
                is Float -> properties.setProperty("f:$key", value.toString())
                is Long -> properties.setProperty("l:$key", value.toString())
                is Boolean -> properties.setProperty("b:$key", value.toString())
            }
        }

        context.contentResolver.openOutputStream(uri)?.use { output ->
            ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry(DB_NAME))
                FileInputStream(dbFile).use { it.copyTo(zip) }
                zip.closeEntry()

                zip.putNextEntry(ZipEntry(PROFILE_ENTRY))
                properties.store(zip, "Vivekananda Vidyamandir profile")
                zip.closeEntry()

                zip.putNextEntry(ZipEntry("backup_info.txt"))
                zip.write("Vivekananda Vidyamandir\nDatabase version: 4\n".toByteArray())
                zip.closeEntry()
            }
        } ?: error("Cannot create backup file")
    }

    fun restoreBackup(context: Context, uri: Uri) {
        val tempDir = File(context.cacheDir, "restore_staging")
        tempDir.deleteRecursively()
        check(tempDir.mkdirs()) { "Cannot create staging folder" }

        val stagedDb = File(tempDir, DB_NAME)
        val stagedProfile = File(tempDir, PROFILE_ENTRY)
        var totalBytes = 0L

        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        val destination = when (entry.name) {
                            DB_NAME -> stagedDb
                            PROFILE_ENTRY -> stagedProfile
                            else -> null
                        }

                        if (destination != null) {
                            FileOutputStream(destination).use { output ->
                                val buffer = ByteArray(8192)
                                while (true) {
                                    val count = zip.read(buffer)
                                    if (count < 0) break
                                    totalBytes += count
                                    require(totalBytes <= MAX_SIZE) {
                                        "Backup file is too large"
                                    }
                                    output.write(buffer, 0, count)
                                }
                            }
                        }
                        zip.closeEntry()
                    }
                }
            } ?: error("Cannot read selected backup")

            require(stagedDb.exists() && stagedDb.length() > 0) {
                "Database missing from backup"
            }

            val header = ByteArray(16)
            FileInputStream(stagedDb).use {
                require(it.read(header) == 16) { "Invalid database file" }
            }
            require(String(header, Charsets.US_ASCII).startsWith("SQLite format 3")) {
                "Invalid SQLite database"
            }

            SQLiteDatabase.openDatabase(
                stagedDb.absolutePath, null, SQLiteDatabase.OPEN_READONLY
            ).use { sqlite ->
                sqlite.rawQuery("PRAGMA integrity_check", null).use { cursor ->
                    require(cursor.moveToFirst() && cursor.getString(0) == "ok") {
                        "Database integrity check failed"
                    }
                }

                sqlite.rawQuery("PRAGMA user_version", null).use { cursor ->
                    require(cursor.moveToFirst() && cursor.getInt(0) == 4) {
                        "Incompatible database version"
                    }
                }

                val requiredTables = setOf(
                    "classes", "teachers", "students", "exams",
                    "exam_subjects", "exam_results", "fee_payments",
                    "late_fee_rules"
                )
                val foundTables = mutableSetOf<String>()

                sqlite.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table'", null
                ).use { cursor ->
                    while (cursor.moveToNext()) foundTables.add(cursor.getString(0))
                }

                require(foundTables.containsAll(requiredTables)) {
                    "Backup does not contain all school data tables"
                }
            }

            val properties = Properties()
            if (stagedProfile.exists()) {
                FileInputStream(stagedProfile).use { properties.load(it) }
            }

            val currentDb = context.getDatabasePath(DB_NAME)
            val safetyFile = File(context.cacheDir, "school_database_before_restore.db")
            val profilePrefs = context.getSharedPreferences(
                "vv_school_profile", Context.MODE_PRIVATE
            )
            val oldProfile = profilePrefs.all.toMap()

            val db = AppDatabase.getDatabase(
                context,
                kotlinx.coroutines.CoroutineScope(
                    kotlinx.coroutines.Dispatchers.IO
                )
            )
            db.openHelper.writableDatabase
                .query("PRAGMA wal_checkpoint(FULL)").use { }

            AppDatabase.closeDatabase()

            if (currentDb.exists()) {
                currentDb.copyTo(safetyFile, overwrite = true)
            }

            try {
                File(currentDb.absolutePath + "-wal").delete()
                File(currentDb.absolutePath + "-shm").delete()
                stagedDb.copyTo(currentDb, overwrite = true)

                val editor = profilePrefs.edit().clear()
                properties.stringPropertyNames().forEach { property ->
                    val value = properties.getProperty(property)
                    when {
                        property.startsWith("s:") ->
                            editor.putString(property.substring(2), value)
                        property.startsWith("i:") ->
                            editor.putInt(property.substring(2), value.toInt())
                        property.startsWith("f:") ->
                            editor.putFloat(property.substring(2), value.toFloat())
                        property.startsWith("l:") ->
                            editor.putLong(property.substring(2), value.toLong())
                        property.startsWith("b:") ->
                            editor.putBoolean(property.substring(2), value.toBoolean())
                    }
                }

                check(editor.commit()) { "Could not restore school settings" }

                context.getSharedPreferences(
                    "vv_auth_session", Context.MODE_PRIVATE
                ).edit().clear().commit()

            } catch (error: Exception) {
                if (safetyFile.exists()) {
                    safetyFile.copyTo(currentDb, overwrite = true)
                }

                val rollback = profilePrefs.edit().clear()
                oldProfile.forEach { (key, value) ->
                    when (value) {
                        is String -> rollback.putString(key, value)
                        is Int -> rollback.putInt(key, value)
                        is Float -> rollback.putFloat(key, value)
                        is Long -> rollback.putLong(key, value)
                        is Boolean -> rollback.putBoolean(key, value)
                    }
                }
                rollback.commit()
                throw error
            }
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
