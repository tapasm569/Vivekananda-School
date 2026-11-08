package com.vidyamandir.schoolmanager.ui

import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.vidyamandir.schoolmanager.data.*
import com.vidyamandir.schoolmanager.util.PaymentReceiptPdf
import com.vidyamandir.schoolmanager.util.ThermalReceiptPrinter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StudentReceiptHistoryActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var list: LinearLayout
    private lateinit var summary: TextView
    private var student: Student? = null

    private fun formatPaidMonths(value: String, academicYear: Int): String {
        val monthNames = listOf(
            "January", "February", "March", "April",
            "May", "June", "July", "August",
            "September", "October", "November", "December"
        )

        return value.split(",").mapNotNull { raw ->
            val token = raw.trim()
            if (token.isBlank()) return@mapNotNull null

            val parts = token.split(":")
            val keyPart = parts[0].trim()
            val amtPart = parts.getOrNull(1)?.trim()

            val keySplit = keyPart.split("-")
            val year: Int
            val month: Int

            if (keySplit.size == 2) {
                year = keySplit[0].toIntOrNull() ?: return@mapNotNull token
                month = keySplit[1].toIntOrNull() ?: return@mapNotNull token
            } else {
                year = academicYear
                month = keyPart.toIntOrNull() ?: return@mapNotNull token
            }

            if (month !in 1..12) token
            else {
                val name = "${monthNames[month - 1]} $year"
                if (amtPart != null) "$name (₹$amtPart)" else name
            }
        }.joinToString(", ")
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = AppDatabase.getDatabase(this, lifecycleScope)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(245, 247, 251))
        }

        val toolbar = MaterialToolbar(this).apply {
            title = "Payment History"
            setTitleTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(22, 58, 105))
            setNavigationIcon(android.R.drawable.ic_media_previous)
            setNavigationOnClickListener { finish() }
        }
        root.addView(toolbar)

        summary = TextView(this).apply {
            textSize = 15f
            setTextColor(Color.rgb(22, 58, 105))
            setPadding(dp(18), dp(16), dp(18), dp(16))
        }
        root.addView(summary)

        val scroll = ScrollView(this)
        list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(4), dp(14), dp(20))
        }
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        loadHistory()
    }

    private fun loadHistory() {
        val id = intent.getLongExtra("STUDENT_ID", -1L)

        lifecycleScope.launch {
            val data = withContext(Dispatchers.IO) {
                val dao = db.schoolDao()
                val s = dao.getStudentById(id)
                val payments = dao.getAllStudentPayments(id).first()
                Pair(s, payments)
            }

            student = data.first
            val s = data.first
            val payments = data.second

            if (s == null) {
                Toast.makeText(this@StudentReceiptHistoryActivity,
                    "Student not found", Toast.LENGTH_LONG).show()
                finish()
                return@launch
            }

            summary.text = "${s.fullName}\n${s.displayClass} • Roll ${s.rollNo}\n" +
                "Total Transactions: ${payments.size}    " +
                "Total Paid: ₹${String.format(Locale.US, "%.2f", payments.sumOf { it.totalAmount })}"

            list.removeAllViews()

            if (payments.isEmpty()) {
                val empty = TextView(this@StudentReceiptHistoryActivity).apply {
                    text = "No payment transactions found."
                    textSize = 16f
                    gravity = Gravity.CENTER
                    setPadding(0, dp(40), 0, dp(40))
                }
                list.addView(empty)
                return@launch
            }

            val formatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

            payments.forEach { payment ->
                val card = MaterialCardView(this@StudentReceiptHistoryActivity).apply {
                    radius = dp(14).toFloat()
                    cardElevation = dp(2).toFloat()
                    setCardBackgroundColor(Color.WHITE)
                    setContentPadding(dp(16), dp(12), dp(16), dp(12))
                    isClickable = true
                    isFocusable = true
                }

                val content = LinearLayout(this@StudentReceiptHistoryActivity).apply {
                    orientation = LinearLayout.VERTICAL
                }

                val top = LinearLayout(this@StudentReceiptHistoryActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }

                val receipt = TextView(this@StudentReceiptHistoryActivity).apply {
                    text = payment.receiptNo
                    textSize = 15f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(Color.rgb(22, 58, 105))
                }
                top.addView(receipt, LinearLayout.LayoutParams(0, -2, 1f))

                val amount = TextView(this@StudentReceiptHistoryActivity).apply {
                    text = "₹${String.format(Locale.US, "%.2f", payment.totalAmount)}"
                    textSize = 16f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(Color.rgb(22, 163, 74))
                }
                top.addView(amount)
                content.addView(top)

                val date = TextView(this@StudentReceiptHistoryActivity).apply {
                    text = formatter.format(Date(payment.paymentDate))
                    textSize = 12f
                    setTextColor(Color.DKGRAY)
                    setPadding(0, dp(7), 0, dp(3))
                }
                content.addView(date)

                val details = TextView(this@StudentReceiptHistoryActivity).apply {
                    val waiverText = if (payment.waivedFineAmount > 0.0) "   •   Waived Fine: ₹${payment.waivedFineAmount}" else ""
                    text = "Academic Year: ${payment.academicYear}\n" +
                        "Paid Months: ${formatPaidMonths(payment.paidMonths, payment.academicYear)}\n" +
                        "Base Fee: ₹${payment.baseAmount}   •   Late Fee: ₹${payment.lateFeeAmount}$waiverText"
                    textSize = 12f
                    setTextColor(Color.GRAY)
                }
                content.addView(details)

                val action = TextView(this@StudentReceiptHistoryActivity).apply {
                    text = "VIEW / PRINT RECEIPT  →"
                    textSize = 12f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(Color.rgb(37, 99, 235))
                    setPadding(0, dp(12), 0, 0)
                }
                content.addView(action)

                card.addView(content)
                card.setOnClickListener { showReceiptActions(s, payment) }

                list.addView(card, LinearLayout.LayoutParams(-1, -2).apply {
                    bottomMargin = dp(12)
                })
            }
        }
    }

    private fun showReceiptActions(s: Student, payment: FeePayment) {
        lifecycleScope.launch {
            val file = withContext(Dispatchers.IO) {
                runCatching { PaymentReceiptPdf.create(this@StudentReceiptHistoryActivity, s, payment) }
                    .getOrNull()
            }

            if (file == null) {
                Toast.makeText(this@StudentReceiptHistoryActivity,
                    "Could not prepare receipt PDF", Toast.LENGTH_LONG).show()
                return@launch
            }

            AlertDialog.Builder(this@StudentReceiptHistoryActivity)
                .setTitle(payment.receiptNo)
                .setItems(arrayOf("View PDF", "Print PDF", "Thermal Printer", "Share PDF")) { _, choice ->
                    runCatching {
                        when (choice) {
                            0 -> PaymentReceiptPdf.view(this@StudentReceiptHistoryActivity, file)
                            1 -> PaymentReceiptPdf.print(this@StudentReceiptHistoryActivity, file)
                            2 -> ThermalReceiptPrinter.choosePrinterAndPrint(
                                this@StudentReceiptHistoryActivity,
              PaymentReceiptPdf.thermalText(s, payment)
                            )
                            3 -> PaymentReceiptPdf.share(this@StudentReceiptHistoryActivity, file)
                        }
                    }.onFailure {
                        Toast.makeText(this@StudentReceiptHistoryActivity,
                            "Receipt action failed: ${it.message}", Toast.LENGTH_LONG).show()
                    }
                }
                .setNegativeButton("Close", null)
                .show()
        }
    }
}
