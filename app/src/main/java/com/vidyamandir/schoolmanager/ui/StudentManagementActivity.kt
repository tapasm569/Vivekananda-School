package com.vidyamandir.schoolmanager.ui

import android.content.Intent
import android.os.Bundle
import java.util.Locale
import java.util.Date
import java.text.SimpleDateFormat
import com.vidyamandir.schoolmanager.util.ThermalReceiptPrinter
import com.vidyamandir.schoolmanager.util.PaymentReceiptPdf
import androidx.appcompat.app.AlertDialog
import android.widget.Toast
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
        binding.rvStudents.adapter = StudentAdapter(filtered) { student ->
            startActivity(Intent(this, StudentReceiptHistoryActivity::class.java)
                .putExtra("STUDENT_ID", student.id))
        }
    }

    private fun openStudentReceipts(student: Student) {
        lifecycleScope.launch(Dispatchers.IO) {
            val payments = db.schoolDao().getAllPayments().first()
                .filter { it.studentId == student.id }
                .sortedByDescending { it.paymentDate }

            withContext(Dispatchers.Main) {
                if (isFinishing || isDestroyed) return@withContext

                if (payments.isEmpty()) {
                    android.widget.Toast.makeText(
                        this@StudentManagementActivity,
                        "No payment receipts found for ${student.fullName}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    return@withContext
                }

                val formatter = SimpleDateFormat(
                    "dd MMM yyyy, hh:mm a", Locale.getDefault()
                )
                val labels = payments.map {
                    "${it.receiptNo}  •  ₹${String.format(Locale.US, "%.2f", it.totalAmount)}  •  ${formatter.format(Date(it.paymentDate))}"
                }.toTypedArray()

                AlertDialog.Builder(this@StudentManagementActivity)
                    .setTitle("Payment Receipts — ${student.fullName}")
                    .setItems(labels) { _, index ->
                        showReceiptActions(student, payments[index])
                    }
                    .setNegativeButton("Close", null)
                    .show()
            }
        }
    }

    private fun showReceiptActions(
        student: Student,
        payment: com.vidyamandir.schoolmanager.data.FeePayment
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            val file = runCatching {
                PaymentReceiptPdf.create(this@StudentManagementActivity, student, payment)
            }.getOrNull()

            withContext(Dispatchers.Main) {
                if (isFinishing || isDestroyed) return@withContext
                if (file == null) {
                    Toast.makeText(this@StudentManagementActivity,
                        "Could not prepare receipt PDF", Toast.LENGTH_LONG).show()
                    return@withContext
                }

                AlertDialog.Builder(this@StudentManagementActivity)
                    .setTitle(payment.receiptNo)
                    .setItems(arrayOf("View PDF", "Print PDF", "Thermal Printer", "Share PDF")) { _, choice ->
                        runCatching {
                            when (choice) {
                                0 -> PaymentReceiptPdf.view(this@StudentManagementActivity, file)
                                1 -> PaymentReceiptPdf.print(this@StudentManagementActivity, file)
                                2 -> ThermalReceiptPrinter.choosePrinterAndPrint(
                                    this@StudentManagementActivity,
                                    PaymentReceiptPdf.thermalText(student, payment)
                                )
                                3 -> PaymentReceiptPdf.share(this@StudentManagementActivity, file)
                            }
                        }.onFailure {
                            Toast.makeText(this@StudentManagementActivity,
                                "Receipt action failed: ${it.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    class StudentAdapter(
        private val list: List<Student>,
        private val onReceiptsClick: (Student) -> Unit
    ) :
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

            ImageHelper.loadProfilePhoto(s.photoUri, holder.binding.ivStudentAvatar, android.R.drawable.sym_def_app_icon)
            holder.binding.root.setOnClickListener { StudentCardActions.openProfile(holder.itemView.context, s) }
            holder.binding.btnStudentWhatsApp.setOnClickListener { StudentCardActions.whatsapp(holder.itemView.context, s) }
            holder.binding.btnStudentCall.setOnClickListener { StudentCardActions.call(holder.itemView.context, s) }
            holder.binding.btnStudentReceipts.setOnClickListener {
                onReceiptsClick(s)
            }
            holder.binding.btnCollectDue.visibility = android.view.View.VISIBLE
            holder.binding.btnCollectDue.setOnClickListener {
                StudentCardActions.collect(holder.itemView.context, s)
            }
        }

        override fun getItemCount() = list.size
    }
}
