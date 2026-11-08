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

        // Main page background and border
        canvas.drawColor(Color.WHITE)
        p.color = navy
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2.5f
        canvas.drawRect(22f, 22f, (a4Width - 22).toFloat(), (a4Height - 22).toFloat(), p)

        // School and student details header
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

        // Swami Vivekananda Portrait
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
        canvas.drawText("STUDENT INFORMATION", center, 268f, p)

        p.color = pale
        canvas.drawRoundRect(42f, 280f, (a4Width - 42).toFloat(), 380f, 10f, 10f, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1f
        p.color = Color.parseColor("#C8D6E5")
        canvas.drawRoundRect(42f, 280f, (a4Width - 42).toFloat(), 380f, 10f, 10f, p)

        // Labels
        p.style = Paint.Style.FILL
        p.textAlign = Paint.Align.LEFT
        p.color = navy
        p.textSize = 11f
        p.isFakeBoldText = true
        canvas.drawText("Student Name", 58f, 305f, p)
        canvas.drawText("Admission No.", 58f, 342f, p)
        canvas.drawText("Class", 315f, 342f, p)
        canvas.drawText("Roll Number", 435f, 342f, p)

        // Values (Class and Roll Number on the same line)
        p.color = Color.BLACK
        p.textSize = 11f
        p.isFakeBoldText = false
        canvas.drawText(student.fullName.take(34), 58f, 322f, p)
        canvas.drawText(student.admissionNo.take(22), 58f, 360f, p)
        canvas.drawText(student.displayClass.take(15), 315f, 360f, p)
        canvas.drawText(student.rollNo.toString(), 435f, 360f, p)

        // Fold guide line
        p.color = gold
        p.strokeWidth = 1.5f
        p.pathEffect = DashPathEffect(floatArrayOf(7f, 5f), 0f)
        canvas.drawLine(30f, 400f, (a4Width - 30).toFloat(), 400f, p)
        p.pathEffect = null
        p.textAlign = Paint.Align.CENTER
        p.textSize = 8f
        p.isFakeBoldText = true
        canvas.drawText("FOLD HERE", center, 395f, p)

        // Rotate 180 degrees: inverted horizontal fold panel
        // Centered inside lower half (Y = 405 to 815 -> center Y = 608f)
        canvas.save()
        canvas.translate(center, 608f)
        canvas.rotate(180f)
        canvas.translate(-255f, -185f)
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
        val boxWidth = 510f
        val boxHeight = 370f
        val cx = boxWidth / 2f

        p.color = Color.WHITE
        canvas.drawRect(0f, 0f, boxWidth, boxHeight, p)

        // Outer box border
        p.color = navy
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.8f
        canvas.drawRect(6f, 6f, boxWidth - 6f, boxHeight - 6f, p)

        // Inner frame
        p.color = Color.parseColor("#CBD5E1")
        p.strokeWidth = 0.8f
        canvas.drawRect(10f, 10f, boxWidth - 10f, boxHeight - 10f, p)

        // Full-width Header
        p.style = Paint.Style.FILL
        p.textAlign = Paint.Align.CENTER
        p.textSize = 14f
        p.isFakeBoldText = true
        p.color = navy
        canvas.drawText("GRADING & RESULT GUIDE", cx, 26f, p)

        p.textSize = 10f
        p.color = gold
        canvas.drawText("VIVEKANANDA VIDYAMANDIR", cx, 44f, p)

        // Top divider
        p.style = Paint.Style.STROKE
        p.strokeWidth = 0.8f
        p.color = Color.parseColor("#E2E8F0")
        canvas.drawLine(30f, 56f, boxWidth - 30f, 56f, p)
        p.style = Paint.Style.FILL

        // LEFT COLUMN: Overall Grade Scale Table (X: 25 to 245)
        p.textAlign = Paint.Align.CENTER
        p.textSize = 10.5f
        p.color = navy
        p.isFakeBoldText = true
        canvas.drawText("OVERALL GRADE SCALE", 135f, 75f, p)

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

        var gy = 96f
        p.textAlign = Paint.Align.LEFT
        p.textSize = 9f
        p.isFakeBoldText = false

        for ((range, grade) in rows) {
            p.color = pale
            canvas.drawRoundRect(25f, gy - 9f, 245f, gy + 7f, 4f, 4f, p)
            p.color = navy
            canvas.drawText(range, 35f, gy, p)
            p.isFakeBoldText = true
            canvas.drawText(grade, 222f, gy, p)
            p.isFakeBoldText = false
            gy += 18f
        }

        // Vertical Divider between columns
        p.style = Paint.Style.STROKE
        p.strokeWidth = 0.8f
        p.color = Color.parseColor("#E2E8F0")
        canvas.drawLine(258f, 68f, 258f, 345f, p)
        p.style = Paint.Style.FILL

        // RIGHT COLUMN: Rules & Guidelines (Centered around rcx = 384f)
        val rcx = 384f

        // 1. Percentage
        p.textAlign = Paint.Align.CENTER
        p.color = navy
        p.textSize = 10f
        p.isFakeBoldText = true
        canvas.drawText("HOW PERCENTAGE IS CALCULATED", rcx, 78f, p)

        p.textSize = 8.5f
        p.isFakeBoldText = false
        p.color = Color.parseColor("#334155")
        canvas.drawText("(Total Marks Obtained /", rcx, 96f, p)
        canvas.drawText("Total Maximum Marks) x 100", rcx, 110f, p)

        p.style = Paint.Style.STROKE
        p.strokeWidth = 0.8f
        p.color = Color.parseColor("#E2E8F0")
        canvas.drawLine(280f, 126f, 488f, 126f, p)
        p.style = Paint.Style.FILL

        // 2. Passing Rule
        p.color = navy
        p.textSize = 10f
        p.isFakeBoldText = true
        canvas.drawText("PASSING RULE", rcx, 144f, p)

        p.textSize = 8.5f
        p.isFakeBoldText = false
        p.color = Color.parseColor("#334155")
        canvas.drawText("A student must score at least", rcx, 162f, p)
        canvas.drawText("35% in every subject.", rcx, 176f, p)

        p.color = Color.parseColor("#DC2626")
        p.isFakeBoldText = true
        canvas.drawText("Overall result is FAILED if any", rcx, 194f, p)
        canvas.drawText("subject is below 35%.", rcx, 208f, p)

        p.style = Paint.Style.STROKE
        p.strokeWidth = 0.8f
        p.color = Color.parseColor("#E2E8F0")
        canvas.drawLine(280f, 226f, 488f, 226f, p)
        p.style = Paint.Style.FILL

        // 3. Guidelines
        p.textSize = 10f
        p.isFakeBoldText = true
        p.color = gold
        canvas.drawText("IMPORTANT GUIDELINES", rcx, 246f, p)

        p.textSize = 8f
        p.isFakeBoldText = false
        p.color = Color.DKGRAY
        canvas.drawText("Please check all details and marks carefully.", rcx, 266f, p)
        canvas.drawText("Contact school for any correction or query.", rcx, 282f, p)
    }

    private fun drawPageTwo(
        canvas: Canvas,
        student: Student,
        results: List<ExamResult>,
        componentsByResult: Map<Long, List<ExamResultComponent>> = emptyMap()
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val tableLeft = 40f
        val tableRight = (a4Width - 40).toFloat()

        paint.color = Color.parseColor("#0F3D6E")
        paint.textSize = 18f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("ACADEMIC PROGRESS STATEMENT", a4Width / 2f, 55f, paint)

        // Header info box
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = Color.parseColor("#CBD5E1")
        canvas.drawRect(tableLeft, 75f, tableRight, 115f, paint)

        paint.style = Paint.Style.FILL
        paint.textSize = 10f
        paint.textAlign = Paint.Align.LEFT
        paint.color = Color.BLACK
        paint.isFakeBoldText = false
        canvas.drawText("Student: ${student.fullName}", tableLeft + 10f, 98f, paint)
        canvas.drawText("Class: ${student.displayClass}", 250f, 98f, paint)
        canvas.drawText("Roll: ${student.rollNo}", 440f, 98f, paint)

        // Table Header
        var currentY = 135f
        val headerHeight = 26f
        paint.color = Color.parseColor("#0F3D6E")
        canvas.drawRect(tableLeft, currentY, tableRight, currentY + headerHeight, paint)

        paint.color = Color.WHITE
        paint.isFakeBoldText = true
        paint.textSize = 9.5f
        val textHeaderY = currentY + 17f
        canvas.drawText("Subject Name", tableLeft + 10f, textHeaderY, paint)
        canvas.drawText("Full", 220f, textHeaderY, paint)
        canvas.drawText("Written", 270f, textHeaderY, paint)
        canvas.drawText("Oral", 325f, textHeaderY, paint)
        canvas.drawText("Practical", 380f, textHeaderY, paint)
        canvas.drawText("Obtained", 445f, textHeaderY, paint)
        canvas.drawText("Grade", 505f, textHeaderY, paint)

        paint.color = Color.BLACK
        paint.isFakeBoldText = false
        var grandFull = 0
        var grandObtained = 0
        var isPassed = true

        val tableTop = currentY
        currentY += headerHeight

        for (res in results) {
            val components = componentsByResult[res.id].orEmpty()
            val customObtained = components.sumOf { it.obtainedMarks }
            val totalObtained = res.obtainedMarks + customObtained

            grandFull += res.fullMarks
            grandObtained += totalObtained
            if (totalObtained < (res.fullMarks * 0.35)) isPassed = false

            val rowHeight = if (components.isNotEmpty()) 38f else 26f
            val baseTextY = currentY + 17f

            paint.color = Color.BLACK
            paint.textSize = 9.5f
            canvas.drawText(res.subjectName, tableLeft + 10f, baseTextY, paint)
            canvas.drawText(res.fullMarks.toString(), 220f, baseTextY, paint)
            canvas.drawText(res.writtenMarks.toString(), 270f, baseTextY, paint)
            canvas.drawText(res.oralMarks.toString(), 325f, baseTextY, paint)
            canvas.drawText(res.practicalMarks.toString(), 380f, baseTextY, paint)
            canvas.drawText(totalObtained.toString(), 445f, baseTextY, paint)
            canvas.drawText(res.grade, 505f, baseTextY, paint)

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
                canvas.drawText("Components: $details", tableLeft + 10f, currentY + 30f, detailPaint)
            }

            paint.color = Color.parseColor("#E2E8F0")
            paint.strokeWidth = 1f
            paint.style = Paint.Style.STROKE
            canvas.drawLine(tableLeft, currentY + rowHeight, tableRight, currentY + rowHeight, paint)
            paint.style = Paint.Style.FILL

            currentY += rowHeight
        }

        // Table border
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = Color.parseColor("#CBD5E1")
        canvas.drawRect(tableLeft, tableTop, tableRight, currentY, paint)
        paint.style = Paint.Style.FILL

        // Summary Card
        currentY += 18f
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
        canvas.drawRoundRect(tableLeft, currentY, tableRight, currentY + 54f, 6f, 6f, paint)

        paint.color = Color.parseColor("#0F3D6E")
        paint.textSize = 10.5f
        paint.isFakeBoldText = true
        canvas.drawText("Grand Total: $grandObtained / $grandFull", tableLeft + 14f, currentY + 23f, paint)
        canvas.drawText(String.format("Percentage: %.2f%%", percentage), tableLeft + 14f, currentY + 41f, paint)
        canvas.drawText("Overall Grade: $overallGrade", 240f, currentY + 32f, paint)

        paint.color = if (isPassed) Color.parseColor("#16A34A") else Color.parseColor("#DC2626")
        paint.textSize = 13f
        canvas.drawText("Result: $finalStatus", 420f, currentY + 32f, paint)

        // Signatures
        val signY = a4Height - 90f
        paint.color = Color.GRAY
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawLine(50f, signY, 180f, signY, paint)
        canvas.drawLine((a4Width - 180).toFloat(), signY, (a4Width - 50).toFloat(), signY, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.BLACK
        paint.textSize = 9.5f
        paint.isFakeBoldText = false
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Class Teacher Signature", 115f, signY + 18f, paint)
        canvas.drawText("Headmaster / Seal", (a4Width - 115).toFloat(), signY + 18f, paint)
    }
}
