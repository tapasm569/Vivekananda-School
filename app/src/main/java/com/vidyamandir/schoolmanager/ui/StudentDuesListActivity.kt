package com.vidyamandir.schoolmanager.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.domain.FeeCalculator
import com.vidyamandir.schoolmanager.data.Student
import com.vidyamandir.schoolmanager.databinding.ActivityStudentManagementBinding
import com.vidyamandir.schoolmanager.databinding.ItemStudentCardBinding
import com.vidyamandir.schoolmanager.util.ImageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

data class StudentDueEntry(
    val student: Student,
    val totalAmount: Double,
    val detailsText: String
)

class StudentDuesListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentManagementBinding
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mode = intent.getStringExtra("VIEW_MODE") ?: "DUE"
        binding.toolbar.title = if (mode == "DUE") "Due Balance Defaulters" else "Paid Students"
        binding.fabAddStudent.hide()
        binding.tilClassFilter.visibility = android.view.View.GONE
        binding.toolbar.setNavigationOnClickListener { finish() }

        db = AppDatabase.getDatabase(this, lifecycleScope)
        binding.rvStudents.layoutManager = LinearLayoutManager(this)

        loadDuesData(mode)
    }

    private fun loadDuesData(mode: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val students = db.schoolDao().getAllActiveStudents().first()
            val rules = FeeCalculator.effectiveRules(
                db.schoolDao().getAllLateFeeRules()
            )
            val displayList = mutableListOf<StudentDueEntry>()

            for (student in students) {
                val payments = db.schoolDao()
                    .getAllStudentPayments(student.id).first()
                val paidKeys = FeeCalculator.parsePaidPeriodKeys(payments)
                val statuses = FeeCalculator.calculateDueMonths(
                    student, paidKeys, rules
                ).filter { it.isDue }
                val unpaid = statuses.filter { !it.isPaid }

                if (mode == "DUE" && unpaid.isNotEmpty()) {
                    val dueAmt = unpaid.sumOf { it.baseFee + it.lateFee }
                    val text = "Unpaid: ${unpaid.size} month(s) • " +
                        "Penalty: ₹${String.format("%.2f", unpaid.sumOf { it.lateFee })}"
                    displayList.add(StudentDueEntry(student, dueAmt, text))
                } else if (mode == "PAID" && unpaid.isEmpty()) {
                    displayList.add(
                        StudentDueEntry(
                            student, 0.0, "All dues cleared up to current month"
                        )
                    )
                }
            }

            withContext(Dispatchers.Main) {
                binding.rvStudents.adapter = DuesAdapter(displayList, mode) { student ->
                    StudentReceiptActions.show(
                        this@StudentDuesListActivity,
                        this@StudentDuesListActivity,
                        lifecycleScope, db, student
                    )
                }
            }
        }
    }

    class DuesAdapter(
        private val list: List<StudentDueEntry>,
        private val mode: String,
        private val onReceiptsClick: (Student) -> Unit
    ) : RecyclerView.Adapter<DuesAdapter.ViewHolder>() {

        class ViewHolder(val binding: ItemStudentCardBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val b = ItemStudentCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(b)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val entry = list[position]
            val s = entry.student
            holder.binding.tvStudentName.text = s.fullName
            holder.binding.tvStudentMeta.text = "${s.displayClass} • Roll: ${s.rollNo}"
            holder.binding.tvParentsName.text = "${entry.detailsText} • 📞 ${s.parentPhone}"

            if (mode == "DUE") {
                holder.binding.tvMonthlyFeeBadge.text = "Due: ₹${entry.totalAmount}"
                holder.binding.tvMonthlyFeeBadge.setTextColor(android.graphics.Color.parseColor("#DC2626"))
                holder.binding.tvMonthlyFeeBadge.setBackgroundColor(android.graphics.Color.parseColor("#FEE2E2"))
            } else {
                holder.binding.tvMonthlyFeeBadge.text = "PAID"
                holder.binding.tvMonthlyFeeBadge.setTextColor(android.graphics.Color.parseColor("#16A34A"))
                holder.binding.tvMonthlyFeeBadge.setBackgroundColor(android.graphics.Color.parseColor("#DCFCE7"))
            }

            ImageHelper.loadProfilePhoto(s.photoUri, holder.binding.ivStudentAvatar, android.R.drawable.sym_def_app_icon)
            holder.binding.root.setOnClickListener { StudentCardActions.openProfile(holder.itemView.context, s) }
            holder.binding.btnStudentWhatsApp.setOnClickListener { StudentCardActions.whatsapp(holder.itemView.context, s) }
            holder.binding.btnStudentCall.setOnClickListener { StudentCardActions.call(holder.itemView.context, s) }
            holder.binding.btnCollectDue.visibility = if (mode == "DUE") android.view.View.VISIBLE else android.view.View.GONE
            holder.binding.btnCollectDue.setOnClickListener { StudentCardActions.collect(holder.itemView.context, s) }
            holder.binding.btnStudentReceipts.visibility = android.view.View.VISIBLE
            holder.binding.btnStudentReceipts.setOnClickListener { onReceiptsClick(s) }
        }

        override fun getItemCount() = list.size
    }
}
