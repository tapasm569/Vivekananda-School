package com.vidyamandir.schoolmanager.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.data.Student
import com.vidyamandir.schoolmanager.databinding.ActivityStudentManagementBinding
import com.vidyamandir.schoolmanager.databinding.ItemStudentCardBinding
import com.vidyamandir.schoolmanager.util.ImageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StudentManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentManagementBinding
    private lateinit var db: AppDatabase
    private var allStudents: List<Student> = emptyList()
    private var selectedClassFilter = "All Classes"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this, lifecycleScope)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.rvStudents.layoutManager = LinearLayoutManager(this)

        binding.fabAddStudent.setOnClickListener {
            startActivity(Intent(this, AddStudentActivity::class.java))
        }

        loadClassFilters()
        observeStudents()
    }

    private fun loadClassFilters() {
        lifecycleScope.launch(Dispatchers.IO) {
            val classes = db.schoolDao().getAllClasses().first()
            val filterOptions = mutableListOf("All Classes")
            classes.forEach { cls ->
                cls.getDisplayUnits().forEach { unit ->
                    val text = if (unit.second.isNotBlank()) "${unit.first} (Sec ${unit.second})" else unit.first
                    filterOptions.add(text)
                }
            }

            withContext(Dispatchers.Main) {
                val adapter = ArrayAdapter(this@StudentManagementActivity, android.R.layout.simple_dropdown_item_1line, filterOptions)
                binding.actvClassFilter.setAdapter(adapter)
                binding.actvClassFilter.setText("All Classes", false)

                binding.actvClassFilter.setOnItemClickListener { _, _, position, _ ->
                    selectedClassFilter = filterOptions[position]
                    applyFilter()
                }
            }
        }
    }

    private fun observeStudents() {
        lifecycleScope.launch {
            db.schoolDao().getAllActiveStudents().collect { list ->
                allStudents = list
                applyFilter()
            }
        }
    }

    private fun applyFilter() {
        val filtered = if (selectedClassFilter == "All Classes") {
            allStudents
        } else {
            allStudents.filter { it.displayClass.equals(selectedClassFilter, ignoreCase = true) }
        }
        binding.rvStudents.adapter = StudentAdapter(filtered)
    }

    class StudentAdapter(private val list: List<Student>) :
        RecyclerView.Adapter<StudentAdapter.ViewHolder>() {

        class ViewHolder(val binding: ItemStudentCardBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val b = ItemStudentCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(b)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val s = list[position]
            holder.binding.tvStudentName.text = s.fullName
            holder.binding.tvStudentMeta.text = "${s.displayClass} • Roll: ${s.rollNo}"
            holder.binding.tvParentsName.text = "Father: ${s.fatherName} • 📞 ${s.parentPhone}"
            holder.binding.tvMonthlyFeeBadge.text = "₹${s.monthlyFee}/m"

            ImageHelper.loadProfilePhoto(
                s.photoUri,
                holder.binding.ivStudentAvatar,
                android.R.drawable.sym_def_app_icon
            )
        }

        override fun getItemCount() = list.size
    }
}
