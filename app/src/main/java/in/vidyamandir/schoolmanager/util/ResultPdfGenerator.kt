package in.vidyamandir.schoolmanager.util

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import in.vidyamandir.schoolmanager.data.ExamResult
import in.vidyamandir.schoolmanager.data.Student
import java.io.File
import java.io.FileOutputStream

class ResultPdfGenerator(private val context: Context) {

    private val a4Width = 595
    private val a4Height = 842

    fun generateReportCard(
        student: Student,
        results: List<ExamResult>,
        outputFile: File
    ) {
        val document = PdfDocument()

        // Page 1: Cover Page
        val pageInfo1 = PdfDocument.PageInfo.Builder(a4Width, a4Height, 1).create()
        val page1 = document.startPage(pageInfo1)
        drawPageOne(page1.canvas, student)
        document.finishPage(page1)

        // Page 2: Result Performance
        val pageInfo2 = PdfDocument.PageInfo.Builder(a4Width, a4Height, 2).create()
        val page2 = document.startPage(pageInfo2)
        drawPageTwo(page2.canvas, student, results)
        document.finishPage(page2)

        FileOutputStream(outputFile).use { out ->
            document.writeTo(out)
        }
        document.close()
    }

    private fun drawPageOne(canvas: Canvas, student: Student) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = Color.parseColor("#0F3D6E")
        canvas.drawRect(24f, 24f, (a4Width - 24).toFloat(), (a4Height - 24).toFloat(), paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#0F3D6E")
        paint.textSize = 24f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("VIVEKANANDA VIDYAMANDIR", (a4Width / 2).toFloat(), 120f, paint)

        paint.textSize = 12f
        paint.isFakeBoldText = false
        paint.color = Color.DKGRAY
        canvas.drawText("Dhupguri, Jalpaiguri, West Bengal - 735210", (a4Width / 2).toFloat(), 145f, paint)

        paint.textSize = 14f
        paint.isFakeBoldText = true
        paint.color = Color.parseColor("#D97706")
        canvas.drawText("ANNUAL PROGRESS REPORT", (a4Width / 2).toFloat(), 190f, paint)

        val quoteTop = 380f
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#F8FAFC")
        canvas.drawRoundRect(50f, quoteTop, (a4Width - 50).toFloat(), quoteTop + 90f, 8f, 8f, paint)

        paint.color = Color.parseColor("#0F3D6E")
        paint.textSize = 12f
        paint.isFakeBoldText = true
        canvas.drawText(
            "\"Education is the manifestation of the perfection already in man.\"",
            (a4Width / 2).toFloat(),
            quoteTop + 40f,
            paint
        )
        paint.textSize = 11f
        paint.isFakeBoldText = false
        paint.color = Color.DKGRAY
        canvas.drawText("— Swami Vivekananda", (a4Width / 2).toFloat(), quoteTop + 65f, paint)

        paint.textSize = 13f
        paint.color = Color.BLACK
        canvas.drawText("Student Name: ${student.fullName}", (a4Width / 2).toFloat(), quoteTop + 140f, paint)
        canvas.drawText("Class: ${student.className}  |  Roll No: ${student.rollNo}", (a4Width / 2).toFloat(), quoteTop + 165f, paint)
    }

    private fun drawPageTwo(canvas: Canvas, student: Student, results: List<ExamResult>) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = Color.parseColor("#0F3D6E")
        paint.textSize = 18f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("ACADEMIC PERFORMANCE STATEMENT", (a4Width / 2).toFloat(), 60f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = Color.LTGRAY
        canvas.drawRect(40f, 80f, (a4Width - 40).toFloat(), 125f, paint)

        paint.style = Paint.Style.FILL
        paint.textSize = 11f
        paint.textAlign = Paint.Align.LEFT
        paint.color = Color.BLACK
        canvas.drawText("Student: ${student.fullName}", 50f, 105f, paint)
        canvas.drawText("Class: ${student.className}", 300f, 105f, paint)
        canvas.drawText("Roll: ${student.rollNo}", 460f, 105f, paint)

        var y = 160f
        paint.color = Color.parseColor("#0F3D6E")
        canvas.drawRect(40f, y - 20f, (a4Width - 40).toFloat(), y + 8f, paint)

        paint.color = Color.WHITE
        paint.isFakeBoldText = true
        paint.textSize = 10f
        canvas.drawText("Subject", 50f, y - 4f, paint)
        canvas.drawText("Full", 230f, y - 4f, paint)
        canvas.drawText("Written", 290f, y - 4f, paint)
        canvas.drawText("Practical", 360f, y - 4f, paint)
        canvas.drawText("Obtained", 440f, y - 4f, paint)
        canvas.drawText("Grade", 510f, y - 4f, paint)

        paint.color = Color.BLACK
        paint.isFakeBoldText = false
        var totalFull = 0
        var totalObtained = 0
        var isAllPassed = true

        y += 25f
        for (res in results) {
            totalFull += res.fullMarks
            totalObtained += res.obtainedMarks
            if (res.obtainedMarks < (res.fullMarks * 0.30)) isAllPassed = false

            paint.color = Color.BLACK
            canvas.drawText(res.subjectName, 50f, y, paint)
            canvas.drawText(res.fullMarks.toString(), 230f, y, paint)
            canvas.drawText(res.writtenMarks.toString(), 290f, y, paint)
            canvas.drawText(res.practicalMarks.toString(), 360f, y, paint)
            canvas.drawText(res.obtainedMarks.toString(), 440f, y, paint)
            canvas.drawText(res.grade, 510f, y, paint)

            paint.color = Color.parseColor("#E2E8F0")
            canvas.drawLine(40f, y + 6f, (a4Width - 40).toFloat(), y + 6f, paint)
            y += 24f
        }

        y += 20f
        val pct = if (totalFull > 0) (totalObtained.toDouble() / totalFull * 100) else 0.0
        val finalStatus = if (isAllPassed) "PASSED" else "FAILED"
        val overallGrade = when {
            pct >= 90 -> "AA"
            pct >= 80 -> "A+"
            pct >= 70 -> "A"
            pct >= 60 -> "B+"
            pct >= 50 -> "B"
            pct >= 40 -> "C"
            pct >= 30 -> "D"
            else -> "F"
        }

        paint.color = Color.parseColor("#F1F5F9")
        canvas.drawRoundRect(40f, y, (a4Width - 40).toFloat(), y + 55f, 6f, 6f, paint)

        paint.color = Color.parseColor("#0F3D6E")
        paint.textSize = 11f
        paint.isFakeBoldText = true
        canvas.drawText("Total: $totalObtained / $totalFull", 55f, y + 25f, paint)
        canvas.drawText(String.format("Percentage: %.2f%%", pct), 55f, y + 43f, paint)
        canvas.drawText("Grade: $overallGrade", 280f, y + 33f, paint)

        paint.color = if (isAllPassed) Color.parseColor("#16A34A") else Color.parseColor("#DC2626")
        paint.textSize = 13f
        canvas.drawText("Status: $finalStatus", 420f, y + 33f, paint)

        val signY = a4Height - 90f
        paint.color = Color.GRAY
        canvas.drawLine(50f, signY, 180f, signY, paint)
        canvas.drawLine((a4Width - 190).toFloat(), signY, (a4Width - 50).toFloat(), signY, paint)

        paint.color = Color.BLACK
        paint.textSize = 10f
        paint.isFakeBoldText = false
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Class Teacher Signature", 115f, signY + 18f, paint)
        canvas.drawText("Headmaster / Seal", (a4Width - 120).toFloat(), signY + 18f, paint)
    }
}
