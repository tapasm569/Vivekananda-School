package com.vidyamandir.schoolmanager.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.data.FeePayment
import com.vidyamandir.schoolmanager.data.Student
import com.vidyamandir.schoolmanager.databinding.ActivityStudentManagementBinding
import com.vidyamandir.schoolmanager.databinding.ItemMonthFeeBinding
import com.vidyamandir.schoolmanager.util.PaymentReceiptPdf
import com.vidyamandir.schoolmanager.util.ThermalReceiptPrinter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TrackPaymentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentManagementBinding
    private lateinit var db: AppDatabase
    private lateinit var summary: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.title = "Today's Tracked Payments"
        binding.fabAddStudent.hide()
        binding.tilClassFilter.visibility = android.view.View.GONE
        binding.toolbar.setNavigationOnClickListener { finish() }

        db = AppDatabase.getDatabase(this, lifecycleScope)
        binding.rvStudents.layoutManager = LinearLayoutManager(this)

        summary = TextView(this).apply {
            textSize = 16f
            setTextColor(android.graphics.Color.rgb(15, 61, 110))
            setPadding(18, 18, 18, 18)
            setBackgroundColor(android.graphics.Color.rgb(239, 246, 255))
        }

        val parent = binding.rvStudents.parent as ViewGroup
        parent.addView(summary, parent.indexOfChild(binding.rvStudents))

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        lifecycleScope.launch {
            db.schoolDao().getTodayPayments(cal.timeInMillis).collect { list ->
                val total = list.sumOf { it.totalAmount }
                summary.text = "TODAY'S COLLECTION\n₹${String.format(Locale.US, "%.2f", total)}\nTransactions: ${list.size}"

                binding.rvStudents.adapter = object :
                    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

                    override fun onCreateViewHolder(
                        parent: ViewGroup, viewType: Int
                    ): RecyclerView.ViewHolder {
                        val b = ItemMonthFeeBinding.inflate(
                            LayoutInflater.from(parent.context), parent, false
                        )
                        return object : RecyclerView.ViewHolder(b.root) {}
                    }

                    override fun onBindViewHolder(
                        holder: RecyclerView.ViewHolder, pos: Int
                    ) {
                        val payment = list[pos]
                        val b = ItemMonthFeeBinding.bind(holder.itemView)
                        val time = SimpleDateFormat(
                            "hh:mm a", Locale.getDefault()
                        ).format(Date(payment.paymentDate))

                        b.tvMonthName.text = "${payment.studentName} (${payment.receiptNo})"
                        b.tvFeeBreakdown.text =
                            "Base: ₹${payment.baseAmount} | Fine: ₹${payment.lateFeeAmount} | $time"
                        b.tvStatusBadge.text = "₹${String.format(Locale.US, "%.2f", payment.totalAmount)}"
                        b.cbSelectMonth.isEnabled = false
                        b.cbSelectMonth.isChecked = true

                        holder.itemView.setOnClickListener {
                            openPaymentReceipt(payment)
                        }
                    }

                    override fun getItemCount() = list.size
                }
            }
        }
    }

    private fun openPaymentReceipt(payment: FeePayment) {
        lifecycleScope.launch(Dispatchers.IO) {
            val student = db.schoolDao().getStudentById(payment.studentId)
            val file = if (student != null) {
                runCatching { PaymentReceiptPdf.create(this@TrackPaymentActivity, student, payment) }.getOrNull()
            } else null

            withContext(Dispatchers.Main) {
                if (isFinishing || isDestroyed) return@withContext
                if (student == null || file == null) {
                    Toast.makeText(this@TrackPaymentActivity,
                        "Student or receipt PDF not found", Toast.LENGTH_LONG).show()
                    return@withContext
                }
                showReceiptActions(student, payment, file)
            }
        }
    }

    private fun showReceiptActions(
        student: Student,
        payment: FeePayment,
        file: java.io.File
    ) {
        AlertDialog.Builder(this)
            .setTitle(payment.receiptNo)
            .setItems(arrayOf("View PDF", "Print PDF", "Thermal Printer", "Share PDF")) { _, choice ->
                runCatching {
                    when (choice) {
                        0 -> PaymentReceiptPdf.view(this, file)
                        1 -> PaymentReceiptPdf.print(this, file)
                        2 -> ThermalReceiptPrinter.choosePrinterAndPrint(
                            this, PaymentReceiptPdf.thermalText(student, payment)
                        )
                        3 -> PaymentReceiptPdf.share(this, file)
                    }
                }.onFailure {
                    Toast.makeText(this, "Receipt action failed: ${it.message}",
                        Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
