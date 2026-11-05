package com.vidyamandir.schoolmanager

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.vidyamandir.schoolmanager.databinding.ActivityMainBinding
import com.vidyamandir.schoolmanager.ui.FeeCollectionActivity
import com.vidyamandir.schoolmanager.ui.LoginActivity
import com.vidyamandir.schoolmanager.ui.ResultPublishActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Toolbar Menu / Logout Action
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_logout -> {
                    logoutAdmin()
                    true
                }
                else -> false
            }
        }

        binding.btnFeeCounter.setOnClickListener {
            startActivity(Intent(this, FeeCollectionActivity::class.java))
        }

        binding.btnResults.setOnClickListener {
            startActivity(Intent(this, ResultPublishActivity::class.java))
        }
    }

    private fun logoutAdmin() {
        val prefs = getSharedPreferences("vv_admin_auth", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
