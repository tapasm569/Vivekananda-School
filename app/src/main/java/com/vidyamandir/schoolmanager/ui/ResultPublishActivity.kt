package com.vidyamandir.schoolmanager.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.data.ExamResult
import com.vidyamandir.schoolmanager.data.Student
import com.vidyamandir.schoolmanager.databinding.ActivityResultPublishBinding
import com.vidyamandir.schoolmanager.util.ResultPdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ResultPublishActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultPublishBinding
    private lateinit var db: AppDatabase
    private lateinit var adapter: SubjectMarkAdapter

    private var studentsList: List<Student> = emptyList()
    private var selectedStudent: Student? = null

    private val subjectScoreItems = mutableListOf(
        SubjectScoreItem("First Language (Bengali)", 100, 75, 20),
        SubjectScoreItem("Second Language (English)", 100, 68, 18),
        SubjectScoreItem("Mathematics", 100, 78, 20),
        SubjectScoreItem("Physical Science", 100, 62, 19),
        SubjectScoreItem("Life Science", 100, 70, 18),
        SubjectScoreItem("History", 100, 65, 17),
        SubjectScoreItem("Geography", 100, 72, 18)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultPublishBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this, lifecycleScope)
        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = SubjectMarkAdapter(subjectScoreItems)
        binding.rvSubjects.layoutManager = LinearLayoutManager(this)
        binding.rvSubjects.adapter = adapter

        loadStudents()

        binding.actvStudentSelect.setOnItemClickListener { _, _, position, _ ->
            selectedStudent = studentsList[position]
        }

        binding.btnSaveMarks.setOnClickListener {
            saveMarksToDatabase {
                Toast.makeText(this, "Marks saved successfully!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnGeneratePdf.setOnClickListener {
            saveMarksToDatabase {
                generateAndOpenPdf()
            }
        }
    }

    private fun loadStudents() {
        lifecycleScope.launch(Dispatchers.IO) {
            studentsList = db.schoolDao().getAllActiveStudents().first()
            withContext(Dispatchers.Main) {
                if (studentsList.isNotEmpty()) {
                    val names = studentsList.map { "${it.fullName} (${it.displayClass}) - Roll ${it.rollNo}" }
                    val arrayAdapter = ArrayAdapter(this@ResultPublishActivity, android.R.layout.simple_dropdown_item_1line, names)
                    binding.actvStudentSelect.setAdapter(arrayAdapter)
                    binding.actvStudentSelect.setText(names[0], false)
                    selectedStudent = studentsList[0]
                }
            }
        }
    }

    private fun saveMarksToDatabase(onSuccess: () -> Unit) {
        val student = selectedStudent
        if (student == null) {
            Toast.makeText(this, "Please select a student", Toast.LENGTH_SHORT).show()
            return
        }

        val examName = binding.etExamName.text.toString().trim().ifEmpty { "Annual Examination 2026" }

        val examResults = subjectScoreItems.map {
            ExamResult(
                studentId = student.id,
                examName = examName,
                academicYear = 2026,
                subjectName = it.subjectName,
                fullMarks = it.fullMarks,
                writtenMarks = it.writtenMarks,
                practicalMarks = it.practicalMarks,
                grade = it.grade,
                approvalStatus = "APPROVED"
            )
        }

        lifecycleScope.launch(Dispatchers.IO) {
            db.schoolDao().insertResults(examResults)
            withContext(Dispatchers.Main) {
                onSuccess()
            }
        }
    }

    private fun generateAndOpenPdf() {
        val student = selectedStudent ?: return
        val examName = binding.etExamName.text.toString().trim().ifEmpty { "Annual Examination 2026" }

        val examResults = subjectScoreItems.map {
            ExamResult(
                studentId = student.id,
                examName = examName,
                academicYear = 2026,
                subjectName = it.subjectName,
                fullMarks = it.fullMarks,
                writtenMarks = it.writtenMarks,
                practicalMarks = it.practicalMarks,
                grade = it.grade,
                approvalStatus = "APPROVED"
            )
        }

        try {
            val pdfDir = File(cacheDir, "report_cards").apply { if (!exists()) mkdirs() }
            val sanitizedName = student.fullName.replace(" ", "_")
            val pdfFile = File(pdfDir, "Report_${sanitizedName}_2026.pdf")

            val generator = ResultPdfGenerator(this)
            generator.generateReportCard(student, examName, examResults, pdfFile)

            val fileUri: Uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                pdfFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(fileUri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NO_HISTORY
            }

            startActivity(Intent.createChooser(intent, "Open 2-Page Vivekananda Report Card"))
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to create PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
