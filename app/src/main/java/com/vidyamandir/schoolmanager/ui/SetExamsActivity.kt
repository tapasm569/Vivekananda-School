package com.vidyamandir.schoolmanager.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.data.ExamDefinition
import com.vidyamandir.schoolmanager.data.ExamSubject
import com.vidyamandir.schoolmanager.databinding.ActivitySetExamsBinding
import com.vidyamandir.schoolmanager.databinding.ItemExamSubjectConfigBinding
import com.vidyamandir.schoolmanager.databinding.ItemMonthFeeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SubjectConfigItem(
    var name: String,
    var fullMarks: Int = 100,
    var hasWritten: Boolean = true,
    var maxWritten: Int = 70,
    var hasOral: Boolean = false,
    var maxOral: Int = 10,
    var hasPractical: Boolean = false,
    var maxPractical: Int = 20
)

class SetExamsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetExamsBinding
    private lateinit var db: AppDatabase
    private val configuredSubjects = mutableListOf<SubjectConfigItem>()
    private lateinit var subjectAdapter: SubjectConfigAdapter
    private var availableClassUnits = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetExamsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this, lifecycleScope)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupSubjectsRecycler()
        loadClasses()
        observeExams()

        binding.btnAddSubject.setOnClickListener {
            configuredSubjects.add(SubjectConfigItem("New Subject", 100, true, 70, false, 10, false, 20))
            subjectAdapter.notifyItemInserted(configuredSubjects.size - 1)
        }

        binding.btnSaveExamConfig.setOnClickListener {
            saveExamSetup()
        }
    }

    private fun setupSubjectsRecycler() {
        // Preload standard subjects
        configuredSubjects.add(SubjectConfigItem("First Language (Bengali)", 100, true, 70, true, 10, true, 20))
        configuredSubjects.add(SubjectConfigItem("Second Language (English)", 100, true, 80, true, 20, false, 0))
        configuredSubjects.add(SubjectConfigItem("Mathematics", 100, true, 80, false, 0, true, 20))
        configuredSubjects.add(SubjectConfigItem("Physical Science", 100, true, 70, false, 0, true, 30))
        configuredSubjects.add(SubjectConfigItem("Life Science", 100, true, 70, false, 0, true, 30))
        configuredSubjects.add(SubjectConfigItem("History", 100, true, 80, true, 20, false, 0))
        configuredSubjects.add(SubjectConfigItem("Geography", 100, true, 70, false, 0, true, 30))

        subjectAdapter = SubjectConfigAdapter(configuredSubjects)
        binding.rvConfiguredSubjects.layoutManager = LinearLayoutManager(this)
        binding.rvConfiguredSubjects.adapter = subjectAdapter
    }

    private fun loadClasses() {
        lifecycleScope.launch(Dispatchers.IO) {
            val classes = db.schoolDao().getAllClasses().first()
            availableClassUnits.clear()
            for (cls in classes) {
                for (unit in cls.getDisplayUnits()) {
                    val label = if (unit.second.isNotBlank()) "${unit.first} (Sec ${unit.second})" else unit.first
                    availableClassUnits.add(label)
                }
            }
            withContext(Dispatchers.Main) {
                if (availableClassUnits.isNotEmpty()) {
                    val adapter = ArrayAdapter(this@SetExamsActivity, android.R.layout.simple_dropdown_item_1line, availableClassUnits)
                    binding.actvClassSelect.setAdapter(adapter)
                    binding.actvClassSelect.setText(availableClassUnits[0], false)
                }
            }
        }
    }

    private fun saveExamSetup() {
        val selectedClass = binding.actvClassSelect.text?.toString()?.trim() ?: ""
        val examName = binding.etExamName.text?.toString()?.trim() ?: ""
        val examDate = binding.etExamDate.text?.toString()?.trim() ?: "2026-05-10"

        if (selectedClass.isEmpty() || examName.isEmpty()) {
            Toast.makeText(this, "Please select class and exam name", Toast.LENGTH_SHORT).show()
            return
        }

        if (configuredSubjects.isEmpty()) {
            Toast.makeText(this, "Please add at least one subject", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val examId = db.schoolDao().insertExam(
                ExamDefinition(
                    examName = examName,
                    targetClass = selectedClass,
                    examDate = examDate,
                    academicYear = 2026
                )
            )

            val subjectEntities = configuredSubjects.map {
                ExamSubject(
                    examId = examId,
                    subjectName = it.name,
                    fullMarks = it.fullMarks,
                    hasWritten = it.hasWritten,
                    maxWritten = it.maxWritten,
                    hasOral = it.hasOral,
                    maxOral = it.maxOral,
                    hasPractical = it.hasPractical,
                    maxPractical = it.maxPractical
                )
            }
            db.schoolDao().insertExamSubjects(subjectEntities)

            withContext(Dispatchers.Main) {
                Toast.makeText(this@SetExamsActivity, "Exam Configuration Saved for $selectedClass!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeExams() {
        binding.rvExistingExams.layoutManager = LinearLayoutManager(this)
        lifecycleScope.launch {
            db.schoolDao().getAllExams().collect { exams ->
                binding.rvExistingExams.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                        val b = ItemMonthFeeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                        return object : RecyclerView.ViewHolder(b.root) {}
                    }
                    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
                        val ex = exams[pos]
                        val b = ItemMonthFeeBinding.bind(holder.itemView)
                        b.tvMonthName.text = "${ex.examName} (${ex.targetClass})"
                        b.tvFeeBreakdown.text = "Date: ${ex.examDate} • Academic Year: ${ex.academicYear}"
                        b.tvStatusBadge.text = "READY"
                        b.cbSelectMonth.visibility = android.view.View.GONE
                    }
                    override fun getItemCount() = exams.size
                }
            }
        }
    }

    class SubjectConfigAdapter(private val list: MutableList<SubjectConfigItem>) :
        RecyclerView.Adapter<SubjectConfigAdapter.ViewHolder>() {

        class ViewHolder(val b: ItemExamSubjectConfigBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val b = ItemExamSubjectConfigBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(b)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.b.etSubjectName.setText(item.name)
            holder.b.etFullMarks.setText(item.fullMarks.toString())

            holder.b.cbWritten.isChecked = item.hasWritten
            holder.b.etMaxWritten.setText(item.maxWritten.toString())

            holder.b.cbOral.isChecked = item.hasOral
            holder.b.etMaxOral.setText(item.maxOral.toString())

            holder.b.cbPractical.isChecked = item.hasPractical
            holder.b.etMaxPractical.setText(item.maxPractical.toString())

            holder.b.etSubjectName.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    item.name = s.toString()
                }
                override fun afterTextChanged(s: Editable?) {}
            })

            holder.b.cbWritten.setOnCheckedChangeListener { _, isChecked -> item.hasWritten = isChecked }
            holder.b.cbOral.setOnCheckedChangeListener { _, isChecked -> item.hasOral = isChecked }
            holder.b.cbPractical.setOnCheckedChangeListener { _, isChecked -> item.hasPractical = isChecked }

            holder.b.btnDeleteSubject.setOnClickListener {
                val currentPos = holder.adapterPosition
                if (currentPos in list.indices) {
                    list.removeAt(currentPos)
                    notifyItemRemoved(currentPos)
                }
            }
        }

        override fun getItemCount() = list.size
    }
}
