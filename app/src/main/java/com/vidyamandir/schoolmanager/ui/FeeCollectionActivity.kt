package com.vidyamandir.schoolmanager.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.data.FeePayment
import com.vidyamandir.schoolmanager.data.LateFeeRule
import com.vidyamandir.schoolmanager.data.Student
import com.vidyamandir.schoolmanager.databinding.ActivityFeeCounterBinding
import com.vidyamandir.schoolmanager.domain.FeeCalculator
import com.vidyamandir.schoolmanager.domain.MonthDueStatus
import com.vidyamandir.schoolmanager.util.ThermalReceiptPrinter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FeeCollectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFeeCounterBinding
    private lateinit var adapter: MonthAdapter
    private lateinit var db: AppDatabase

    private var studentsList: List<Student> = emptyList()
    private var selectedStudent: Student? = null
    private var activeRules: List<LateFeeRule> = emptyList()
    private var currentlySelectedMonths: List<MonthDueStatus> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeeCounterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this, lifecycleScope)

        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = MonthAdapter { selectedMonths ->
            currentlySelectedMonths = selectedMonths
            calculateTotals(selectedMonths)
        }
        binding.rvMonths.layoutManager = LinearLayoutManager(this)
        binding.rvMonths.adapter = adapter

        loadInitialData()

        binding.actvStudentSelect.setOnItemClickListener { _, _, position, _ ->
            selectedStudent = studentsList[position]
            displayStudentInfo(selectedStudent!!)
            loadStudentFeeMonths(selectedStudent!!)
        }

        binding.btnSubmitPayment.setOnClickListener {
            submitPayment()
        }
    }

    private fun loadInitialData() {
        lifecycleScope.launch(Dispatchers.IO) {
            activeRules = db.schoolDao().getAllLateFeeRules()
            studentsList = db.schoolDao().getAllActiveStudents().first()

            withContext(Dispatchers.Main) {
                if (studentsList.isNotEmpty()) {
                    val names = studentsList.map { "${it.fullName} (${it.displayClass}) - Roll ${it.rollNo}" }
                    val arrayAdapter = ArrayAdapter(this@FeeCollectionActivity, android.R.layout.simple_dropdown_item_1line, names)
                    binding.actvStudentSelect.setAdapter(arrayAdapter)
                    val requestedId = intent.getLongExtra("STUDENT_ID", -1L)
                    if (requestedId > 0L) {
                        val index = studentsList.indexOfFirst { it.id == requestedId }
                        if (index >= 0) {
                            selectedStudent = studentsList[index]
                            binding.actvStudentSelect.setText(names[index], false)
                            displayStudentInfo(selectedStudent!!)
                            loadStudentFeeMonths(selectedStudent!!)
                        }
                    }
                }
            }
        }
    }

    private fun displayStudentInfo(student: Student) {
        binding.tvSelectedStudentName.text = "${student.fullName} (Roll: ${student.rollNo})"
        binding.tvSelectedStudentMeta.text = "Class: ${student.displayClass} | Monthly Fee: ₹${student.monthlyFee}"
    }

    private fun loadStudentFeeMonths(student: Student) {
        lifecycleScope.launch(Dispatchers.IO) {
            val payments = db.schoolDao().getStudentPayments(student.id, 2026).first()
            val paidMonthsSet = mutableSetOf<Int>()

            payments.forEach { p ->
                p.paidMonths.split(",").filter { it.isNotBlank() }.forEach { m ->
                    paidMonthsSet.add(m.trim().toInt())
                }
            }

            val statusList = FeeCalculator.calculateDueMonths(student, paidMonthsSet, activeRules)

            withContext(Dispatchers.Main) {
                adapter.submitList(statusList)
            }
        }
    }

    private fun calculateTotals(selectedMonths: List<MonthDueStatus>) {
        val baseSum = selectedMonths.sumOf { it.baseFee }
        val lateSum = selectedMonths.sumOf { it.lateFee }
        val payableTotal = baseSum + lateSum

        binding.tvTotalBaseFee.text = "₹${String.format("%.2f", baseSum)}"
        binding.tvTotalLateFee.text = "₹${String.format("%.2f", lateSum)}"
        binding.tvTotalPayable.text = "₹${String.format("%.2f", payableTotal)}"
    }

    private fun submitPayment() {
        val student = selectedStudent
        if (student == null) {
            Toast.makeText(this, "Please select a student first", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentlySelectedMonths.isEmpty()) {
            Toast.makeText(this, "Please select at least one month to pay", Toast.LENGTH_SHORT).show()
            return
        }

        val baseSum = currentlySelectedMonths.sumOf { it.baseFee }
        val lateSum = currentlySelectedMonths.sumOf { it.lateFee }
        val total = baseSum + lateSum
        val monthsString = currentlySelectedMonths.joinToString(",") { it.monthIndex.toString() }
        val receipt = "REC-${System.currentTimeMillis().toString().takeLast(6)}"

        lifecycleScope.launch(Dispatchers.IO) {
            val payment = FeePayment(
                studentId = student.id,
                studentName = student.fullName,
                receiptNo = receipt,
                academicYear = 2026,
                paidMonths = monthsString,
                baseAmount = baseSum,
                lateFeeAmount = lateSum,
                totalAmount = total,
                paymentDate = System.currentTimeMillis()
            )
            db.schoolDao().recordPayment(payment)

            withContext(Dispatchers.Main) {
                Toast.makeText(this@FeeCollectionActivity, "Payment recorded. Receipt: $receipt", Toast.LENGTH_LONG).show()
                shareReceipt(student, receipt, monthsString, baseSum, lateSum, total)
                loadStudentFeeMonths(student)
            }
        }
    }

    private fun shareReceipt(student: Student, receipt: String, months: String, base: Double, late: Double, total: Double) {
        val monthNames = months.split(",").mapNotNull { it.toIntOrNull() }.mapNotNull { index ->
            java.text.DateFormatSymbols().months.getOrNull(index - 1)
        }.joinToString(", ")
        val date = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
        val message = """VIVEKANANDA VIDYAMANDIR
Dhupguri, Jalpaiguri
FEE PAYMENT RECEIPT
================================
Receipt No: $receipt
Date: $date
Student: ${student.fullName}
Admission No: ${student.admissionNo}
Class: ${student.displayClass} | Roll: ${student.rollNo}
Academic Year: 2026
Paid For: $monthNames
--------------------------------
Base Fee: Rs. ${String.format("%.2f", base)}
Late Fee: Rs. ${String.format("%.2f", late)}
TOTAL PAID: Rs. ${String.format("%.2f", total)}
================================
Payment Status: PAID
Thank you for your payment.
""".trimIndent()
        val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, message)
        }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Payment receipt generated")
            .setMessage("Receipt $receipt is saved in payment history. Choose how to deliver it.")
            .setPositiveButton("WhatsApp") { _, _ ->
                try {
                    send.setPackage("com.whatsapp")
                    startActivity(send)
                } catch (_: Exception) {
                    send.setPackage(null)
                    startActivity(android.content.Intent.createChooser(send, "Share payment receipt"))
                }
            }
            .setNegativeButton("Print receipt") { _, _ ->
                ThermalReceiptPrinter.choosePrinterAndPrint(this, message)
            }
            .setNeutralButton("Other apps") { _, _ ->
                send.setPackage(null)
                startActivity(android.content.Intent.createChooser(send, "Share payment receipt"))
            }
            .show()
    }

}
