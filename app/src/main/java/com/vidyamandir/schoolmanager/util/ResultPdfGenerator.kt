package com.vidyamandir.schoolmanager.util

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.vidyamandir.schoolmanager.data.ExamResult
import com.vidyamandir.schoolmanager.data.ExamResultComponent
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
        outputFile: File,
        componentsByResult: Map<Long, List<ExamResultComponent>> = emptyMap()
    ) {
        val document = PdfDocument()

        val pageInfo1 = PdfDocument.PageInfo.Builder(a4Width, a4Height, 1).create()
        val page1 = document.startPage(pageInfo1)
        drawPageOne(page1.canvas, student, examName)
        document.finishPage(page1)

        val pageInfo2 = PdfDocument.PageInfo.Builder(a4Width, a4Height, 2).create()
        val page2 = document.startPage(pageInfo2)
        drawPageTwo(page2.canvas, student, results, componentsByResult)
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
        outputFile: File,
        componentsByStudent: Map<Long, Map<Long, List<ExamResultComponent>>> = emptyMap()
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
                drawPageTwo(
                    page2.canvas, student, results,
                    componentsByStudent[student.id].orEmpty()
                )
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
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val navy = Color.parseColor("#123B65")
        val gold = Color.parseColor("#D69A28")
        val pale = Color.parseColor("#F3F7FC")
        val center = a4Width / 2f

        val profile = context.getSharedPreferences(
            "vv_school_profile", Context.MODE_PRIVATE
        )
        val schoolName = profile.getString(
            "school_name", "Vivekananda Vidyamandir"
        ) ?: "Vivekananda Vidyamandir"
        val address = profile.getString("address", "") ?: ""
        val contact = profile.getString("contact_number", "") ?: ""
        val email = profile.getString("email", "") ?: ""

        // Main page background and border.
        canvas.drawColor(Color.WHITE)
        p.color = navy
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2.5f
        canvas.drawRect(22f, 22f, 573f, 820f, p)

        // TOP PANEL: school and student details.
        p.style = Paint.Style.FILL
        p.color = navy
        p.textAlign = Paint.Align.CENTER
        p.textSize = 23f
        p.isFakeBoldText = true
        canvas.drawText(schoolName.uppercase().take(42), center, 55f, p)

        p.textSize = 10f
        p.isFakeBoldText = false
        p.color = Color.DKGRAY
        if (address.isNotBlank()) {
            canvas.drawText(address.take(90), center, 74f, p)
        }
        val contactLine = listOf(
            if (contact.isNotBlank()) "Phone: $contact" else "",
            if (email.isNotBlank()) "Email: $email" else ""
        ).filter { it.isNotBlank() }.joinToString("   |   ")
        if (contactLine.isNotBlank()) {
            canvas.drawText(contactLine.take(100), center, 91f, p)
        }

        p.color = gold
        p.textSize = 15f
        p.isFakeBoldText = true
        canvas.drawText(examName.uppercase().take(45), center, 116f, p)

        // Load the supplied Swami Vivekananda photograph.
        val imageId = context.resources.getIdentifier(
            "swami_vivekananda", "drawable", context.packageName
        )
        val portrait = if (imageId != 0) {
            BitmapFactory.decodeResource(context.resources, imageId)
        } else null

        if (portrait != null) {
            drawCircularPortrait(canvas, portrait, center, 190f, 57f, gold)
        } else {
            p.color = pale
            canvas.drawCircle(center, 190f, 57f, p)
            p.color = navy
            p.textSize = 9f
            canvas.drawText("SWAMI VIVEKANANDA", center, 193f, p)
        }

        p.color = navy
        p.textSize = 10f
        p.isFakeBoldText = true
        canvas.drawText("STUDENT INFORMATION", center, 270f, p)

        p.color = pale
        canvas.drawRoundRect(42f, 282f, 553f, 397f, 10f, 10f, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1f
        p.color = Color.parseColor("#C8D6E5")
        canvas.drawRoundRect(42f, 282f, 553f, 397f, 10f, 10f, p)

        p.style = Paint.Style.FILL
        p.textAlign = Paint.Align.LEFT
        p.color = navy
        p.textSize = 12f
        p.isFakeBoldText = true
        canvas.drawText("Student Name", 58f, 310f, p)
        canvas.drawText("Admission No.", 58f, 342f, p)
        canvas.drawText("Class", 315f, 342f, p)
        canvas.drawText("Roll Number", 315f, 374f, p)

        p.color = Color.BLACK
        p.textSize = 11f
        p.isFakeBoldText = false
        canvas.drawText(student.fullName.take(34), 58f, 327f, p)
        canvas.drawText(student.admissionNo.take(22), 58f, 359f, p)
        canvas.drawText(student.displayClass.take(22), 315f, 359f, p)
        canvas.drawText(student.rollNo.toString(), 315f, 389f, p)

        // Fold guide between the two panels.
        p.color = gold
        p.strokeWidth = 1.5f
        p.pathEffect = DashPathEffect(floatArrayOf(7f, 5f), 0f)
        canvas.drawLine(30f, 421f, 565f, 421f, p)
        p.pathEffect = null
        p.textAlign = Paint.Align.CENTER
        p.textSize = 8f
        p.isFakeBoldText = true
        canvas.drawText("FOLD HERE", center, 416f, p)

        // BOTTOM PANEL: rotate 180 degrees from its previous orientation.
        // Total rotation is 270 degrees so the landscape panel still fits.
        canvas.save()
        canvas.translate(center, 619.5f)
        canvas.rotate(270f)
        canvas.translate(-198.5f, -297.5f)
        drawGradingGuide(canvas, navy, gold, pale)
        canvas.restore()

        portrait?.recycle()
    }

    private fun drawCircularPortrait(
        canvas: Canvas,
        bitmap: Bitmap,
        cx: Float,
        cy: Float,
        radius: Float,
        ringColor: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val path = Path().apply { addCircle(cx, cy, radius, Path.Direction.CW) }
        val save = canvas.save()
        canvas.clipPath(path)

        val side = minOf(bitmap.width, bitmap.height)
        val left = (bitmap.width - side) / 2
        val top = (bitmap.height - side) / 2
        val src = Rect(left, top, left + side, top + side)
        val dst = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        canvas.drawBitmap(bitmap, src, dst, paint)
        canvas.restoreToCount(save)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = ringColor
        canvas.drawCircle(cx, cy, radius + 2f, paint)
    }

    private fun drawGradingGuide(
        canvas: Canvas,
        navy: Int,
        gold: Int,
        pale: Int
    ) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val cx = 198.5f

        p.color = Color.WHITE
        canvas.drawRect(0f, 0f, 397f, 595f, p)

        p.color = navy
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f
        canvas.drawRect(8f, 8f, 389f, 587f, p)

        p.style = Paint.Style.FILL
        p.textAlign = Paint.Align.CENTER
        p.textSize = 18f
        p.isFakeBoldText = true
        p.color = navy
        canvas.drawText("GRADING & RESULT GUIDE", cx, 34f, p)

        p.textSize = 11f
        p.color = gold
        canvas.drawText("VIVEKANANDA VIDYAMANDIR", cx, 145f, p)

        p.textSize = 12f
        p.color = navy
        p.isFakeBoldText = true
        canvas.drawText("OVERALL GRADE SCALE", cx, 177f, p)

        val rows = listOf(
            "90% and above" to "AA",
            "80% - 89.99%" to "A+",
            "70% - 79.99%" to "A",
            "60% - 69.99%" to "B+",
            "50% - 59.99%" to "B",
            "35% - 49.99%" to "C",
            "25% - 34.99%" to "D",
            "Below 25%" to "F"
        )

        var y = 202f
        p.textAlign = Paint.Align.LEFT
        p.textSize = 10f
        p.isFakeBoldText = false

        for ((range, grade) in rows) {
            p.color = pale
            canvas.drawRoundRect(35f, y - 13f, 362f, y + 5f, 4f, 4f, p)
            p.color = navy
            canvas.drawText(range, 48f, y, p)
            p.isFakeBoldText = true
            canvas.drawText(grade, 325f, y, p)
            p.isFakeBoldText = false
            y += 22f
        }

        p.textAlign = Paint.Align.CENTER
        p.color = navy
        p.textSize = 12f
        p.isFakeBoldText = true
        canvas.drawText("HOW PERCENTAGE IS CALCULATED", cx, 398f, p)

        p.textSize = 10f
        p.isFakeBoldText = false
        canvas.drawText(
            "(Total Marks Obtained / Total Maximum Marks) x 100",
            cx, 420f, p
        )

        p.textSize = 12f
        p.isFakeBoldText = true
        canvas.drawText("PASSING RULE", cx, 453f, p)

        p.textSize = 10f
        p.isFakeBoldText = false
        canvas.drawText(
            "A student must score at least 35% in every subject.",
            cx, 474f, p
        )
        canvas.drawText(
            "Overall result is FAILED if any subject is below 35%.",
            cx, 491f, p
        )

        p.textSize = 11f
        p.isFakeBoldText = true
        p.color = gold
        canvas.drawText("IMPORTANT GUIDELINES", cx, 522f, p)

        p.textSize = 9f
        p.isFakeBoldText = false
        p.color = Color.DKGRAY
        canvas.drawText("Please check all details and marks carefully.", cx, 541f, p)
        canvas.drawText("Contact the school for any correction or query.", cx, 556f, p)

    }

    private fun drawPageTwo(
        canvas: Canvas,
        student: Student,
        results: List<ExamResult>,
        componentsByResult: Map<Long, List<ExamResultComponent>> = emptyMap()
    ) {
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
            val components = componentsByResult[res.id].orEmpty()
            val customObtained = components.sumOf { it.obtainedMarks }
            val totalObtained = res.obtainedMarks + customObtained

            grandFull += res.fullMarks
            grandObtained += totalObtained
            if (totalObtained < (res.fullMarks * 0.35)) isPassed = false

            canvas.drawText(res.subjectName, 50f, y, paint)
            canvas.drawText(res.fullMarks.toString(), 220f, y, paint)
            canvas.drawText(res.writtenMarks.toString(), 270f, y, paint)
            canvas.drawText(res.oralMarks.toString(), 330f, y, paint)
            canvas.drawText(res.practicalMarks.toString(), 380f, y, paint)
            canvas.drawText(totalObtained.toString(), 450f, y, paint)
            canvas.drawText(res.grade, 520f, y, paint)

            var rowHeight = 24f

            if (components.isNotEmpty()) {
                val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.DKGRAY
                    textSize = 7f
                }

                val details = components
                    .sortedBy { it.position }
                    .joinToString("  |  ") {
                        "${it.componentName}: ${it.obtainedMarks}/${it.maxMarks}"
                    }

                canvas.drawText("Components: $details", 50f, y + 12f, detailPaint)
                rowHeight = 38f
            }

            paint.color = Color.parseColor("#E2E8F0")
            canvas.drawLine(
                40f, y + rowHeight - 5f,
                (a4Width - 40).toFloat(), y + rowHeight - 5f, paint
            )
            paint.color = Color.BLACK
            y += rowHeight
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
