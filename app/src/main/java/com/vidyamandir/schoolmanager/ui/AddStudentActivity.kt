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
import com.vidyamandir.schoolmanager.util.ImageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

data class ClassUnitOption(
    val className: String,
    val section: String,
    val fee: Double
) {
    val displayText: String
        get() = if (section.isNotBlank()) "$className (Sec $section)" else className
}

class AddStudentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddStudentBinding
    private lateinit var db: AppDatabase
    private var internalPhotoPath: String? = null
    private val classUnitOptions = mutableListOf<ClassUnitOption>()

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val savedPath = ImageHelper.saveImageToInternalStorage(this, it, "student")
            if (savedPath != null) {
                internalPhotoPath = savedPath
                ImageHelper.loadProfilePhoto(savedPath, binding.ivStudentPhoto, android.R.drawable.ic_menu_camera)
            }
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
            classUnitOptions.clear()

            for (cls in classes) {
                for (unit in cls.getDisplayUnits()) {
                    classUnitOptions.add(ClassUnitOption(unit.first, unit.second, cls.monthlyFee))
                }
            }

            withContext(Dispatchers.Main) {
                if (classUnitOptions.isNotEmpty()) {
                    val names = classUnitOptions.map { it.displayText }
                    val adapter = ArrayAdapter(this@AddStudentActivity, android.R.layout.simple_dropdown_item_1line, names)
                    binding.actvClass.setAdapter(adapter)
                    binding.actvClass.setText(names[0], false)
                } else {
                    Toast.makeText(this@AddStudentActivity, "Please add a class first in 'CLASS' menu.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun saveStudent() {
        val selectedText = binding.actvClass.text?.toString()?.trim() ?: ""
        val matchedUnit = classUnitOptions.firstOrNull { it.displayText.equals(selectedText, ignoreCase = true) }

        if (matchedUnit == null) {
            Toast.makeText(this, "Please select a valid Class", Toast.LENGTH_SHORT).show()
            return
        }

        val name = binding.etStudentName.text?.toString()?.trim() ?: ""
        val rollStr = binding.etRollNo.text?.toString()?.trim() ?: "1"
        val father = binding.etFatherName.text?.toString()?.trim() ?: ""
        val mother = binding.etMotherName.text?.toString()?.trim() ?: ""
        val phone = binding.etContactPhone.text?.toString()?.trim() ?: ""
        val address = binding.etAddress.text?.toString()?.trim() ?: ""

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please enter Student Name and Contact Number", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val admissionNo = "VV-${System.currentTimeMillis().toString().takeLast(5)}"
            val admissionMonth = Calendar.getInstance().get(Calendar.MONTH) + 1

            val student = Student(
                admissionNo = admissionNo,
                fullName = name,
                className = matchedUnit.className,
                section = matchedUnit.section,
                rollNo = rollStr.toIntOrNull() ?: 1,
                fatherName = father,
                motherName = mother,
                parentPhone = phone,
                address = address,
                photoUri = internalPhotoPath,
                admissionMonth = admissionMonth,
                monthlyFee = matchedUnit.fee
            )

            db.schoolDao().insertStudent(student)

            withContext(Dispatchers.Main) {
                Toast.makeText(this@AddStudentActivity, "Student enrolled successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
