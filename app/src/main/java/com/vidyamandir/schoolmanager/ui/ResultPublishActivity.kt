package com.vidyamandir.schoolmanager.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.data.ExamDefinition
import com.vidyamandir.schoolmanager.data.ExamResult
import com.vidyamandir.schoolmanager.data.ExamSubject
import com.vidyamandir.schoolmanager.data.Student
import com.vidyamandir.schoolmanager.databinding.ActivityResultPublishBinding
import com.vidyamandir.schoolmanager.databinding.ItemPublishMarkInputBinding
import com.vidyamandir.schoolmanager.util.ResultPdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class PublishSubjectRow(
    val subjectName: String,
    val fullMarks: Int,
    val hasWritten: Boolean,
    val maxWritten: Int,
    var writtenMarks: Int = 0,
    val hasOral: Boolean,
    val maxOral: Int,
    var oralMarks: Int = 0,
    val hasPractical: Boolean,
    val maxPractical: Int,
    var practicalMarks: Int = 0
) {
    val totalObtained: Int get() = writtenMarks + oralMarks + practicalMarks

    val grade: String
        get() {
            val pct = if (fullMarks > 0) (totalObtained.toDouble() / fullMarks * 100) else 0.0
            return when {
                pct >= 90 -> "AA"
                pct >= 80 -> "A+"
                pct >= 70 -> "A"
                pct >= 60 -> "B+"
                pct >= 50 -> "B"
                pct >= 35 -> "C"
                pct >= 25 -> "D"
                else -> "F"
            }
        }
}

class ResultPublishActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultPublishBinding
    private lateinit var db: AppDatabase
    private lateinit var adapter: PublishMarkAdapter

    private var examsList: List<ExamDefinition> = emptyList()
    private var selectedExam: ExamDefinition? = null
    private var classStudents: List<Student> = emptyList()
    private var currentStudentIndex = 0

    private val subjectRows = mutableListOf<PublishSubjectRow>()
    private var userRole = "ADMIN"
    private var loggedInTeacherName = "Admin"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultPublishBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this, lifecycleScope)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val prefs = getSharedPreferences("vv_auth_session", Context.MODE_PRIVATE)
        userRole = prefs.getString("session_role", "ADMIN") ?: "ADMIN"
        loggedInTeacherName = prefs.getString("teacher_name", "Admin") ?: "Admin"

        adapter = PublishMarkAdapter(subjectRows)
        binding.rvStudentMarks.layoutManager = LinearLayoutManager(this)
        binding.rvStudentMarks.adapter = adapter

        loadExams()
        setupListeners()
    }

    private fun loadExams() {
        lifecycleScope.launch(Dispatchers.IO) {
            examsList = db.schoolDao().getAllExams().first()
            withContext(Dispatchers.Main) {
                if (examsList.isNotEmpty()) {
                    val examNames = examsList.map { "${it.examName} (${it.targetClass})" }
                    val arrayAdapter = ArrayAdapter(this@ResultPublishActivity, android.R.layout.simple_dropdown_item_1line, examNames)
                    binding.actvExamSelect.setAdapter(arrayAdapter)
                    binding.actvExamSelect.setText(examNames[0], false)
                    onExamChosen(examsList[0])
                } else {
                    binding.tvTargetClassBanner.text = "No exams configured yet! Go to 'SET EXAMS' first."
                }
            }
        }
    }

    private fun onExamChosen(exam: ExamDefinition) {
        selectedExam = exam
        binding.tvTargetClassBanner.text = "Target Class: ${exam.targetClass} • Date: ${exam.examDate}"

        lifecycleScope.launch(Dispatchers.IO) {
            val subjects = db.schoolDao().getSubjectsForExam(exam.id)
            val allStudents = db.schoolDao().getAllActiveStudents().first()
            classStudents = allStudents.filter { it.displayClass.equals(exam.targetClass, ignoreCase = true) }

            withContext(Dispatchers.Main) {
                subjectRows.clear()
                subjects.forEach { s ->
                    subjectRows.add(
                        PublishSubjectRow(
                            subjectName = s.subjectName,
                            fullMarks = s.fullMarks,
                            hasWritten = s.hasWritten,
                            maxWritten = s.maxWritten,
                            hasOral = s.hasOral,
                            maxOral = s.maxOral,
                            hasPractical = s.hasPractical,
                            maxPractical = s.maxPractical
                        )
                    )
                }
                adapter.notifyDataSetChanged()

                if (classStudents.isNotEmpty()) {
                    val studentNames = classStudents.map { "${it.fullName} (Roll ${it.rollNo})" }
                    val studentAdapter = ArrayAdapter(this@ResultPublishActivity, android.R.layout.simple_dropdown_item_1line, studentNames)
                    binding.actvStudentSelect.setAdapter(studentAdapter)
                    binding.actvStudentSelect.setText(studentNames[0], false)
                    currentStudentIndex = 0
                }
            }
        }
    }

    private fun setupListeners() {
        binding.actvExamSelect.setOnItemClickListener { _, _, position, _ ->
            onExamChosen(examsList[position])
        }

        binding.actvStudentSelect.setOnItemClickListener { _, _, position, _ ->
            currentStudentIndex = position
        }

        binding.btnSaveMarks.setOnClickListener {
            saveCurrentStudentMarks(onComplete = {
                val statusMsg = if (userRole == "ADMIN") "Marks Saved & Approved!" else "Marks submitted for Admin verification!"
                Toast.makeText(this, statusMsg, Toast.LENGTH_SHORT).show()
            })
        }

        binding.btnSaveAndNext.setOnClickListener {
            saveCurrentStudentMarks(onComplete = {
                if (currentStudentIndex < classStudents.size - 1) {
                    currentStudentIndex++
                    val nextStudent = classStudents[currentStudentIndex]
                    binding.actvStudentSelect.setText("${nextStudent.fullName} (Roll ${nextStudent.rollNo})", false)
                    Toast.makeText(this, "Next: ${nextStudent.fullName}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "All students of this class completed!", Toast.LENGTH_LONG).show()
                }
            })
        }

        binding.btnGeneratePdf.setOnClickListener {
            saveCurrentStudentMarks(onComplete = {
                generateAndOpenPdf()
            })
        }
    }

    private fun saveCurrentStudentMarks(onComplete: () -> Unit) {
        val exam = selectedExam
        if (exam == null || classStudents.isEmpty()) {
            Toast.makeText(this, "No student or exam selected", Toast.LENGTH_SHORT).show()
            return
        }

        val student = classStudents[currentStudentIndex]
        val status = if (userRole == "ADMIN") "APPROVED" else "PENDING"

        val results = subjectRows.map {
            ExamResult(
                studentId = student.id,
                examName = exam.examName,
                academicYear = exam.academicYear,
                subjectName = it.subjectName,
                fullMarks = it.fullMarks,
                writtenMarks = it.writtenMarks,
                oralMarks = it.oralMarks,
                practicalMarks = it.practicalMarks,
                grade = it.grade,
                submittedByTeacher = loggedInTeacherName,
                approvalStatus = status
            )
        }

        lifecycleScope.launch(Dispatchers.IO) {
            db.schoolDao().insertResults(results)
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    private fun generateAndOpenPdf() {
        val student = classStudents[currentStudentIndex]
        val exam = selectedExam ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            val examResults = db.schoolDao().getStudentExamResults(student.id, exam.examName)
            withContext(Dispatchers.Main) {
                if (examResults.isEmpty()) {
                    Toast.makeText(this@ResultPublishActivity, "Please save marks first", Toast.LENGTH_SHORT).show()
                    return@withContext
                }

                try {
                    val pdfDir = File(cacheDir, "report_cards").apply { if (!exists()) mkdirs() }
                    val pdfFile = File(pdfDir, "Report_${student.fullName.replace(" ", "_")}.pdf")

                    val generator = ResultPdfGenerator(this@ResultPublishActivity)
                    generator.generateReportCard(student, exam.examName, examResults, pdfFile)

                    val uri: Uri = FileProvider.getUriForFile(
                        this@ResultPublishActivity,
                        "${packageName}.fileprovider",
                        pdfFile
                    )
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NO_HISTORY
                    }
                    startActivity(Intent.createChooser(intent, "Open Report Card"))
                } catch (e: Exception) {
                    Toast.makeText(this@ResultPublishActivity, "PDF Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    class PublishMarkAdapter(private val list: List<PublishSubjectRow>) :
        RecyclerView.Adapter<PublishMarkAdapter.ViewHolder>() {

        class ViewHolder(val b: ItemPublishMarkInputBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val b = ItemPublishMarkInputBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(b)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.b.tvSubjectTitle.text = "${item.subjectName} (Full: ${item.fullMarks})"

            // Written Field
            holder.b.tilWritten.visibility = if (item.hasWritten) View.VISIBLE else View.GONE
            holder.b.tilWritten.hint = "Written (/${item.maxWritten})"
            holder.b.etWritten.setText(item.writtenMarks.toString())

            // Oral Field
            holder.b.tilOral.visibility = if (item.hasOral) View.VISIBLE else View.GONE
            holder.b.tilOral.hint = "Oral (/${item.maxOral})"
            holder.b.etOral.setText(item.oralMarks.toString())

            // Practical Field
            holder.b.tilPractical.visibility = if (item.hasPractical) View.VISIBLE else View.GONE
            holder.b.tilPractical.hint = "Practical (/${item.maxPractical})"
            holder.b.etPractical.setText(item.practicalMarks.toString())

            val updateBadge = {
                holder.b.tvGradeBadge.text = "Grade: ${item.grade} (${item.totalObtained}/${item.fullMarks})"
            }
            updateBadge()

            holder.b.etWritten.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    val input = s.toString().toIntOrNull() ?: 0
                    item.writtenMarks = input.coerceAtMost(item.maxWritten)
                    updateBadge()
                }
                override fun afterTextChanged(s: Editable?) {}
            })

            holder.b.etOral.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    val input = s.toString().toIntOrNull() ?: 0
                    item.oralMarks = input.coerceAtMost(item.maxOral)
                    updateBadge()
                }
                override fun afterTextChanged(s: Editable?) {}
            })

            holder.b.etPractical.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    val input = s.toString().toIntOrNull() ?: 0
                    item.practicalMarks = input.coerceAtMost(item.maxPractical)
                    updateBadge()
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }

        override fun getItemCount() = list.size
    }
}
