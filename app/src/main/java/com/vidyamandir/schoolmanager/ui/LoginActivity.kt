package com.vidyamandir.schoolmanager.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.vidyamandir.schoolmanager.MainActivity
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.databinding.ActivityLoginBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var db: AppDatabase

    companion object {
        private const val PREFS_NAME = "vv_auth_session"
        private const val KEY_ROLE = "session_role" // "ADMIN" or "TEACHER"
        private const val KEY_TEACHER_NAME = "teacher_name"
        private const val SCHOOL_CODE = "VV001"
        private const val ADMIN_PASSWORD = "India360@"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        db = AppDatabase.getDatabase(this, lifecycleScope)

        binding.btnLogin.setOnClickListener {
            authenticate()
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

        // 1. Check Admin Credentials
        if (inputPassword == ADMIN_PASSWORD) {
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putString(KEY_ROLE, "ADMIN")
                .apply()
            Toast.makeText(this, "Logged in as Administrator", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // 2. Check Teacher Credentials
        lifecycleScope.launch(Dispatchers.IO) {
            val teacher = db.schoolDao().getTeacherByPassword(inputPassword)
            withContext(Dispatchers.Main) {
                if (teacher != null) {
                    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                        .putString(KEY_ROLE, "TEACHER")
                        .putString(KEY_TEACHER_NAME, teacher.name)
                        .apply()
                    Toast.makeText(this@LoginActivity, "Welcome Teacher ${teacher.name}!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@LoginActivity, TeacherPortalActivity::class.java))
                    finish()
                } else {
                    binding.tilPassword.error = "Incorrect Password"
                    Toast.makeText(this@LoginActivity, "Authentication Failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
