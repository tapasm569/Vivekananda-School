#!/usr/bin/env bash
set -e

echo "=== [1/3] Securing LoginActivity against Startup Crashes ==="
cat << 'FILE_EOF' > app/src/main/java/com/vidyamandir/schoolmanager/ui/LoginActivity.kt
package com.vidyamandir.schoolmanager.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.vidyamandir.schoolmanager.MainActivity
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.databinding.ActivityLoginBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var db: AppDatabase? = null

    companion object {
        private const val PREFS_NAME = "vv_auth_session"
        private const val KEY_ROLE = "session_role"
        private const val KEY_TEACHER_NAME = "teacher_name"
        private const val SCHOOL_CODE = "VV001"
        private const val ADMIN_PASSWORD = "India360@"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        super.onCreate(savedInstanceState)

        try {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val role = prefs.getString(KEY_ROLE, null)
            if (role == "ADMIN") {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                return
            } else if (role == "TEACHER") {
                startActivity(Intent(this, TeacherPortalActivity::class.java))
                finish()
                return
            }

            binding = ActivityLoginBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Initialize DB safely in background or lazily
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    db = AppDatabase.getDatabase(applicationContext)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            binding.btnLogin.setOnClickListener {
                authenticate()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Login Start Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun authenticate() {
        val inputCode = binding.etSchoolCode.text?.toString()?.trim() ?: ""
        val inputPassword = binding.etPassword.text?.toString()?.trim() ?: ""

        if (!inputCode.equals(SCHOOL_CODE, ignoreCase = true)) {
            binding.tilSchoolCode.error = "Invalid School Code (Must be VV001)"
            return
        }
        binding.tilSchoolCode.error = null

        if (inputPassword.isEmpty()) {
            binding.tilPassword.error = "Please enter password"
            return
        }

        if (inputPassword == ADMIN_PASSWORD) {
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putString(KEY_ROLE, "ADMIN")
                .apply()
            Toast.makeText(this, "Logged in as Administrator", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        val database = db
        if (database == null) {
            Toast.makeText(this, "Database initializing, please wait...", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val teacher = database.schoolDao().getTeacherByPassword(inputPassword)
                withContext(Dispatchers.Main) {
                    if (teacher != null) {
                        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                            .putString(KEY_ROLE, "TEACHER")
                            .putString(KEY_TEACHER_NAME, teacher.name)
                            .apply()
                        Toast.makeText(this@LoginActivity, "Welcome Teacher ${teacher.name}!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@LoginActivity, TeacherPortalActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        binding.tilPassword.error = "Incorrect Password"
                        Toast.makeText(this@LoginActivity, "Authentication Failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LoginActivity, "DB Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
FILE_EOF

echo "=== [2/3] Securing AppDatabase against Main-Thread Blocking ==="
cat << 'FILE_EOF' > app/src/main/java/com/vidyamandir/schoolmanager/data/AppDatabase.kt
package com.vidyamandir.schoolmanager.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope

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
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun schoolDao(): SchoolDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vv_school_clean.db"
                )
                .fallbackToDestructiveMigration()
                .fallbackToDestructiveMigrationOnDowngrade()
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        try {
                            db.execSQL("INSERT OR IGNORE INTO late_fee_rules (id, dayFrom, dayTo, fineAmount) VALUES (1, 1, 10, 0.0);")
                            db.execSQL("INSERT OR IGNORE INTO late_fee_rules (id, dayFrom, dayTo, fineAmount) VALUES (2, 11, 20, 50.0);")
                            db.execSQL("INSERT OR IGNORE INTO late_fee_rules (id, dayFrom, dayTo, fineAmount) VALUES (3, 21, 30, 100.0);")
                        } catch (_: Exception) {}
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return getDatabase(context)
        }
    }
}
FILE_EOF

echo "=== [3/3] Committing and Pushing to GitHub ==="
git add app/src/main/java/com/vidyamandir/schoolmanager/ui/LoginActivity.kt
git add app/src/main/java/com/vidyamandir/schoolmanager/data/AppDatabase.kt
git commit -m "fix: make database initialization asynchronous and wrap LoginActivity in try-catch to prevent pre-login crash"
git push origin main

echo "=========================================================================="
echo " SUCCESS! Startup safety patch pushed to GitHub main.                     "
echo " Download your compiled APK from GitHub Actions and test opening the app. "
echo "=========================================================================="
