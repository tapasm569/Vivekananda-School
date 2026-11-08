package com.vidyamandir.schoolmanager.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        SchoolClass::class,
        Teacher::class,
        Student::class,
        ExamDefinition::class,
        ExamSubject::class,
        ExamResult::class,
        ExamSubjectComponent::class,
        ExamResultComponent::class,
        FeePayment::class,
        LateFeeRule::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun schoolDao(): SchoolDao

    companion object {

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE classes ADD COLUMN subjects TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `exam_subject_components` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `examSubjectId` INTEGER NOT NULL,
                    `componentName` TEXT NOT NULL,
                    `maxMarks` INTEGER NOT NULL,
                    `position` INTEGER NOT NULL,
                    FOREIGN KEY(`examSubjectId`) REFERENCES `exam_subjects`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE)"""
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_exam_subject_components_examSubjectId` ON `exam_subject_components` (`examSubjectId`)"
                )
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `exam_result_components` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `examResultId` INTEGER NOT NULL,
                    `componentName` TEXT NOT NULL,
                    `maxMarks` INTEGER NOT NULL,
                    `obtainedMarks` INTEGER NOT NULL,
                    `position` INTEGER NOT NULL,
                    FOREIGN KEY(`examResultId`) REFERENCES `exam_results`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE)"""
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_exam_result_components_examResultId` ON `exam_result_components` (`examResultId`)"
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE fee_payments ADD COLUMN waivedFineAmount REAL NOT NULL DEFAULT 0.0"
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vivekananda_vidyamandir.db"
                )
                .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                .fallbackToDestructiveMigrationOnDowngrade()
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        fun closeDatabase() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        val dao = database.schoolDao()
                        dao.insertLateFeeRule(LateFeeRule(dayFrom = 1, dayTo = 4, fineAmount = 0.0))
                        dao.insertLateFeeRule(LateFeeRule(dayFrom = 5, dayTo = 9, fineAmount = 20.0))
                        dao.insertLateFeeRule(LateFeeRule(dayFrom = 10, dayTo = 19, fineAmount = 40.0))
                        dao.insertLateFeeRule(LateFeeRule(dayFrom = 20, dayTo = 29, fineAmount = 60.0))
                        dao.insertLateFeeRule(LateFeeRule(dayFrom = 30, dayTo = 9999, fineAmount = 100.0))
                    }
                }
            }
        }
    }
}
