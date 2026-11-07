package com.vidyamandir.schoolmanager.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
        FeePayment::class,
        LateFeeRule::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun schoolDao(): SchoolDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vivekananda_vidyamandir.db"
                )
                // Keep existing school records safe. Add explicit Room migrations before any schema upgrade.
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
