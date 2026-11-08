package com.vidyamandir.schoolmanager.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.data.FeePayment
import com.vidyamandir.schoolmanager.data.Student
import com.vidyamandir.schoolmanager.databinding.ActivityFeeCounterBinding
import com.vidyamandir.schoolmanager.domain.FeeCalculator
import com.vidyamandir.schoolmanager.domain.MonthDueStatus
import com.vidyamandir.schoolmanager.util.FirebaseSyncManager
import com.vidyamandir.schoolmanager.util.PaymentReceiptPdf
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
    private var visibleStudents: List<Student> = emptyList()
    private var selectedStudent: Student? = null
    private var currentlySelectedMonths: List<MonthDueStatus> = emptyList()

    private var calculatedBaseSum: Double = 0.0
    private var calculatedLateFine: Double = 0.0
    private var userFineCharged: Double = 0.0
    private var userCashReceived: Double = 0.0

    private var isUpdatingInputs = false
    private var selectedClassFilter = "All Classes"
    private var isSettingStudentText = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFeeCounterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this, lifecycleScope)

        binding.toolbar.setNavigationIcon(
            androidx.appcompat.R.drawable.abc_ic_ab_back_material
        )
        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = MonthAdapter { selectedMonths ->
            currentlySelectedMonths = selectedMonths.sortedWith(
                compareBy({ it.academicYear }, { it.monthIndex })
            )
            onSelectedMonthsChanged(currentlySelectedMonths)
        }

        binding.rvMonths.layoutManager = LinearLayoutManager(this)
        binding.rvMonths.adapter = adapter

        binding.btnSubmitPayment.isEnabled = false
        binding.btnViewPaymentHistory.isEnabled = false

        binding.actvClassSelect.setOnItemClickListener { parent, _, position, _ ->
            selectedClassFilter = parent.getItemAtPosition(position).toString()
            resetStudentSelection()
            updateStudentOptions("")
        }

        binding.actvStudentSelect.threshold = 1

        binding.actvStudentSelect.setOnItemClickListener { parent, _, position, _ ->
            val label = parent.getItemAtPosition(position).toString()
            val student = visibleStudents.firstOrNull { studentLabel(it) == label }
            if (student != null) selectStudent(student)
        }

        binding.actvStudentSelect.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!isSettingStudentText && selectedStudent != null) {
                    resetStudentSelection()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // 1-Tap Waive Fine button
        binding.btnWaiveFine.setOnClickListener {
            isUpdatingInputs = true
            binding.etCustomFine.setText("0.00")
            userFineCharged = 0.0
            isUpdatingInputs = false
            recomputePayableAndAllocation()
        }

        // 1-Tap Full Pay button
        binding.btnFullPay.setOnClickListener {
            val totalBillable = calculatedBaseSum + userFineCharged
            isUpdatingInputs = true
            binding.etCashReceived.setText(String.format("%.2f", totalBillable))
            userCashReceived = totalBillable
            isUpdatingInputs = false
            recomputePayableAndAllocation()
        }

        // Custom Fine listener
        binding.etCustomFine.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!isUpdatingInputs) {
                    userFineCharged = s?.toString()?.toDoubleOrNull() ?: 0.0
                    val totalBillable = calculatedBaseSum + userFineCharged
                    isUpdatingInputs = true
                    binding.etCashReceived.setText(String.format("%.2f", totalBillable))
                    userCashReceived = totalBillable
                    isUpdatingInputs = false
                    recomputePayableAndAllocation()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Cash Received listener
        binding.etCashReceived.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!isUpdatingInputs) {
                    userCashReceived = s?.toString()?.toDoubleOrNull() ?: 0.0
                    recomputePayableAndAllocation()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnSubmitPayment.setOnClickListener {
            submitPayment()
        }

        binding.btnViewPaymentHistory.setOnClickListener {
            openPaymentHistory()
        }

        loadInitialData()
    }

    private fun loadInitialData() {
        lifecycleScope.launch {
            try {
                studentsList = withContext(Dispatchers.IO) {
                    db.schoolDao().getAllActiveStudents().first()
                }

                val classNames = listOf("All Classes") +
                    studentsList.map { it.displayClass }.distinct().sorted()

                binding.actvClassSelect.setAdapter(
                    ArrayAdapter(this@FeeCollectionActivity, android.R.layout.simple_dropdown_item_1line, classNames)
                )

                val requestedId = intent.getLongExtra("STUDENT_ID", -1L)
                val requestedStudent = studentsList.firstOrNull { it.id == requestedId }

                if (requestedStudent != null) {
                    selectedClassFilter = requestedStudent.displayClass
                    binding.actvClassSelect.setText(selectedClassFilter, false)
                    updateStudentOptions(studentLabel(requestedStudent))
                    selectStudent(requestedStudent)
                } else {
                    binding.actvClassSelect.setText("All Classes", false)
                    updateStudentOptions("")
                }
            } catch (e: Exception) {
                Toast.makeText(this@FeeCollectionActivity, "Unable to load students: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun studentLabel(student: Student): String {
        return "${student.fullName} (${student.displayClass}) - Roll ${student.rollNo}"
    }

    private fun updateStudentOptions(query: String) {
        visibleStudents = studentsList.filter { student ->
            val matchesClass = selectedClassFilter == "All Classes" ||
                student.displayClass.equals(selectedClassFilter, ignoreCase = true)

            matchesClass && (
                query.isBlank() ||
                    student.fullName.contains(query, ignoreCase = true) ||
                    student.admissionNo.contains(query, ignoreCase = true)
            )
        }

        val names = visibleStudents.map { studentLabel(it) }
        binding.actvStudentSelect.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, names)
        )
        binding.actvStudentSelect.threshold = 1
    }

    private fun selectStudent(student: Student) {
        selectedStudent = student
        isSettingStudentText = true
        binding.actvStudentSelect.setText(studentLabel(student), false)
        isSettingStudentText = false

        binding.btnSubmitPayment.isEnabled = false
        binding.btnViewPaymentHistory.isEnabled = true

        displayStudentInfo(student)
        currentlySelectedMonths = emptyList()
        onSelectedMonthsChanged(emptyList())
        loadStudentFeeMonths(student)
    }

    private fun resetStudentSelection() {
        selectedStudent = null
        currentlySelectedMonths = emptyList()

        isSettingStudentText = true
        binding.actvStudentSelect.setText("", false)
        isSettingStudentText = false

        binding.tvSelectedStudentName.text = "Student: Please select above"
        binding.tvSelectedStudentMeta.text = "Class and monthly fee will appear here"

        binding.btnSubmitPayment.isEnabled = false
        binding.btnViewPaymentHistory.isEnabled = false

        adapter.submitList(emptyList())
        onSelectedMonthsChanged(emptyList())
    }

    private fun displayStudentInfo(student: Student) {
        binding.tvSelectedStudentName.text = "${student.fullName} (Roll: ${student.rollNo})"
        binding.tvSelectedStudentMeta.text = "Class: ${student.displayClass} | Monthly Fee: ₹${student.monthlyFee}"
    }

    private fun loadStudentFeeMonths(student: Student) {
        lifecycleScope.launch {
            try {
                val data = withContext(Dispatchers.IO) {
                    val payments = db.schoolDao().getAllStudentPayments(student.id).first()
                    val rules = FeeCalculator.effectiveRules(db.schoolDao().getAllLateFeeRules())
                    val paidMap = FeeCalculator.parsePaidMonthAmounts(payments, student.monthlyFee)
                    Pair(paidMap, rules)
                }

                val statusList = FeeCalculator.calculateDueMonthsWithAmounts(
                    student, data.first, data.second
                )

                if (selectedStudent?.id == student.id) {
                    adapter.submitList(statusList)
                }
            } catch (e: Exception) {
                Toast.makeText(this@FeeCollectionActivity, "Unable to load fee months: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun onSelectedMonthsChanged(selectedMonths: List<MonthDueStatus>) {
        calculatedBaseSum = selectedMonths.sumOf { it.baseFee }
        calculatedLateFine = selectedMonths.sumOf { it.lateFee }
        userFineCharged = calculatedLateFine
        val total = calculatedBaseSum + userFineCharged
        userCashReceived = total

        isUpdatingInputs = true
        binding.tvTotalBaseFee.text = "₹${String.format("%.2f", calculatedBaseSum)}"
        binding.etCustomFine.setText(String.format("%.2f", userFineCharged))
        binding.etCashReceived.setText(String.format("%.2f", userCashReceived))
        isUpdatingInputs = false

        recomputePayableAndAllocation()
    }

    private fun recomputePayableAndAllocation() {
        val waivedFine = maxOf(0.0, calculatedLateFine - userFineCharged)
        val totalBillable = calculatedBaseSum + userFineCharged

        binding.tvTotalPayable.text = "₹${String.format("%.2f", totalBillable)}"
        binding.tvWaivedNote.text = "Calculated Fine: ₹${String.format("%.2f", calculatedLateFine)} (Waived: ₹${String.format("%.2f", waivedFine)})"

        if (currentlySelectedMonths.isEmpty()) {
            binding.tvAllocationPreview.text = "Select due months to begin"
            binding.btnSubmitPayment.isEnabled = false
            binding.btnSubmitPayment.text = "Confirm Payment & Record"
            return
        }

        // Sequential FIFO Allocation
        val actualFinePaid = minOf(userFineCharged, userCashReceived)
        val cashForBase = maxOf(0.0, userCashReceived - actualFinePaid)

        var remainingCash = cashForBase
        val lines = mutableListOf<String>()

        if (userFineCharged > 0.0) {
            lines.add("Fine Charged: ₹${String.format("%.2f", actualFinePaid)}")
        } else if (waivedFine > 0.0) {
            lines.add("Fine: Waived ₹${String.format("%.2f", waivedFine)}")
        }

        currentlySelectedMonths.forEach { m ->
            val needed = m.baseFee
            val allocated = minOf(remainingCash, needed)
            val remAfter = needed - allocated

            if (remAfter <= 0.01) {
                lines.add("${m.monthName}: ₹${String.format("%.0f", allocated)} (Fully Paid)")
            } else if (allocated > 0.0) {
                lines.add("${m.monthName}: ₹${String.format("%.0f", allocated)} (Partial, ₹${String.format("%.0f", remAfter)} Due)")
            } else {
                lines.add("${m.monthName}: ₹0 (Unpaid, ₹${String.format("%.0f", needed)} Due)")
            }
            remainingCash = maxOf(0.0, remainingCash - allocated)
        }

        binding.tvAllocationPreview.text = lines.joinToString("  •  ")

        val canSubmit = userCashReceived > 0.0 && currentlySelectedMonths.isNotEmpty()
        binding.btnSubmitPayment.isEnabled = canSubmit
        binding.btnSubmitPayment.text = if (canSubmit) {
            "Confirm Payment (₹${String.format("%.2f", userCashReceived)}) & Record"
        } else {
            "Confirm Payment & Record"
        }
    }

    private fun submitPayment() {
        val student = selectedStudent ?: return
        if (currentlySelectedMonths.isEmpty()) {
            Toast.makeText(this, "Select at least one month", Toast.LENGTH_SHORT).show()
            return
        }

        if (userCashReceived <= 0.0) {
            Toast.makeText(this, "Cash received must be greater than zero", Toast.LENGTH_SHORT).show()
            return
        }

        val totalBillable = calculatedBaseSum + userFineCharged
        if (userCashReceived > totalBillable + 0.01) {
            Toast.makeText(this, "Cash received exceeds total billable amount (₹$totalBillable)", Toast.LENGTH_SHORT).show()
            return
        }

        val actualFinePaid = minOf(userFineCharged, userCashReceived)
        val cashForBase = maxOf(0.0, userCashReceived - actualFinePaid)
        val waivedFine = maxOf(0.0, calculatedLateFine - userFineCharged)

        // Generate paid token string with allocated amounts
        var remainingCash = cashForBase
        val allocatedTokens = mutableListOf<String>()

        currentlySelectedMonths.forEach { m ->
            val needed = m.baseFee
            val allocated = minOf(remainingCash, needed)
            if (allocated > 0.0) {
                allocatedTokens.add("${m.periodKey}:${String.format(java.util.Locale.US, "%.2f", allocated)}")
            }
            remainingCash = maxOf(0.0, remainingCash - allocated)
        }

        val paidMonthsString = allocatedTokens.joinToString(",")
        val receipt = "REC-${System.currentTimeMillis()}"

        binding.btnSubmitPayment.isEnabled = false

        lifecycleScope.launch {
            try {
                val payment = FeePayment(
                    studentId = student.id,
                    studentName = student.fullName,
                    receiptNo = receipt,
                    academicYear = currentlySelectedMonths.first().academicYear,
                    paidMonths = paidMonthsString,
                    baseAmount = cashForBase,
                    lateFeeAmount = actualFinePaid,
                    totalAmount = userCashReceived,
                    paymentDate = System.currentTimeMillis(),
                    waivedFineAmount = waivedFine
                )

                val pdfFile = withContext(Dispatchers.IO) {
                    val id = db.schoolDao().recordPayment(payment)
                    val savedPayment = payment.copy(id = id)
                    FirebaseSyncManager.syncPayment(savedPayment)

                    PaymentReceiptPdf.create(
                        this@FeeCollectionActivity, student, savedPayment
                    )
                }

                Toast.makeText(this@FeeCollectionActivity, "Payment saved. Receipt: $receipt", Toast.LENGTH_LONG).show()
                showReceiptActions(pdfFile, receipt, student, payment)

                if (selectedStudent?.id == student.id) {
                    loadStudentFeeMonths(student)
                }
            } catch (e: Exception) {
                Toast.makeText(this@FeeCollectionActivity, "Payment failed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.btnSubmitPayment.isEnabled = selectedStudent != null
            }
        }
    }

    private fun openPaymentHistory() {
        val student = selectedStudent ?: return
        startActivity(
            android.content.Intent(this, StudentReceiptHistoryActivity::class.java)
                .putExtra("STUDENT_ID", student.id)
        )
    }

    private fun showReceiptActions(
        file: java.io.File,
        receipt: String,
        student: Student,
        payment: FeePayment
    ) {
        AlertDialog.Builder(this)
            .setTitle("Receipt saved successfully")
            .setMessage("Receipt $receipt recorded. Total: ₹${String.format("%.2f", payment.totalAmount)}.")
            .setItems(
                arrayOf("View PDF", "Print PDF", "Thermal Printer", "Share PDF")
            ) { _, choice ->
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
                    Toast.makeText(this, "Receipt action failed: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }
}
