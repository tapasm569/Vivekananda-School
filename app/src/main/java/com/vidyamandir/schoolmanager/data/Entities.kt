package com.vidyamandir.schoolmanager.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
@Entity(tableName = "classes")
data class SchoolClass(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val className: String = "",
    val monthlyFee: Double = 0.0,
    val hasSections: Boolean = false,
    val sections: String = "",
    @androidx.room.ColumnInfo(defaultValue = "''")
    val subjects: String = ""
) {
    @Exclude
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

@IgnoreExtraProperties
@Entity(tableName = "teachers")
data class Teacher(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val phone: String = "",
    val password: String = "",
    val photoUri: String? = null
)

@IgnoreExtraProperties
@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val admissionNo: String = "",
    val fullName: String = "",
    val className: String = "",
    val section: String = "",
    val rollNo: Int = 1,
    val fatherName: String = "",
    val motherName: String = "",
    val parentPhone: String = "",
    val address: String = "",
    val photoUri: String? = null,
    val admissionMonth: Int = 1,
    val monthlyFee: Double = 0.0,
    val isActive: Boolean = true
) {
    @get:Exclude
    val displayClass: String
        get() = if (section.isNotBlank()) "$className (Sec $section)" else className
}

@IgnoreExtraProperties
@Entity(tableName = "exams")
data class ExamDefinition(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val examName: String = "",
    val targetClass: String = "",
    val examDate: String = "",
    val academicYear: Int = 2026
)

@IgnoreExtraProperties
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
    val examId: Long = 0,
    val subjectName: String = "",
    val fullMarks: Int = 100,
    val hasWritten: Boolean = true,
    val maxWritten: Int = 70,
    val hasOral: Boolean = false,
    val maxOral: Int = 10,
    val hasPractical: Boolean = false,
    val maxPractical: Int = 20
)

@IgnoreExtraProperties
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
    val studentId: Long = 0,
    val examName: String = "",
    val academicYear: Int = 2026,
    val subjectName: String = "",
    val fullMarks: Int = 100,
    val writtenMarks: Int = 0,
    val oralMarks: Int = 0,
    val practicalMarks: Int = 0,
    val grade: String = "",
    val submittedByTeacher: String = "Admin",
    val approvalStatus: String = "APPROVED"
) {
    @get:Exclude
    val obtainedMarks: Int get() = writtenMarks + oralMarks + practicalMarks
}

@IgnoreExtraProperties
@Entity(
    tableName = "exam_subject_components",
    foreignKeys = [
        ForeignKey(
            entity = ExamSubject::class,
            parentColumns = ["id"],
            childColumns = ["examSubjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["examSubjectId"])]
)
data class ExamSubjectComponent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val examSubjectId: Long = 0,
    val componentName: String = "",
    val maxMarks: Int = 0,
    val position: Int = 0
)

@IgnoreExtraProperties
@Entity(
    tableName = "exam_result_components",
    foreignKeys = [
        ForeignKey(
            entity = ExamResult::class,
            parentColumns = ["id"],
            childColumns = ["examResultId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["examResultId"])]
)
data class ExamResultComponent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val examResultId: Long = 0,
    val componentName: String = "",
    val maxMarks: Int = 0,
    val obtainedMarks: Int = 0,
    val position: Int = 0
)

@IgnoreExtraProperties
@Entity(
    tableName = "fee_payments",
    indices = [Index(value = ["studentId"])]
)
data class FeePayment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long = 0,
    val studentName: String = "",
    val receiptNo: String = "",
    val academicYear: Int = 2026,
    val paidMonths: String = "",
    val baseAmount: Double = 0.0,
    val lateFeeAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paymentDate: Long = 0L,
    val waivedFineAmount: Double = 0.0
)

@IgnoreExtraProperties
@Entity(tableName = "late_fee_rules")
data class LateFeeRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayFrom: Int = 0,
    val dayTo: Int = 0,
    val fineAmount: Double = 0.0
)
