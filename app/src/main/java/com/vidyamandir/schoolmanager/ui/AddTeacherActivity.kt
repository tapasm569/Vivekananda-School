package com.vidyamandir.schoolmanager.ui

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.data.Teacher
import com.vidyamandir.schoolmanager.databinding.ActivityAddTeacherBinding
import com.vidyamandir.schoolmanager.util.ImageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddTeacherActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTeacherBinding
    private lateinit var db: AppDatabase
    private var internalPhotoPath: String? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val savedPath = ImageHelper.saveImageToInternalStorage(this, it, "teacher")
            if (savedPath != null) {
                internalPhotoPath = savedPath
                ImageHelper.loadProfilePhoto(savedPath, binding.ivTeacherPhoto, android.R.drawable.ic_menu_camera)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTeacherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this, lifecycleScope)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnUploadTeacherPhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnSaveTeacher.setOnClickListener {
            saveTeacher()
        }
    }

    private fun saveTeacher() {
        val name = binding.etTeacherName.text?.toString()?.trim() ?: ""
        val phone = binding.etTeacherPhone.text?.toString()?.trim() ?: ""
        val password = binding.etTeacherPassword.text?.toString()?.trim() ?: ""

        if (name.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields (Name, Phone, Password)", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val teacher = Teacher(
                name = name,
                phone = phone,
                password = password,
                photoUri = internalPhotoPath
            )
            db.schoolDao().insertTeacher(teacher)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@AddTeacherActivity, "Teacher registered successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
