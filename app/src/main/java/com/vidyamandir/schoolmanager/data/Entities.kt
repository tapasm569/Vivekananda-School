package com.vidyamandir.schoolmanager.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "classes")
data class SchoolClass(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val className: String,             // e.g. "Class 5" or "Nursery"
    val monthlyFee: Double,            // e.g. 500.0
    val hasSections: Boolean = false,  // true only when divided
    val sections: String = ""          // comma-separated: "A,B"
) {
    fun getDisplayUnits(): List<Pair<String, String>> {
        // Returns list of Pair(ClassName, Section)
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
    val phone: String,                 // Used as contact & ID
    val password: String,              // Teacher login password
    val photoUri: String? = null
)

@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val admissionNo: String,
    val fullName: String,
    val className: String,
    val section: String = "",          // Empty if class has no section
    val rollNo: Int,
    val fatherName: String = "",
    val motherName: String = "",
    val parentPhone: String,
    val address: String = "",
    val photoUri: String? = null,
    val admissionMonth: Int = 1,       // 1 for Jan .. 12 for Dec
    val monthlyFee: Double,
    val isActive: Boolean = true
) {
    val displayClass: String
        get() = if (section.isNotBlank()) "$className (Sec $section)" else className
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
    ]
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

@Entity(
    tableName = "exam_results",
    foreignKeys = [
        ForeignKey(
            entity = Student::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ExamResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val examName: String,
    val academicYear: Int,
    val subjectName: String,
    val fullMarks: Int,
    val writtenMarks: Int,
    val practicalMarks: Int,
    val grade: String,
    val submittedByTeacher: String = "Admin",
    val approvalStatus: String = "APPROVED" // "PENDING", "APPROVED", "REJECTED"
) {
    val obtainedMarks: Int get() = writtenMarks + practicalMarks
}
