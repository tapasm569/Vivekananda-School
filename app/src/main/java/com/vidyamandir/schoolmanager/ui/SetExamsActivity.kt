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
import androidx.room.withTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.data.ExamDefinition
import com.vidyamandir.schoolmanager.data.ExamSubject
import com.vidyamandir.schoolmanager.data.ExamSubjectComponent
import com.vidyamandir.schoolmanager.databinding.ActivitySetExamsBinding
import com.vidyamandir.schoolmanager.databinding.ItemExamSubjectConfigBinding
import com.vidyamandir.schoolmanager.databinding.ItemMonthFeeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CustomComponentConfig(
    var name: String,
    var maxMarks: Int = 10
)

data class SubjectConfigItem(
    var name: String,
    var fullMarks: Int = 100,
    var hasWritten: Boolean = true,
    var maxWritten: Int = 70,
    var hasOral: Boolean = false,
    var maxOral: Int = 10,
    var hasPractical: Boolean = false,
    var maxPractical: Int = 20,
    val customComponents: MutableList<CustomComponentConfig> = mutableListOf()
)

class SetExamsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetExamsBinding
    private lateinit var db: AppDatabase
    private val configuredSubjects = mutableListOf<SubjectConfigItem>()
    private lateinit var subjectAdapter: SubjectConfigAdapter
    private var availableClassUnits = mutableListOf<String>()
    private val availableClassSubjects = mutableMapOf<String, List<String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetExamsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this, lifecycleScope)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupSubjectsRecycler()
        loadClasses()
        observeExams()

        binding.actvClassSelect.setOnItemClickListener { _, _, position, _ ->
            val selected = availableClassUnits.getOrNull(position)
            if (selected != null) populateSubjectsForClass(selected)
        }

        binding.btnAddSubject.setOnClickListener {
            configuredSubjects.add(SubjectConfigItem("New Subject", 100, true, 70, false, 10, false, 20))
            subjectAdapter.notifyItemInserted(configuredSubjects.size - 1)
        }

        binding.btnSaveExamConfig.setOnClickListener {
            saveExamSetup()
        }
    }

    private fun setupSubjectsRecycler() {
        subjectAdapter = SubjectConfigAdapter(configuredSubjects)
        binding.rvConfiguredSubjects.layoutManager = LinearLayoutManager(this)
        binding.rvConfiguredSubjects.adapter = subjectAdapter
    }

    private fun defaultSubjects(): List<SubjectConfigItem> {
        return listOf(
            SubjectConfigItem("First Language (Bengali)", 100, true, 70, true, 10, true, 20),
            SubjectConfigItem("Second Language (English)", 100, true, 80, true, 20, false, 0),
            SubjectConfigItem("Mathematics", 100, true, 80, false, 0, true, 20),
            SubjectConfigItem("Physical Science", 100, true, 70, false, 0, true, 30),
            SubjectConfigItem("Life Science", 100, true, 70, false, 0, true, 30),
            SubjectConfigItem("History", 100, true, 80, true, 20, false, 0),
            SubjectConfigItem("Geography", 100, true, 70, false, 0, true, 30)
        )
    }

    private fun populateSubjectsForClass(classLabel: String) {
        val names = availableClassSubjects[classLabel].orEmpty()

        val newSubjects = if (names.isNotEmpty()) {
            names.map { SubjectConfigItem(it) }
        } else {
            defaultSubjects()
        }

        configuredSubjects.clear()
        configuredSubjects.addAll(newSubjects)
        subjectAdapter.notifyDataSetChanged()
    }

    private fun loadClasses() {
        lifecycleScope.launch(Dispatchers.IO) {
            val classes = db.schoolDao().getAllClasses().first()
            availableClassUnits.clear()
            availableClassSubjects.clear()
            for (cls in classes) {
                for (unit in cls.getDisplayUnits()) {
                    val label = if (unit.second.isNotBlank()) "${unit.first} (Sec ${unit.second})" else unit.first
                    availableClassUnits.add(label)
                    availableClassSubjects[label] = cls.subjects
                        .split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .distinctBy { it.lowercase() }
                }
            }
            withContext(Dispatchers.Main) {
                if (availableClassUnits.isNotEmpty()) {
                    val adapter = ArrayAdapter(this@SetExamsActivity, android.R.layout.simple_dropdown_item_1line, availableClassUnits)
                    binding.actvClassSelect.setAdapter(adapter)
                    binding.actvClassSelect.setText(availableClassUnits[0], false)
                    populateSubjectsForClass(availableClassUnits[0])
                }
            }
        }
    }

    private fun saveExamSetup() {
        val selectedClass = binding.actvClassSelect.text?.toString()?.trim().orEmpty()
        val examName = binding.etExamName.text?.toString()?.trim().orEmpty()
        val examDate = binding.etExamDate.text?.toString()?.trim().orEmpty()
        val academicYear = 2026

        if (selectedClass.isBlank() || examName.isBlank() || examDate.isBlank()) {
            Toast.makeText(this, "Please enter class, exam name and date", Toast.LENGTH_SHORT).show()
            return
        }

        if (configuredSubjects.isEmpty()) {
            Toast.makeText(this, "Please add at least one subject", Toast.LENGTH_SHORT).show()
            return
        }

        val subjectNames = configuredSubjects.map { it.name.trim() }

        if (subjectNames.any { it.isBlank() } ||
            subjectNames.distinctBy { it.lowercase() }.size != subjectNames.size) {
            Toast.makeText(this, "Subject names must be filled and unique", Toast.LENGTH_SHORT).show()
            return
        }

        for (subject in configuredSubjects) {
            if (subject.fullMarks <= 0) {
                Toast.makeText(this, "Full marks must be greater than zero", Toast.LENGTH_SHORT).show()
                return
            }

            val customNames = subject.customComponents.map { it.name.trim() }

            if (customNames.any { it.isBlank() } ||
                customNames.distinctBy { it.lowercase() }.size != customNames.size ||
                subject.customComponents.any { it.maxMarks <= 0 }) {
                Toast.makeText(
                    this,
                    "Check custom component names and maximum marks for ${subject.name}",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
        }

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    db.withTransaction {
                        val existing = db.schoolDao().findExistingExam(
                            examName, selectedClass, academicYear
                        )

                        check(existing == null) {
                            "An exam with this name already exists for this class and year"
                        }

                        val examId = db.schoolDao().insertExam(
                            ExamDefinition(
                                examName = examName,
                                targetClass = selectedClass,
                                examDate = examDate,
                                academicYear = academicYear
                            )
                        )

                        val subjectEntities = configuredSubjects.map {
                            ExamSubject(
                                examId = examId,
                                subjectName = it.name.trim(),
                                fullMarks = it.fullMarks,
                                hasWritten = it.hasWritten,
                                maxWritten = it.maxWritten,
                                hasOral = it.hasOral,
                                maxOral = it.maxOral,
                                hasPractical = it.hasPractical,
                                maxPractical = it.maxPractical
                            )
                        }

                        val subjectIds = db.schoolDao().insertExamSubjects(subjectEntities)
                        val components = mutableListOf<ExamSubjectComponent>()

                        configuredSubjects.forEachIndexed { index, subject ->
                            val subjectId = subjectIds.getOrNull(index)
                                ?: error("Could not save subject ${subject.name}")

                            subject.customComponents.forEachIndexed { componentIndex, component ->
                                components.add(
                                    ExamSubjectComponent(
                                        examSubjectId = subjectId,
                                        componentName = component.name.trim(),
                                        maxMarks = component.maxMarks,
                                        position = componentIndex
                                    )
                                )
                            }
                        }

                        if (components.isNotEmpty()) {
                            db.schoolDao().insertExamSubjectComponents(components)
                        }
                    }
                }

                Toast.makeText(
                    this@SetExamsActivity,
                    "Exam Configuration Saved for $selectedClass!",
                    Toast.LENGTH_LONG
                ).show()

            } catch (e: Exception) {
                Toast.makeText(
                    this@SetExamsActivity,
                    "Exam not saved: ${e.message ?: "Unknown error"}",
                    Toast.LENGTH_LONG
                ).show()
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

        class ViewHolder(val b: ItemExamSubjectConfigBinding) :
            RecyclerView.ViewHolder(b.root) {
            val watchers = mutableMapOf<android.widget.EditText, TextWatcher>()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val b = ItemExamSubjectConfigBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(b)
        }

        private fun watch(
            holder: ViewHolder,
            field: android.widget.EditText,
            update: (String) -> Unit
        ) {
            holder.watchers.remove(field)?.let { field.removeTextChangedListener(it) }

            val watcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    update(s?.toString().orEmpty())
                }
                override fun afterTextChanged(s: Editable?) {}
            }

            holder.watchers[field] = watcher
            field.addTextChangedListener(watcher)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            val b = holder.b

            holder.watchers.forEach { (field, watcher) ->
                field.removeTextChangedListener(watcher)
            }
            holder.watchers.clear()

            b.etSubjectName.setText(item.name)
            b.etFullMarks.setText(item.fullMarks.toString())
            b.etMaxWritten.setText(item.maxWritten.toString())
            b.etMaxOral.setText(item.maxOral.toString())
            b.etMaxPractical.setText(item.maxPractical.toString())

            b.cbWritten.setOnCheckedChangeListener(null)
            b.cbOral.setOnCheckedChangeListener(null)
            b.cbPractical.setOnCheckedChangeListener(null)

            b.cbWritten.isChecked = item.hasWritten
            b.cbOral.isChecked = item.hasOral
            b.cbPractical.isChecked = item.hasPractical

            val recalculateFullMarks = {
                item.fullMarks =
                    (if (item.hasWritten) item.maxWritten else 0) +
                    (if (item.hasOral) item.maxOral else 0) +
                    (if (item.hasPractical) item.maxPractical else 0) +
                    item.customComponents.sumOf { it.maxMarks }

                b.etFullMarks.setText(item.fullMarks.toString())
            }

            b.etFullMarks.isFocusable = false
            b.etFullMarks.isFocusableInTouchMode = false
            b.etFullMarks.isClickable = false

            watch(holder, b.etSubjectName) { item.name = it }
            watch(holder, b.etMaxWritten) {
                item.maxWritten = it.toIntOrNull() ?: 0
                recalculateFullMarks()
            }
            watch(holder, b.etMaxOral) {
                item.maxOral = it.toIntOrNull() ?: 0
                recalculateFullMarks()
            }
            watch(holder, b.etMaxPractical) {
                item.maxPractical = it.toIntOrNull() ?: 0
                recalculateFullMarks()
            }

            b.cbWritten.setOnCheckedChangeListener { _, checked ->
                item.hasWritten = checked
                recalculateFullMarks()
            }
            b.cbOral.setOnCheckedChangeListener { _, checked ->
                item.hasOral = checked
                recalculateFullMarks()
            }
            b.cbPractical.setOnCheckedChangeListener { _, checked ->
                item.hasPractical = checked
                recalculateFullMarks()
            }

            b.llCustomComponents.removeAllViews()

            item.customComponents.forEachIndexed { index, component ->
                val row = android.widget.LinearLayout(b.root.context).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(0, 4, 0, 4)
                }

                val nameField = android.widget.EditText(b.root.context).apply {
                    hint = "Component name"
                    setSingleLine(true)
                    textSize = 13f
                    setText(component.name)
                    inputType = android.text.InputType.TYPE_CLASS_TEXT
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 2f
                    )
                }

                val marksField = android.widget.EditText(b.root.context).apply {
                    hint = "Max"
                    setSingleLine(true)
                    textSize = 13f
                    setText(component.maxMarks.toString())
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f
                    )
                }

                val deleteButton = android.widget.Button(b.root.context).apply {
                    text = "✕"
                    textSize = 12f
                    minWidth = 42
                    setPadding(0, 0, 0, 0)
                }

                nameField.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        component.name = s?.toString().orEmpty()
                    }
                    override fun afterTextChanged(s: Editable?) {}
                })

                marksField.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        component.maxMarks = s?.toString()?.toIntOrNull() ?: 0
                        recalculateFullMarks()
                    }
                    override fun afterTextChanged(s: Editable?) {}
                })

                deleteButton.setOnClickListener {
                    val currentPosition = holder.adapterPosition
                    if (currentPosition != RecyclerView.NO_POSITION) {
                        val currentItem = list[currentPosition]
                        currentItem.customComponents.remove(component)
                        notifyItemChanged(currentPosition)
                    }
                }

                row.addView(nameField)
                row.addView(marksField)
                row.addView(deleteButton)
                b.llCustomComponents.addView(row)
            }

            b.btnAddCustomComponent.setOnClickListener {
                val currentPosition = holder.adapterPosition
                if (currentPosition != RecyclerView.NO_POSITION) {
                    list[currentPosition].customComponents.add(
                        CustomComponentConfig("New Component", 10)
                    )
                    notifyItemChanged(currentPosition)
                }
            }

            recalculateFullMarks()

            b.btnDeleteSubject.setOnClickListener {
                val currentPosition = holder.adapterPosition
                if (currentPosition != RecyclerView.NO_POSITION) {
                    list.removeAt(currentPosition)
                    notifyItemRemoved(currentPosition)
                }
            }
        }

        override fun getItemCount() = list.size
    }
}
