package com.vidyamandir.schoolmanager.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.data.Teacher
import com.vidyamandir.schoolmanager.databinding.ActivityAddStudentBinding
import com.vidyamandir.schoolmanager.databinding.ActivityStudentManagementBinding
import com.vidyamandir.schoolmanager.databinding.ItemStudentCardBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TeacherManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentManagementBinding
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.title = "Teacher & Staff Directory"
        binding.fabAddStudent.text = "Add Teacher"
        binding.toolbar.setNavigationOnClickListener { finish() }

        db = AppDatabase.getDatabase(this, lifecycleScope)
        binding.rvStudents.layoutManager = LinearLayoutManager(this)

        binding.fabAddStudent.setOnClickListener {
            // Quick Add Teacher dialog/logic
            showAddTeacherDialog()
        }

        lifecycleScope.launch {
            db.schoolDao().getAllTeachers().collect { teachers ->
                binding.rvStudents.adapter = TeacherAdapter(teachers)
            }
        }
    }

    private fun showAddTeacherDialog() {
        val teacher = Teacher(
            name = "Achintya Mondal",
            phone = "9876543210",
            password = "teacher@2026"
        )
        lifecycleScope.launch(Dispatchers.IO) {
            db.schoolDao().insertTeacher(teacher)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@TeacherManagementActivity, "Sample Teacher Added! Password: teacher@2026", Toast.LENGTH_SHORT).show()
            }
        }
    }

    class TeacherAdapter(private val list: List<Teacher>) :
        RecyclerView.Adapter<TeacherAdapter.TeacherViewHolder>() {

        class TeacherViewHolder(val binding: ItemStudentCardBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeacherViewHolder {
            val binding = ItemStudentCardBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return TeacherViewHolder(binding)
        }

        override fun onBindViewHolder(holder: TeacherViewHolder, position: Int) {
            val t = list[position]
            holder.binding.tvStudentName.text = t.name
            holder.binding.tvStudentMeta.text = "Staff • Contact: ${t.phone}"
            holder.binding.tvParentsName.text = "Password: ••••••••"
            holder.binding.tvMonthlyFeeBadge.text = "TEACHER"
        }

        override fun getItemCount() = list.size
    }
}
