package com.vidyamandir.schoolmanager.util

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.vidyamandir.schoolmanager.data.ExamResult
import com.vidyamandir.schoolmanager.data.Student
import java.io.File
import java.io.FileOutputStream

class ResultPdfGenerator(private val context: Context) {

    private val a4Width = 595
    private val a4Height = 842

    fun generateReportCard(
        student: Student,
        examName: String,
        results: List<ExamResult>,
        outputFile: File
    ) {
        val document = PdfDocument()

        val pageInfo1 = PdfDocument.PageInfo.Builder(a4Width, a4Height, 1).create()
        val page1 = document.startPage(pageInfo1)
        drawPageOne(page1.canvas, student, examName)
        document.finishPage(page1)

        val pageInfo2 = PdfDocument.PageInfo.Builder(a4Width, a4Height, 2).create()
        val page2 = document.startPage(pageInfo2)
        drawPageTwo(page2.canvas, student, results)
        document.finishPage(page2)

        FileOutputStream(outputFile).use { out ->
            document.writeTo(out)
        }
        document.close()
    }


    fun generateClassReportCards(
        students: List<Student>,
        examName: String,
        resultsByStudent: Map<Long, List<ExamResult>>,
        outputFile: File
    ) {
        val document = PdfDocument()
        var pageNumber = 1

        try {
            for (student in students) {
                val results = resultsByStudent[student.id].orEmpty()
                if (results.isEmpty()) continue

                val pageInfo1 = PdfDocument.PageInfo.Builder(
                    a4Width, a4Height, pageNumber++
                ).create()
                val page1 = document.startPage(pageInfo1)
                drawPageOne(page1.canvas, student, examName)
                document.finishPage(page1)

                val pageInfo2 = PdfDocument.PageInfo.Builder(
                    a4Width, a4Height, pageNumber++
                ).create()
                val page2 = document.startPage(pageInfo2)
                drawPageTwo(page2.canvas, student, results)
                document.finishPage(page2)
            }

            require(pageNumber > 1) {
                "No approved marksheets are available for this class and exam"
            }

            outputFile.parentFile?.mkdirs()
            FileOutputStream(outputFile).use { output ->
                document.writeTo(output)
            }
        } finally {
            document.close()
        }
    }

    private fun drawPageOne(canvas: Canvas, student: Student, examName: String) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = Color.parseColor("#0F3D6E")
        canvas.drawRect(24f, 24f, (a4Width - 24).toFloat(), (a4Height - 24).toFloat(), paint)

        paint.strokeWidth = 1f
        canvas.drawRect(28f, 28f, (a4Width - 28).toFloat(), (a4Height - 28).toFloat(), paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#0F3D6E")
        val profile = context.getSharedPreferences(
            "vv_school_profile", Context.MODE_PRIVATE
        )
        val schoolName = profile.getString(
            "school_name", "Vivekananda Vidyamandir"
        ) ?: "Vivekananda Vidyamandir"
        val schoolAddress = profile.getString(
            "address", "Dhupguri, Jalpaiguri, West Bengal - 735210"
        ) ?: ""
        val schoolContact = profile.getString("contact_number", "") ?: ""

        paint.textSize = 24f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(
            schoolName.uppercase().take(42),
            (a4Width / 2).toFloat(), 95f, paint
        )

        paint.textSize = 12f
        paint.isFakeBoldText = false
        paint.color = Color.DKGRAY
        canvas.drawText(
            schoolAddress.take(75),
            (a4Width / 2).toFloat(), 120f, paint
        )
        if (schoolContact.isNotBlank()) {
            canvas.drawText(
                "Contact: $schoolContact",
                (a4Width / 2).toFloat(), 137f, paint
            )
        }

        paint.textSize = 14f
        paint.isFakeBoldText = true
        paint.color = Color.parseColor("#D97706")
        canvas.drawText(examName.uppercase(), (a4Width / 2).toFloat(), 160f, paint)

        val centerX = (a4Width / 2).toFloat()
        val centerY = 310f

        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#FEF3C7")
        canvas.drawCircle(centerX, centerY, 100f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = Color.parseColor("#D97706")
        canvas.drawCircle(centerX, centerY, 102f, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#0F3D6E")
        paint.textSize = 13f
        paint.isFakeBoldText = true
        canvas.drawText("SWAMI VIVEKANANDA", centerX, centerY - 15f, paint)
        paint.textSize = 10f
        paint.color = Color.parseColor("#B45309")
        canvas.drawText("★ MOTTO OF EDUCATION ★", centerX, centerY + 10f, paint)

        val quoteTop = 460f
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#F8FAFC")
        canvas.drawRoundRect(45f, quoteTop, (a4Width - 45).toFloat(), quoteTop + 95f, 10f, 10f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = Color.parseColor("#E2E8F0")
        canvas.drawRoundRect(45f, quoteTop, (a4Width - 45).toFloat(), quoteTop + 95f, 10f, 10f, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#0F3D6E")
        paint.textSize = 13f
        paint.isFakeBoldText = true
        canvas.drawText(
            "\"Education is the manifestation of the perfection already in man.\"",
            centerX,
            quoteTop + 40f,
            paint
        )
        paint.textSize = 11f
        paint.isFakeBoldText = false
        paint.color = Color.DKGRAY
        canvas.drawText("— Swami Vivekananda", centerX, quoteTop + 68f, paint)

        val profileTop = 590f
        paint.color = Color.parseColor("#FFFFFF")
        canvas.drawRoundRect(45f, profileTop, (a4Width - 45).toFloat(), profileTop + 140f, 10f, 10f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.parseColor("#CBD5E1")
        canvas.drawRoundRect(45f, profileTop, (a4Width - 45).toFloat(), profileTop + 140f, 10f, 10f, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#0F3D6E")
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("STUDENT PROFILE", centerX, profileTop + 30f, paint)

        paint.textSize = 12f
        paint.color = Color.BLACK
        paint.isFakeBoldText = false
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Student Name: ${student.fullName}", 70f, profileTop + 65f, paint)
        canvas.drawText("Admission ID: ${student.admissionNo}", 70f, profileTop + 95f, paint)
        canvas.drawText("Class: ${student.displayClass}", 320f, profileTop + 65f, paint)
        canvas.drawText("Roll Number: ${student.rollNo}", 320f, profileTop + 95f, paint)
    }

    private fun drawPageTwo(canvas: Canvas, student: Student, results: List<ExamResult>) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = Color.parseColor("#0F3D6E")
        paint.textSize = 18f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("ACADEMIC PROGRESS STATEMENT", (a4Width / 2).toFloat(), 55f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = Color.LTGRAY
        canvas.drawRect(40f, 75f, (a4Width - 40).toFloat(), 115f, paint)

        paint.style = Paint.Style.FILL
        paint.textSize = 10f
        paint.textAlign = Paint.Align.LEFT
        paint.color = Color.BLACK
        paint.isFakeBoldText = false
        canvas.drawText("Student: ${student.fullName}", 50f, 98f, paint)
        canvas.drawText("Class: ${student.displayClass}", 260f, 98f, paint)
        canvas.drawText("Roll: ${student.rollNo}", 440f, 98f, paint)

        var y = 145f
        paint.color = Color.parseColor("#0F3D6E")
        canvas.drawRect(40f, y - 18f, (a4Width - 40).toFloat(), y + 8f, paint)

        paint.color = Color.WHITE
        paint.isFakeBoldText = true
        paint.textSize = 10f
        canvas.drawText("Subject Name", 50f, y - 4f, paint)
        canvas.drawText("Full", 220f, y - 4f, paint)
        canvas.drawText("Written", 270f, y - 4f, paint)
        canvas.drawText("Oral", 330f, y - 4f, paint)
        canvas.drawText("Practical", 380f, y - 4f, paint)
        canvas.drawText("Obtained", 450f, y - 4f, paint)
        canvas.drawText("Grade", 520f, y - 4f, paint)

        paint.color = Color.BLACK
        paint.isFakeBoldText = false
        var grandFull = 0
        var grandObtained = 0
        var isPassed = true

        y += 24f
        for (res in results) {
            grandFull += res.fullMarks
            grandObtained += res.obtainedMarks
            if (res.obtainedMarks < (res.fullMarks * 0.35)) isPassed = false

            canvas.drawText(res.subjectName, 50f, y, paint)
            canvas.drawText(res.fullMarks.toString(), 220f, y, paint)
            canvas.drawText(res.writtenMarks.toString(), 270f, y, paint)
            canvas.drawText(res.oralMarks.toString(), 330f, y, paint)
            canvas.drawText(res.practicalMarks.toString(), 380f, y, paint)
            canvas.drawText(res.obtainedMarks.toString(), 450f, y, paint)
            canvas.drawText(res.grade, 520f, y, paint)

            paint.color = Color.parseColor("#E2E8F0")
            canvas.drawLine(40f, y + 5f, (a4Width - 40).toFloat(), y + 5f, paint)
            paint.color = Color.BLACK
            y += 24f
        }

        y += 20f
        val percentage = if (grandFull > 0) (grandObtained.toDouble() / grandFull * 100) else 0.0
        val finalStatus = if (isPassed) "PASSED" else "FAILED"
        val overallGrade = when {
            percentage >= 90 -> "AA"
            percentage >= 80 -> "A+"
            percentage >= 70 -> "A"
            percentage >= 60 -> "B+"
            percentage >= 50 -> "B"
            percentage >= 35 -> "C"
            percentage >= 25 -> "D"
            else -> "F"
        }

        paint.color = Color.parseColor("#F1F5F9")
        canvas.drawRoundRect(40f, y, (a4Width - 40).toFloat(), y + 60f, 6f, 6f, paint)

        paint.color = Color.parseColor("#0F3D6E")
        paint.textSize = 11f
        paint.isFakeBoldText = true
        canvas.drawText("Grand Total: $grandObtained / $grandFull", 55f, y + 26f, paint)
        canvas.drawText(String.format("Percentage: %.2f%%", percentage), 55f, y + 46f, paint)
        canvas.drawText("Overall Grade: $overallGrade", 270f, y + 36f, paint)

        paint.color = if (isPassed) Color.parseColor("#16A34A") else Color.parseColor("#DC2626")
        paint.textSize = 14f
        canvas.drawText("Result: $finalStatus", 420f, y + 36f, paint)

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
