package com.vidyamandir.schoolmanager

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.vidyamandir.schoolmanager.databinding.ActivityMainBinding
import com.vidyamandir.schoolmanager.ui.FeeCollectionActivity
import com.vidyamandir.schoolmanager.ui.ResultPublishActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnFeeCounter.setOnClickListener {
            startActivity(Intent(this, FeeCollectionActivity::class.java))
        }

        binding.btnResults.setOnClickListener {
            startActivity(Intent(this, ResultPublishActivity::class.java))
        }
    }
}
