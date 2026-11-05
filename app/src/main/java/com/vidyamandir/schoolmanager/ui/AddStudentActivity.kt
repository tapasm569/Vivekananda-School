package com.vidyamandir.schoolmanager.ui

import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.data.Student
import com.vidyamandir.schoolmanager.databinding.ActivityAddStudentBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class AddStudentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddStudentBinding
    private lateinit var db: AppDatabase
    private var selectedPhotoUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedPhotoUri = it
            binding.ivStudentPhoto.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddStudentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this, lifecycleScope)
        binding.toolbar.setNavigationOnClickListener { finish() }

        loadClasses()

        binding.btnUploadPhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnSaveStudent.setOnClickListener {
            saveStudent()
        }
    }

    private fun loadClasses() {
        lifecycleScope.launch(Dispatchers.IO) {
            val classes = db.schoolDao().getAllClasses().first()
            val classNames = classes.map { it.className }
            withContext(Dispatchers.Main) {
                val adapter = ArrayAdapter(this@AddStudentActivity, android.R.layout.simple_dropdown_item_1line, classNames)
                binding.actvClass.setAdapter(adapter)
                if (classNames.isNotEmpty()) {
                    binding.actvClass.setText(classNames[0], false)
                }
            }
        }
    }

    private fun saveStudent() {
        val name = binding.etStudentName.text?.toString()?.trim() ?: ""
        val rollStr = binding.etRollNo.text?.toString()?.trim() ?: "1"
        val className = binding.actvClass.text?.toString()?.trim() ?: "Class V"
        val father = binding.etFatherName.text?.toString()?.trim() ?: ""
        val mother = binding.etMotherName.text?.toString()?.trim() ?: ""
        val phone = binding.etContactPhone.text?.toString()?.trim() ?: ""
        val address = binding.etAddress.text?.toString()?.trim() ?: ""

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please enter Student Name and Contact Number", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val cls = db.schoolDao().getClassByName(className)
            val fee = cls?.monthlyFee ?: 500.0
            val admissionNo = "VV-${System.currentTimeMillis().toString().takeLast(5)}"
            val admissionMonth = Calendar.getInstance().get(Calendar.MONTH) + 1

            val student = Student(
                admissionNo = admissionNo,
                fullName = name,
                className = className,
                section = "A",
                rollNo = rollStr.toIntOrNull() ?: 1,
                fatherName = father,
                motherName = mother,
                parentPhone = phone,
                address = address,
                photoUri = selectedPhotoUri?.toString(),
                admissionMonth = admissionMonth,
                monthlyFee = fee
            )

            db.schoolDao().insertStudent(student)

            withContext(Dispatchers.Main) {
                Toast.makeText(this@AddStudentActivity, "Student Enrolled Successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
