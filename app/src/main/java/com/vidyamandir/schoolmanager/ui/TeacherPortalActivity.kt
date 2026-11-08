package com.vidyamandir.schoolmanager.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.withTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.data.ExamDefinition
import com.vidyamandir.schoolmanager.data.ExamResult
import com.vidyamandir.schoolmanager.data.ExamResultComponent
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
    private var examsList: List<ExamDefinition> = emptyList()
    private var selectedExam: ExamDefinition? = null

    private val scoreItems = mutableListOf<SubjectScoreItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTeacherPortalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this, lifecycleScope)

        adapter = SubjectMarkAdapter(scoreItems)
        binding.rvMarks.layoutManager = LinearLayoutManager(this)
        binding.rvMarks.adapter = adapter

        loadStudents()

        binding.actvStudent.setOnItemClickListener { _, _, position, _ ->
            if (position in studentsList.indices) {
                selectedStudent = studentsList[position]
                loadExamsForSelectedStudent()
            }
        }

        binding.actvExam.setOnItemClickListener { _, _, position, _ ->
            if (position in examsList.indices) {
                selectedExam = examsList[position]
                loadSubjectsForSelectedExam()
            }
        }

        binding.btnSubmitToAdmin.setOnClickListener {
            submitMarks()
        }

        binding.btnTeacherLogout.setOnClickListener {
            getSharedPreferences("vv_auth_session", Context.MODE_PRIVATE)
                .edit().clear().apply()

            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun loadStudents() {
        lifecycleScope.launch {
            try {
                val students = withContext(Dispatchers.IO) {
                    db.schoolDao().getAllActiveStudents().first()
                }

                studentsList = students

                if (studentsList.isEmpty()) {
                    Toast.makeText(
                        this@TeacherPortalActivity,
                        "No active students found",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                val names = studentsList.map {
                    "${it.fullName} (${it.displayClass}) - Roll ${it.rollNo}"
                }

                val studentAdapter = ArrayAdapter(
                    this@TeacherPortalActivity,
                    android.R.layout.simple_dropdown_item_1line,
                    names
                )

                binding.actvStudent.setAdapter(studentAdapter)
                binding.actvStudent.setText(names[0], false)

                selectedStudent = studentsList[0]
                loadExamsForSelectedStudent()

            } catch (e: Exception) {
                Toast.makeText(
                    this@TeacherPortalActivity,
                    "Unable to load students: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun loadExamsForSelectedStudent() {
        val student = selectedStudent ?: return

        selectedExam = null
        examsList = emptyList()
        scoreItems.clear()
        adapter.replaceItems(emptyList())

        binding.actvExam.setText("", false)
        binding.actvExam.setAdapter(
            ArrayAdapter(
                this@TeacherPortalActivity,
                android.R.layout.simple_dropdown_item_1line,
                emptyList<String>()
            )
        )

        lifecycleScope.launch {
            try {
                val exams = withContext(Dispatchers.IO) {
                    db.schoolDao().getExamsForClass(student.displayClass)
                }

                if (selectedStudent?.id != student.id) return@launch

                examsList = exams

                if (examsList.isEmpty()) {
                    Toast.makeText(
                        this@TeacherPortalActivity,
                        "No exams configured for ${student.displayClass}",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                val names = examsList.map {
                    "${it.examName} • ${it.examDate} • ${it.academicYear}"
                }

                val examAdapter = ArrayAdapter(
                    this@TeacherPortalActivity,
                    android.R.layout.simple_dropdown_item_1line,
                    names
                )

                binding.actvExam.setAdapter(examAdapter)
                binding.actvExam.setText("", false)

                Toast.makeText(
                    this@TeacherPortalActivity,
                    "${examsList.size} exam(s) available for ${student.displayClass}",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {
                Toast.makeText(
                    this@TeacherPortalActivity,
                    "Unable to load exams: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun loadSubjectsForSelectedExam() {
        val student = selectedStudent ?: return
        val exam = selectedExam ?: return

        scoreItems.clear()
        adapter.replaceItems(emptyList())

        lifecycleScope.launch {
            try {
                val loadedItems = withContext(Dispatchers.IO) {
                    val dao = db.schoolDao()
                    val subjects = dao.getSubjectsForExam(exam.id)
                    val items = mutableListOf<SubjectScoreItem>()

                    for (subject in subjects) {
                        val configuredComponents =
                            dao.getExamSubjectComponents(subject.id)

                        val item = SubjectScoreItem(
                            subjectName = subject.subjectName,
                            fullMarks = subject.fullMarks,
                            writtenMarks = 0,
                            oralMarks = 0,
                            practicalMarks = 0,
                            hasWritten = subject.hasWritten,
                            maxWritten = subject.maxWritten,
                            hasOral = subject.hasOral,
                            maxOral = subject.maxOral,
                            hasPractical = subject.hasPractical,
                            maxPractical = subject.maxPractical,
                            customComponents = configuredComponents.map {
                                ComponentScoreItem(
                                    name = it.componentName,
                                    maxMarks = it.maxMarks,
                                    obtainedMarks = 0,
                                    position = it.position
                                )
                            }.toMutableList()
                        )

                        val existing = dao.getExistingExamResult(
                            student.id,
                            exam.examName,
                            exam.academicYear,
                            subject.subjectName
                        )

                        if (existing != null) {
                            item.writtenMarks = existing.writtenMarks
                            item.oralMarks = existing.oralMarks
                            item.practicalMarks = existing.practicalMarks

                            val savedComponents =
                                dao.getExamResultComponents(existing.id)

                            item.customComponents.forEach { component ->
                                val saved = savedComponents.firstOrNull {
                                    it.componentName.equals(
                                        component.name,
                                        ignoreCase = true
                                    )
                                }

                                if (saved != null) {
                                    component.obtainedMarks =
                                        saved.obtainedMarks
                                }
                            }
                        }

                        items.add(item)
                    }

                    items
                }

                if (selectedStudent?.id != student.id ||
                    selectedExam?.id != exam.id
                ) return@launch

                scoreItems.clear()
                scoreItems.addAll(loadedItems)
                adapter.replaceItems(loadedItems)

                if (loadedItems.isEmpty()) {
                    Toast.makeText(
                        this@TeacherPortalActivity,
                        "No subjects configured for this exam",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@TeacherPortalActivity,
                    "Unable to load subjects: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun submitMarks() {
        val student = selectedStudent
        val exam = selectedExam

        if (student == null) {
            Toast.makeText(this, "Please select a student", Toast.LENGTH_SHORT).show()
            return
        }

        if (exam == null) {
            Toast.makeText(this, "Please select an exam", Toast.LENGTH_SHORT).show()
            return
        }

        if (scoreItems.isEmpty()) {
            Toast.makeText(this, "No subjects available", Toast.LENGTH_SHORT).show()
            return
        }

        val teacherName = getSharedPreferences(
            "vv_auth_session",
            Context.MODE_PRIVATE
        ).getString("teacher_name", "Teacher") ?: "Teacher"

        val itemsToSave = scoreItems.toList()

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    db.withTransaction {
                        val dao = db.schoolDao()

                        itemsToSave.distinctBy {
                            it.subjectName.trim().lowercase()
                        }.forEach { item ->

                            val existing = dao.getExistingExamResult(
                                student.id,
                                exam.examName,
                                exam.academicYear,
                                item.subjectName.trim()
                            )

                            val result = ExamResult(
                                id = existing?.id ?: 0L,
                                studentId = student.id,
                                examName = exam.examName,
                                academicYear = exam.academicYear,
                                subjectName = item.subjectName.trim(),
                                fullMarks = item.fullMarks,
                                writtenMarks = item.writtenMarks,
                                oralMarks = item.oralMarks,
                                practicalMarks = item.practicalMarks,
                                grade = item.grade,
                                submittedByTeacher = teacherName,
                                approvalStatus = existing?.approvalStatus ?: "PENDING"
                            )

                            val resultId = if (existing != null) {
                                dao.updateExamResult(result)
                                existing.id
                            } else {
                                dao.insertExamResult(result)
                            }

                            dao.deleteExamResultComponents(resultId)

                            val components = item.customComponents.map {
                                ExamResultComponent(
                                    examResultId = resultId,
                                    componentName = it.name,
                                    maxMarks = it.maxMarks,
                                    obtainedMarks = it.obtainedMarks,
                                    position = it.position
                                )
                            }

                            if (components.isNotEmpty()) {
                                dao.insertExamResultComponents(components)
                            }
                        }
                    }
                }

                Toast.makeText(
                    this@TeacherPortalActivity,
                    "Marks submitted for Admin Approval!",
                    Toast.LENGTH_LONG
                ).show()

            } catch (e: Exception) {
                Toast.makeText(
                    this@TeacherPortalActivity,
                    "Failed to submit marks: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
