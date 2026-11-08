package com.vidyamandir.schoolmanager.util

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.vidyamandir.schoolmanager.data.FeePayment
import com.vidyamandir.schoolmanager.data.Student
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PaymentReceiptPdf {
    fun create(context: Context, student: Student, payment: FeePayment): File {
        val directory = File(context.filesDir, "receipts").apply { mkdirs() }
        val safeName = payment.receiptNo.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val file = File(directory, "$safeName-${payment.paymentDate}.pdf")
        if (file.exists() && file.length() > 0L) return file

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val navy = android.graphics.Color.rgb(15, 61, 110)
        val green = android.graphics.Color.rgb(22, 163, 74)
        val ink = android.graphics.Color.rgb(30, 41, 59)
        val muted = android.graphics.Color.rgb(100, 116, 139)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        fun text(value: String, x: Float, y: Float, size: Float = 12f, color: Int = ink, bold: Boolean = false) {
            paint.color = color
            paint.textSize = size
            paint.typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(value, x, y, paint)
        }
        fun line(y: Float, color: Int = android.graphics.Color.LTGRAY) {
            paint.color = color
            paint.strokeWidth = 1f
            canvas.drawLine(42f, y, 553f, y, paint)
        }

        paint.color = navy
        canvas.drawRect(0f, 0f, 595f, 150f, paint)
        text("VIVEKANANDA VIDYAMANDIR", 42f, 55f, 23f, android.graphics.Color.WHITE, true)
        text("Dhupguri, Jalpaiguri", 42f, 82f, 13f, android.graphics.Color.WHITE)
        text("OFFICIAL FEE PAYMENT RECEIPT", 42f, 119f, 14f, android.graphics.Color.WHITE, true)

        text("Receipt No.", 42f, 190f, 10f, muted, true)
        text(payment.receiptNo, 42f, 213f, 17f, navy, true)
        text("Payment Date", 350f, 190f, 10f, muted, true)
        val date = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(payment.paymentDate))
        text(date, 350f, 213f, 12f, ink, true)
        line(235f)

        text("STUDENT DETAILS", 42f, 270f, 12f, navy, true)
        text("Student Name", 42f, 305f, 10f, muted)
        text(student.fullName, 42f, 326f, 13f, ink, true)
        text("Admission No.", 350f, 305f, 10f, muted)
        text(student.admissionNo, 350f, 326f, 13f, ink, true)
        text("Class", 42f, 365f, 10f, muted)
        text(student.displayClass, 42f, 386f, 12f, ink, true)
        text("Roll No.", 350f, 365f, 10f, muted)
        text(student.rollNo.toString(), 350f, 386f, 12f, ink, true)
        line(415f)

        text("PAYMENT DETAILS", 42f, 452f, 12f, navy, true)
        text("Academic Year", 42f, 486f, 10f, muted)
        text(payment.academicYear.toString(), 200f, 486f, 10f, muted)
        text("Fee Allocation", 42f, 520f, 10f, muted)

        val monthsFormatted = formatPaidMonthTokens(payment.paidMonths, payment.academicYear)
        text(monthsFormatted.ifBlank { "-" }, 200f, 520f, 10f, ink, true)
        line(540f)

        text("Base Fee Received", 42f, 570f, 12f, ink)
        text("Rs. ${"%.2f".format(Locale.US, payment.baseAmount)}", 440f, 570f, 12f, ink, true)

        text("Late Fine Charged", 42f, 600f, 12f, ink)
        text("Rs. ${"%.2f".format(Locale.US, payment.lateFeeAmount)}", 440f, 600f, 12f, ink, true)

        if (payment.waivedFineAmount > 0.0) {
            text("Late Fine Concession (Waived)", 42f, 630f, 11f, android.graphics.Color.rgb(220, 38, 38))
            text("- Rs. ${"%.2f".format(Locale.US, payment.waivedFineAmount)}", 440f, 630f, 11f, android.graphics.Color.rgb(220, 38, 38), true)
            line(650f)
        } else {
            line(625f)
        }

        paint.color = 0xFFEFF6FF.toInt()
        val cardTop = if (payment.waivedFineAmount > 0.0) 665f else 640f
        canvas.drawRoundRect(35f, cardTop, 560f, cardTop + 70f, 12f, 12f, paint)
        text("TOTAL AMOUNT PAID", 55f, cardTop + 30f, 12f, navy, true)
        text("Rs. ${"%.2f".format(Locale.US, payment.totalAmount)}", 365f, cardTop + 42f, 20f, green, true)

        text("Payment Status: PAID", 42f, cardTop + 90f, 12f, green, true)
        text("This is an official computer-generated receipt. Please keep it safely.", 42f, cardTop + 115f, 9f, muted)
        text("Thank you for your payment.", 42f, cardTop + 135f, 10f, navy, true)

        document.finishPage(page)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    fun thermalText(student: Student, payment: FeePayment): String {
        val months = formatPaidMonthTokens(payment.paidMonths, payment.academicYear)
        val date = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(payment.paymentDate))
        val waiverLine = if (payment.waivedFineAmount > 0.0) "Fine Concession: -Rs. ${"%.2f".format(Locale.US, payment.waivedFineAmount)}\n" else ""

        return """VIVEKANANDA VIDYAMANDIR
Dhupguri, Jalpaiguri
FEE PAYMENT RECEIPT
--------------------------------
Receipt No: ${payment.receiptNo}
Date: $date
Student: ${student.fullName}
Admission No: ${student.admissionNo}
Class: ${student.displayClass} | Roll: ${student.rollNo}
Academic Year: ${payment.academicYear}
Paid For: $months
--------------------------------
Base Fee Paid: Rs. ${"%.2f".format(Locale.US, payment.baseAmount)}
Late Fine Paid: Rs. ${"%.2f".format(Locale.US, payment.lateFeeAmount)}
$waiverLine--------------------------------
TOTAL PAID: Rs. ${"%.2f".format(Locale.US, payment.totalAmount)}
--------------------------------
Payment Status: PAID
Thank you for your payment.
""".trimIndent()
    }

    private fun formatPaidMonthTokens(raw: String, academicYear: Int): String {
        return raw.split(",")
            .mapNotNull { token ->
                val clean = token.trim()
                if (clean.isBlank()) return@mapNotNull null
                val parts = clean.split(":")
                val key = parts[0].trim()
                val amt = parts.getOrNull(1)?.trim()

                val monthName = if (key.matches(Regex("\\d{4}-\\d{2}"))) {
                    val p = key.split("-")
                    val m = p[1].toIntOrNull() ?: 1
                    DateFormatSymbols().months.getOrNull(m - 1) ?: key
                } else {
                    val m = key.toIntOrNull() ?: 1
                    DateFormatSymbols().months.getOrNull(m - 1) ?: key
                }

                if (amt != null) "$monthName (₹$amt)" else monthName
            }.joinToString(", ")
    }

    fun share(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share payment receipt PDF"))
    }

    fun view(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Open receipt PDF"))
    }

    fun print(context: Context, file: File) {
        val manager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        manager.print("Fee receipt - ${file.nameWithoutExtension}", object : PrintDocumentAdapter() {
            override fun onLayout(oldAttributes: PrintAttributes?, newAttributes: PrintAttributes?, cancellationSignal: CancellationSignal?, callback: LayoutResultCallback, extras: Bundle?) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                callback.onLayoutFinished(
                    PrintDocumentInfo.Builder(file.name).setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(1).build(), true
                )
            }

            override fun onWrite(pages: Array<out PageRange>, destination: ParcelFileDescriptor, cancellationSignal: CancellationSignal, callback: WriteResultCallback) {
                try {
                    FileInputStream(file).use { input -> FileOutputStream(destination.fileDescriptor).use { output ->
                        val buffer = ByteArray(8192)
                        while (true) {
                            if (cancellationSignal.isCanceled) {
                                callback.onWriteCancelled()
                                return
                            }
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                        }
                        output.flush()
                    } }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                }
            }
        }, null)
    }
}
