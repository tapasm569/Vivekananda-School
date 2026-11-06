package com.vidyamandir.schoolmanager.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "classes")
data class SchoolClass(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val className: String,
    val monthlyFee: Double,
    val hasSections: Boolean = false,
    val sections: String = ""
) {
    fun getDisplayUnits(): List<Pair<String, String>> {
        if (!hasSections || sections.isBlank()) {
            return listOf(Pair(className, ""))
        }
        return sections.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { sec -> Pair(className, sec) }
    }
}

@Entity(tableName = "teachers")
data class Teacher(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val password: String,
    val photoUri: String? = null
)

@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val admissionNo: String,
    val fullName: String,
    val className: String,
    val section: String = "",
    val rollNo: Int,
    val fatherName: String = "",
    val motherName: String = "",
    val parentPhone: String,
    val address: String = "",
    val photoUri: String? = null,
    val admissionMonth: Int = 1,
    val monthlyFee: Double,
    val isActive: Boolean = true
) {
    val displayClass: String
        get() = if (section.isNotBlank()) "$className (Sec $section)" else className
}

@Entity(tableName = "exams")
data class ExamDefinition(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val examName: String,
    val targetClass: String,
    val examDate: String,
    val academicYear: Int = 2026
)

@Entity(
    tableName = "exam_subjects",
    foreignKeys = [
        ForeignKey(
            entity = ExamDefinition::class,
            parentColumns = ["id"],
            childColumns = ["examId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["examId"])]
)
data class ExamSubject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val examId: Long,
    val subjectName: String,
    val fullMarks: Int = 100,
    val hasWritten: Boolean = true,
    val maxWritten: Int = 70,
    val hasOral: Boolean = false,
    val maxOral: Int = 10,
    val hasPractical: Boolean = false,
    val maxPractical: Int = 20
)

@Entity(
    tableName = "exam_results",
    foreignKeys = [
        ForeignKey(
            entity = Student::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["studentId"])]
)
data class ExamResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val examName: String,
    val academicYear: Int,
    val subjectName: String,
    val fullMarks: Int,
    val writtenMarks: Int = 0,
    val oralMarks: Int = 0,
    val practicalMarks: Int = 0,
    val grade: String,
    val submittedByTeacher: String = "Admin",
    val approvalStatus: String = "APPROVED" // "PENDING", "APPROVED", "REJECTED"
) {
    val obtainedMarks: Int get() = writtenMarks + oralMarks + practicalMarks
}

@Entity(
    tableName = "fee_payments",
    foreignKeys = [
        ForeignKey(
            entity = Student::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["studentId"])]
)
data class FeePayment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val studentName: String = "",
    val receiptNo: String,
    val academicYear: Int,
    val paidMonths: String,
    val baseAmount: Double,
    val lateFeeAmount: Double,
    val totalAmount: Double,
    val paymentDate: Long
)

@Entity(tableName = "late_fee_rules")
data class LateFeeRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayFrom: Int,
    val dayTo: Int,
    val fineAmount: Double
)
