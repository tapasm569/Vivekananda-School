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
        FeePayment::class,
        LateFeeRule::class,
        ExamResult::class
    ],
    version = 2,
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
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
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
                        // Initial Default Classes
                        dao.insertClass(SchoolClass(className = "Class V", monthlyFee = 450.0, sections = "A,B"))
                        dao.insertClass(SchoolClass(className = "Class VI", monthlyFee = 480.0, sections = "A,B"))
                        dao.insertClass(SchoolClass(className = "Class VII", monthlyFee = 500.0, sections = "A,B"))
                        dao.insertClass(SchoolClass(className = "Class VIII", monthlyFee = 550.0, sections = "A,B"))

                        // Initial Default Late Rules
                        dao.insertLateFeeRule(LateFeeRule(dayFrom = 1, dayTo = 10, fineAmount = 0.0))
                        dao.insertLateFeeRule(LateFeeRule(dayFrom = 11, dayTo = 20, fineAmount = 50.0))
                        dao.insertLateFeeRule(LateFeeRule(dayFrom = 21, dayTo = 30, fineAmount = 100.0))
                    }
                }
            }
        }
    }
}
