package com.vidyamandir.schoolmanager.ui

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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class StudentDuesListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentManagementBinding
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mode = intent.getStringExtra("VIEW_MODE") ?: "DUE"
        binding.toolbar.title = if (mode == "DUE") "Defaulters / Due Balance List" else "Paid Students List"
        binding.fabAddStudent.hide()
        binding.toolbar.setNavigationOnClickListener { finish() }

        db = AppDatabase.getDatabase(this, lifecycleScope)
        binding.rvStudents.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            val students = db.schoolDao().getAllActiveStudents().first()
            val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
            val displayList = mutableListOf<Pair<Student, Double>>()

            for (student in students) {
                val payments = db.schoolDao().getStudentPayments(student.id, 2026).first()
                val paidMonths = payments.flatMap { p ->
                    p.paidMonths.split(",").filter { it.isNotBlank() }.map { it.trim().toInt() }
                }.toSet()

                val unpaidCount = (student.admissionMonth..currentMonth).count { !paidMonths.contains(it) }

                if (mode == "DUE" && unpaidCount > 0) {
                    displayList.add(Pair(student, unpaidCount * student.monthlyFee))
                } else if (mode == "PAID" && unpaidCount == 0) {
                    displayList.add(Pair(student, student.monthlyFee))
                }
            }

            binding.rvStudents.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                    val b = ItemStudentCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                    return object : RecyclerView.ViewHolder(b.root) {}
                }

                override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
                    val (s, amt) = displayList[pos]
                    val b = ItemStudentCardBinding.bind(holder.itemView)
                    b.tvStudentName.text = s.fullName
                    b.tvStudentMeta.text = "${s.className} (${s.section}) • Roll: ${s.rollNo}"
                    b.tvParentsName.text = "Phone: ${s.parentPhone}"
                    b.tvMonthlyFeeBadge.text = if (mode == "DUE") "Due: ₹$amt" else "PAID"
                    s.photoUri?.let { b.ivStudentAvatar.setImageURI(Uri.parse(it)) }
                }

                override fun getItemCount() = displayList.size
            }
        }
    }
}
