package com.vidyamandir.schoolmanager.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.data.Student
import com.vidyamandir.schoolmanager.databinding.ActivityStudentManagementBinding
import com.vidyamandir.schoolmanager.databinding.ItemStudentCardBinding
import kotlinx.coroutines.launch

class StudentManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentManagementBinding
    private lateinit var db: AppDatabase

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

        lifecycleScope.launch {
            db.schoolDao().getAllActiveStudents().collect { list ->
                binding.rvStudents.adapter = StudentCardAdapter(list)
            }
        }
    }

    class StudentCardAdapter(private val list: List<Student>) :
        RecyclerView.Adapter<StudentCardAdapter.StudentViewHolder>() {

        class StudentViewHolder(val binding: ItemStudentCardBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
            val binding = ItemStudentCardBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return StudentViewHolder(binding)
        }

        override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
            val s = list[position]
            holder.binding.tvStudentName.text = s.fullName
            holder.binding.tvStudentMeta.text = "${s.className} (${s.section}) • Roll: ${s.rollNo}"
            holder.binding.tvParentsName.text = "Father: ${s.fatherName} • 📞 ${s.parentPhone}"
            holder.binding.tvMonthlyFeeBadge.text = "₹${s.monthlyFee}/m"

            s.photoUri?.let {
                try {
                    holder.binding.ivStudentAvatar.setImageURI(Uri.parse(it))
                } catch (_: Exception) {}
            }
        }

        override fun getItemCount() = list.size
    }
}
