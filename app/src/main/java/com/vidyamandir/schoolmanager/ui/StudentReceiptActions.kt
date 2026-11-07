package com.vidyamandir.schoolmanager.ui

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LifecycleCoroutineScope
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.data.Student
import com.vidyamandir.schoolmanager.util.PaymentReceiptPdf
import com.vidyamandir.schoolmanager.util.ThermalReceiptPrinter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StudentReceiptActions {

    fun show(
        context: Context,
        activity: Activity,
        scope: LifecycleCoroutineScope,
        db: AppDatabase,
        student: Student
    ) {
        scope.launch(Dispatchers.IO) {
            val payments = db.schoolDao()
                .getStudentPayments(student.id, 2026)
                .first()
                .sortedByDescending { it.paymentDate }

            withContext(Dispatchers.Main) {
                if (activity.isFinishing || activity.isDestroyed) return@withContext

                if (payments.isEmpty()) {
                    Toast.makeText(
                        context,
                        "No payment receipts found for ${student.fullName}.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@withContext
                }

                val formatter = SimpleDateFormat(
                    "dd MMM yyyy, hh:mm a",
                    Locale.getDefault()
                )

                val labels = payments.map {
                    "${it.receiptNo}  •  ₹${String.format(Locale.US, "%.2f", it.totalAmount)}  •  ${formatter.format(Date(it.paymentDate))}"
                }.toTypedArray()

                AlertDialog.Builder(context)
                    .setTitle("Payment Receipts — ${student.fullName}")
                    .setItems(labels) { _, index ->
                        showReceiptOptions(
                            context,
                            activity,
                            scope,
                            student,
                            payments[index]
                        )
                    }
                    .setNegativeButton("Close", null)
                    .show()
            }
        }
    }

    private fun showReceiptOptions(
        context: Context,
        activity: Activity,
        scope: LifecycleCoroutineScope,
        student: Student,
        payment: com.vidyamandir.schoolmanager.data.FeePayment
    ) {
        scope.launch(Dispatchers.IO) {
            val file = runCatching {
                PaymentReceiptPdf.create(context.applicationContext, student, payment)
            }.getOrNull()

            withContext(Dispatchers.Main) {
                if (activity.isFinishing || activity.isDestroyed) return@withContext

                if (file == null) {
                    Toast.makeText(
                        context,
                        "Could not prepare receipt PDF.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@withContext
                }

                AlertDialog.Builder(context)
                    .setTitle(payment.receiptNo)
                    .setItems(
                        arrayOf("View PDF", "Print PDF", "Thermal Printer", "Share PDF")
                    ) { _, choice ->
                        runCatching {
                            when (choice) {
                                0 -> PaymentReceiptPdf.view(context, file)
                                1 -> PaymentReceiptPdf.print(context, file)
                                2 -> ThermalReceiptPrinter.choosePrinterAndPrint(
                                    activity,
                                    PaymentReceiptPdf.thermalText(student, payment)
                                )
                                3 -> PaymentReceiptPdf.share(context, file)
                            }
                        }.onFailure {
                            Toast.makeText(
                                context,
                                "Unable to open receipt: ${it.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }
}
