package com.vidyamandir.schoolmanager.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.vidyamandir.schoolmanager.MainActivity
import com.vidyamandir.schoolmanager.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    companion object {
        private const val PREFS_NAME = "vv_admin_auth"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val VALID_SCHOOL_CODE = "VV001"
        private const val VALID_PASSWORD = "India360@"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if session is already active
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_IS_LOGGED_IN, false)) {
            navigateToDashboard()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            validateAndAuthenticate()
        }
    }

    private fun validateAndAuthenticate() {
        val inputCode = binding.etSchoolCode.text?.toString()?.trim() ?: ""
        val inputPassword = binding.etPassword.text?.toString()?.trim() ?: ""

        binding.tilSchoolCode.error = null
        binding.tilPassword.error = null

        var hasError = false

        if (inputCode.isEmpty()) {
            binding.tilSchoolCode.error = "School Code is required"
            hasError = true
        }

        if (inputPassword.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            hasError = true
        }

        if (hasError) return

        if (inputCode.equals(VALID_SCHOOL_CODE, ignoreCase = true) && inputPassword == VALID_PASSWORD) {
            // Persist session
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .apply()

            Toast.makeText(this, "Authentication successful. Welcome Admin!", Toast.LENGTH_SHORT).show()
            navigateToDashboard()
        } else {
            if (!inputCode.equals(VALID_SCHOOL_CODE, ignoreCase = true)) {
                binding.tilSchoolCode.error = "Invalid School Code"
            }
            if (inputPassword != VALID_PASSWORD) {
                binding.tilPassword.error = "Incorrect password"
            }
            Toast.makeText(this, "Access Denied: Invalid credentials", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
