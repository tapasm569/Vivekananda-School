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
import androidx.room.withTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.data.ExamDefinition
import com.vidyamandir.schoolmanager.data.ExamResult
import com.vidyamandir.schoolmanager.data.ExamResultComponent
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

data class PublishComponentMark(
    val name: String,
    val maxMarks: Int,
    var obtainedMarks: Int = 0,
    val position: Int = 0
)

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
    var practicalMarks: Int = 0,
    val customComponents: MutableList<PublishComponentMark> = mutableListOf()
) {
    val totalObtained: Int get() = writtenMarks + oralMarks + practicalMarks + customComponents.sumOf { it.obtainedMarks }

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
            val dao = db.schoolDao()
            val subjects = dao.getSubjectsForExam(exam.id)
            val componentsBySubject = subjects.associate { subject ->
                subject.id to dao.getExamSubjectComponents(subject.id)
            }
            val allStudents = dao.getAllActiveStudents().first()
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
                            maxPractical = s.maxPractical,
                            customComponents = componentsBySubject[s.id].orEmpty()
                                .map {
                                    PublishComponentMark(
                                        name = it.componentName,
                                        maxMarks = it.maxMarks,
                                        position = it.position
                                    )
                                }.toMutableList()
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
                    loadCurrentStudentMarks()
                }
            }
        }
    }

    private fun loadCurrentStudentMarks() {
        val exam = selectedExam ?: return
        if (classStudents.isEmpty()) return

        val student = classStudents.getOrNull(currentStudentIndex) ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            val dao = db.schoolDao()

            subjectRows.forEach { row ->
                val existing = dao.getExistingExamResult(
                    student.id,
                    exam.examName,
                    exam.academicYear,
                    row.subjectName
                )

                row.writtenMarks = existing?.writtenMarks ?: 0
                row.oralMarks = existing?.oralMarks ?: 0
                row.practicalMarks = existing?.practicalMarks ?: 0

                val savedComponents = if (existing != null) {
                    dao.getExamResultComponents(existing.id)
                } else {
                    emptyList()
                }

                row.customComponents.forEach { component ->
                    component.obtainedMarks = savedComponents
                        .firstOrNull {
                            it.componentName.equals(component.name, ignoreCase = true)
                        }?.obtainedMarks ?: 0
                }
            }

            withContext(Dispatchers.Main) {
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun setupListeners() {
        binding.actvExamSelect.setOnItemClickListener { _, _, position, _ ->
            onExamChosen(examsList[position])
        }

        binding.actvStudentSelect.setOnItemClickListener { _, _, position, _ ->
            currentStudentIndex = position
            loadCurrentStudentMarks()
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
                    loadCurrentStudentMarks()
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

        val student = classStudents.getOrNull(currentStudentIndex) ?: return
        val status = if (userRole == "ADMIN") "APPROVED" else "PENDING"

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                db.withTransaction {
                    val dao = db.schoolDao()

                    subjectRows
                        .distinctBy { it.subjectName.trim().lowercase() }
                        .forEach { row ->

                            val subjectName = row.subjectName.trim()

                            val existing = dao.getExistingExamResult(
                                student.id,
                                exam.examName,
                                exam.academicYear,
                                subjectName
                            )

                            val result = ExamResult(
                                id = existing?.id ?: 0,
                                studentId = student.id,
                                examName = exam.examName,
                                academicYear = exam.academicYear,
                                subjectName = subjectName,
                                fullMarks = row.fullMarks,
                                writtenMarks = row.writtenMarks,
                                oralMarks = row.oralMarks,
                                practicalMarks = row.practicalMarks,
                                grade = row.grade,
                                submittedByTeacher = loggedInTeacherName,
                                approvalStatus = existing?.approvalStatus ?: status
                            )

                            val resultId = if (existing != null) {
                                dao.updateExamResult(result)
                                existing.id
                            } else {
                                dao.insertExamResult(result)
                            }

                            dao.deleteExamResultComponents(resultId)

                            val componentRecords = row.customComponents.map { component ->
                                ExamResultComponent(
                                    examResultId = resultId,
                                    componentName = component.name,
                                    maxMarks = component.maxMarks,
                                    obtainedMarks = component.obtainedMarks.coerceIn(
                                        0, component.maxMarks
                                    ),
                                    position = component.position
                                )
                            }

                            if (componentRecords.isNotEmpty()) {
                                dao.insertExamResultComponents(componentRecords)
                            }
                        }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ResultPublishActivity,
                        "All marks saved successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    onComplete()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ResultPublishActivity,
                        "Save failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun generateAndOpenPdf() {
        val student = classStudents[currentStudentIndex]
        val exam = selectedExam ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            val dao = db.schoolDao()
            val examResults = dao.getStudentExamResultsForYear(
                student.id, exam.examName, exam.academicYear
            )
            val componentsByResult = examResults.associate { result ->
                result.id to dao.getExamResultComponents(result.id)
            }

            withContext(Dispatchers.Main) {
                if (examResults.isEmpty()) {
                    Toast.makeText(this@ResultPublishActivity, "Please save marks first", Toast.LENGTH_SHORT).show()
                    return@withContext
                }

                try {
                    val pdfDir = File(cacheDir, "report_cards").apply { if (!exists()) mkdirs() }
                    val pdfFile = File(pdfDir, "Report_${student.fullName.replace(" ", "_")}.pdf")

                    val generator = ResultPdfGenerator(this@ResultPublishActivity)
                    generator.generateReportCard(
                        student, exam.examName, examResults, pdfFile,
                        componentsByResult
                    )

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

        class ViewHolder(val b: ItemPublishMarkInputBinding) :
            RecyclerView.ViewHolder(b.root) {
            var writtenWatcher: TextWatcher? = null
            var oralWatcher: TextWatcher? = null
            var practicalWatcher: TextWatcher? = null
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val b = ItemPublishMarkInputBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(b)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            val b = holder.b

            holder.writtenWatcher?.let { b.etWritten.removeTextChangedListener(it) }
            holder.oralWatcher?.let { b.etOral.removeTextChangedListener(it) }
            holder.practicalWatcher?.let { b.etPractical.removeTextChangedListener(it) }

            b.tvSubjectTitle.text = "${item.subjectName} (Full: ${item.fullMarks})"

            b.tilWritten.visibility = if (item.hasWritten) View.VISIBLE else View.GONE
            b.tilWritten.hint = "Written (/${item.maxWritten})"

            b.tilOral.visibility = if (item.hasOral) View.VISIBLE else View.GONE
            b.tilOral.hint = "Oral (/${item.maxOral})"

            b.tilPractical.visibility = if (item.hasPractical) View.VISIBLE else View.GONE
            b.tilPractical.hint = "Practical (/${item.maxPractical})"

            fun updateBadge() {
                b.tvGradeBadge.text =
                    "Grade: ${item.grade} (${item.totalObtained}/${item.fullMarks})"
            }

            fun setInput(
                editText: android.widget.EditText,
                value: Int
            ) {
                val text = value.toString()
                if (editText.text?.toString() != text) {
                    editText.setText(text)
                }
            }

            setInput(b.etWritten, item.writtenMarks)
            setInput(b.etOral, item.oralMarks)
            setInput(b.etPractical, item.practicalMarks)

            holder.writtenWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    item.writtenMarks = (s?.toString()?.toIntOrNull() ?: 0)
                        .coerceIn(0, item.maxWritten)
                    updateBadge()
                }
                override fun afterTextChanged(s: Editable?) {}
            }

            holder.oralWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    item.oralMarks = (s?.toString()?.toIntOrNull() ?: 0)
                        .coerceIn(0, item.maxOral)
                    updateBadge()
                }
                override fun afterTextChanged(s: Editable?) {}
            }

            holder.practicalWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    item.practicalMarks = (s?.toString()?.toIntOrNull() ?: 0)
                        .coerceIn(0, item.maxPractical)
                    updateBadge()
                }
                override fun afterTextChanged(s: Editable?) {}
            }

            b.etWritten.addTextChangedListener(holder.writtenWatcher)
            b.etOral.addTextChangedListener(holder.oralWatcher)
            b.etPractical.addTextChangedListener(holder.practicalWatcher)

            b.llCustomMarks.removeAllViews()

            item.customComponents.forEach { component ->
                val row = android.widget.LinearLayout(b.root.context).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(0, 4, 0, 4)
                }

                val label = android.widget.TextView(b.root.context).apply {
                    text = "${component.name} (/${component.maxMarks})"
                    textSize = 13f
                    setTextColor(android.graphics.Color.rgb(15, 61, 110))
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        0,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                }

                val input = android.widget.EditText(b.root.context).apply {
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER
                    setSingleLine(true)
                    textSize = 14f
                    gravity = android.view.Gravity.CENTER
                    setText(component.obtainedMarks.toString())
                    hint = "Marks"
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        (72 * b.root.resources.displayMetrics.density).toInt(),
                        (48 * b.root.resources.displayMetrics.density).toInt()
                    )
                }

                input.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        component.obtainedMarks =
                            (s?.toString()?.toIntOrNull() ?: 0)
                                .coerceIn(0, component.maxMarks)
                        updateBadge()
                    }
                    override fun afterTextChanged(s: Editable?) {}
                })

                row.addView(label)
                row.addView(input)
                b.llCustomMarks.addView(row)
            }

            updateBadge()
        }

        override fun getItemCount() = list.size
    }

}
