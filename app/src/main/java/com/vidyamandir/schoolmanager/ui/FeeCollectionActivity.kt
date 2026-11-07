package com.vidyamandir.schoolmanager.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
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
            currentlySelectedMonths = selectedMonths
            calculateTotals(selectedMonths)
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

            val student = visibleStudents.firstOrNull {
                studentLabel(it) == label
            }

            if (student != null) {
                selectStudent(student)
            }
        }

        binding.actvStudentSelect.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?, start: Int, count: Int, after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?, start: Int, before: Int, count: Int
            ) {
                if (!isSettingStudentText) {
                    if (selectedStudent != null) {
                        resetStudentSelection()
                    }
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
                    studentsList.map { it.displayClass }
                        .distinct()
                        .sorted()

                binding.actvClassSelect.setAdapter(
                    ArrayAdapter(
                        this@FeeCollectionActivity,
                        android.R.layout.simple_dropdown_item_1line,
                        classNames
                    )
                )

                val requestedId = intent.getLongExtra("STUDENT_ID", -1L)
                val requestedStudent = studentsList.firstOrNull {
                    it.id == requestedId
                }

                if (requestedStudent != null) {
                    selectedClassFilter = requestedStudent.displayClass
                    binding.actvClassSelect.setText(
                        selectedClassFilter, false
                    )
                    updateStudentOptions(studentLabel(requestedStudent))
                    selectStudent(requestedStudent)
                } else {
                    binding.actvClassSelect.setText("All Classes", false)
                    updateStudentOptions("")
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@FeeCollectionActivity,
                    "Unable to load students: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun studentLabel(student: Student): String {
        return "${student.fullName} (${student.displayClass}) - Roll ${student.rollNo}"
    }

    private fun updateStudentOptions(query: String) {
        visibleStudents = studentsList.filter { student ->
            val matchesClass = selectedClassFilter == "All Classes" ||
                student.displayClass.equals(
                    selectedClassFilter,
                    ignoreCase = true
                )

            matchesClass && (
                query.isBlank() ||
                    student.fullName.contains(query, ignoreCase = true) ||
                    student.admissionNo.contains(query, ignoreCase = true)
                )
        }

        val names = visibleStudents.map { studentLabel(it) }

        binding.actvStudentSelect.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                names
            )
        )

        binding.actvStudentSelect.threshold = 1
    }

    private fun selectStudent(student: Student) {
        selectedStudent = student

        isSettingStudentText = true
        binding.actvStudentSelect.setText(studentLabel(student), false)
        isSettingStudentText = false

        binding.btnSubmitPayment.isEnabled = true
        binding.btnViewPaymentHistory.isEnabled = true

        displayStudentInfo(student)
        currentlySelectedMonths = emptyList()
        calculateTotals(emptyList())
        loadStudentFeeMonths(student)
    }

    private fun resetStudentSelection() {
        selectedStudent = null
        currentlySelectedMonths = emptyList()

        isSettingStudentText = true
        binding.actvStudentSelect.setText("", false)
        isSettingStudentText = false

        binding.tvSelectedStudentName.text =
            "Student: Please select above"

        binding.tvSelectedStudentMeta.text =
            "Class and monthly fee will appear here"

        binding.btnSubmitPayment.isEnabled = false
        binding.btnViewPaymentHistory.isEnabled = false

        adapter.submitList(emptyList())
        calculateTotals(emptyList())
    }

    private fun displayStudentInfo(student: Student) {
        binding.tvSelectedStudentName.text =
            "${student.fullName} (Roll: ${student.rollNo})"

        binding.tvSelectedStudentMeta.text =
            "Class: ${student.displayClass} | Monthly Fee: ₹${student.monthlyFee}"
    }

    private fun loadStudentFeeMonths(student: Student) {
        lifecycleScope.launch {
            try {
                val data = withContext(Dispatchers.IO) {
                    val payments = db.schoolDao()
                        .getAllStudentPayments(student.id)
                        .first()

                    val rules = FeeCalculator.effectiveRules(db.schoolDao().getAllLateFeeRules())

                    Pair(
                        FeeCalculator.parsePaidPeriodKeys(payments),
                        rules
                    )
                }

                val statusList = FeeCalculator.calculateDueMonths(
                    student,
                    data.first,
                    data.second
                )

                if (selectedStudent?.id == student.id) {
                    adapter.submitList(statusList)
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@FeeCollectionActivity,
                    "Unable to load fee months: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun calculateTotals(selectedMonths: List<MonthDueStatus>) {
        val baseSum = selectedMonths.sumOf { it.baseFee }
        val lateSum = selectedMonths.sumOf { it.lateFee }
        val payableTotal = baseSum + lateSum

        binding.tvTotalBaseFee.text =
            "₹${String.format("%.2f", baseSum)}"

        binding.tvTotalLateFee.text =
            "₹${String.format("%.2f", lateSum)}"

        binding.tvTotalPayable.text =
            "₹${String.format("%.2f", payableTotal)}"
    }

    private fun submitPayment() {
        val student = selectedStudent

        if (student == null) {
            Toast.makeText(
                this,
                "Please select a student first",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val monthsToPay = currentlySelectedMonths

        if (monthsToPay.isEmpty()) {
            Toast.makeText(
                this,
                "Please select at least one month to pay",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val baseSum = monthsToPay.sumOf { it.baseFee }
        val lateSum = monthsToPay.sumOf { it.lateFee }
        val total = baseSum + lateSum

        val monthsString = monthsToPay.joinToString(",") {
            it.periodKey
        }

        val receipt = "REC-${System.currentTimeMillis()}"

        binding.btnSubmitPayment.isEnabled = false

        lifecycleScope.launch {
            try {
                val payment = FeePayment(
                    studentId = student.id,
                    studentName = student.fullName,
                    receiptNo = receipt,
                    academicYear = monthsToPay.first().academicYear,
                    paidMonths = monthsString,
                    baseAmount = baseSum,
                    lateFeeAmount = lateSum,
                    totalAmount = total,
                    paymentDate = System.currentTimeMillis()
                )

                val pdfFile = withContext(Dispatchers.IO) {
                    db.schoolDao().recordPayment(payment)

                    PaymentReceiptPdf.create(
                        this@FeeCollectionActivity,
                        student,
                        payment
                    )
                }

                Toast.makeText(
                    this@FeeCollectionActivity,
                    "Payment saved. Receipt: $receipt",
                    Toast.LENGTH_LONG
                ).show()

                showReceiptActions(pdfFile, receipt, student, payment)

                if (selectedStudent?.id == student.id) {
                    loadStudentFeeMonths(student)
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@FeeCollectionActivity,
                    "Payment failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                binding.btnSubmitPayment.isEnabled =
                    selectedStudent != null
            }
        }
    }

    private fun openPaymentHistory() {
        val student = selectedStudent ?: return

        startActivity(
            android.content.Intent(
                this,
                StudentReceiptHistoryActivity::class.java
            ).putExtra("STUDENT_ID", student.id)
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
            .setMessage(
                "Receipt $receipt is stored on this phone. " +
                    "You can reopen it anytime from the Paid List."
            )
            .setItems(
                arrayOf(
                    "View PDF",
                    "Print PDF",
                    "Thermal Printer",
                    "Share PDF"
                )
            ) { _, choice ->
                runCatching {
                    when (choice) {
                        0 -> PaymentReceiptPdf.view(this, file)
                        1 -> PaymentReceiptPdf.print(this, file)
                        2 -> ThermalReceiptPrinter.choosePrinterAndPrint(
                            this,
                            PaymentReceiptPdf.thermalText(student, payment)
                        )
                        3 -> PaymentReceiptPdf.share(this, file)
                    }
                }.onFailure {
                    Toast.makeText(
                        this,
                        "Receipt action failed: ${it.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }
}
