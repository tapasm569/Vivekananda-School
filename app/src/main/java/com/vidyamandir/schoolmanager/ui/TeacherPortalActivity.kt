package com.vidyamandir.schoolmanager.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.data.ExamResult
import com.vidyamandir.schoolmanager.data.Student
import com.vidyamandir.schoolmanager.databinding.ActivityTeacherPortalBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TeacherPortalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTeacherPortalBinding
    private lateinit var db: AppDatabase
    private lateinit var adapter: SubjectMarkAdapter

    private var studentsList: List<Student> = emptyList()
    private var selectedStudent: Student? = null

    private val scoreItems = mutableListOf(
        SubjectScoreItem("First Language (Bengali)", 100, 70, 18),
        SubjectScoreItem("Second Language (English)", 100, 65, 17),
        SubjectScoreItem("Mathematics", 100, 75, 19),
        SubjectScoreItem("Physical Science", 100, 60, 18),
        SubjectScoreItem("Life Science", 100, 68, 18),
        SubjectScoreItem("History", 100, 62, 17),
        SubjectScoreItem("Geography", 100, 70, 18)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTeacherPortalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this, lifecycleScope)

        adapter = SubjectMarkAdapter(scoreItems)
        binding.rvMarks.layoutManager = LinearLayoutManager(this)
        binding.rvMarks.adapter = adapter

        loadStudents()

        binding.btnSubmitToAdmin.setOnClickListener {
            submitMarks()
        }

        binding.btnTeacherLogout.setOnClickListener {
            getSharedPreferences("vv_auth_session", Context.MODE_PRIVATE).edit().clear().apply()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun loadStudents() {
        lifecycleScope.launch(Dispatchers.IO) {
            studentsList = db.schoolDao().getAllActiveStudents().first()
            withContext(Dispatchers.Main) {
                if (studentsList.isNotEmpty()) {
                    val names = studentsList.map { "${it.fullName} (${it.displayClass}) - Roll ${it.rollNo}" }
                    val arrayAdapter = ArrayAdapter(this@TeacherPortalActivity, android.R.layout.simple_dropdown_item_1line, names)
                    binding.actvStudent.setAdapter(arrayAdapter)
                    binding.actvStudent.setText(names[0], false)
                    selectedStudent = studentsList[0]

                    binding.actvStudent.setOnItemClickListener { _, _, position, _ ->
                        selectedStudent = studentsList[position]
                    }
                }
            }
        }
    }

    private fun submitMarks() {
        val student = selectedStudent
        if (student == null) {
            Toast.makeText(this, "Please select a student", Toast.LENGTH_SHORT).show()
            return
        }

        val examName = binding.etExamName.text?.toString()?.trim() ?: "Annual Examination 2026"
        val teacherName = getSharedPreferences("vv_auth_session", Context.MODE_PRIVATE)
            .getString("teacher_name", "Teacher") ?: "Teacher"

        val results = scoreItems.map {
            ExamResult(
                studentId = student.id,
                examName = examName,
                academicYear = 2026,
                subjectName = it.subjectName,
                fullMarks = it.fullMarks,
                writtenMarks = it.writtenMarks,
                practicalMarks = it.practicalMarks,
                grade = it.grade,
                submittedByTeacher = teacherName,
                approvalStatus = "PENDING"
            )
        }

        lifecycleScope.launch(Dispatchers.IO) {
            db.schoolDao().insertResults(results)
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@TeacherPortalActivity,
                    "Marks for ${student.fullName} submitted to Admin Panel for approval!",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
