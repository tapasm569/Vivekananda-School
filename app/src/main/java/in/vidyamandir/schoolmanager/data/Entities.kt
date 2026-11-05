package in.vidyamandir.schoolmanager.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val admissionNo: String,
    val fullName: String,
    val className: String,
    val section: String,
    val rollNo: Int,
    val admissionMonth: Int,   // 1 for Jan .. 12 for Dec
    val monthlyFee: Double,
    val parentPhone: String,
    val isActive: Boolean = true
)

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
    val receiptNo: String,
    val academicYear: Int,
    val paidMonths: String,    // e.g. "1,2,3"
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
    val grade: String
) {
    val obtainedMarks: Int get() = writtenMarks + practicalMarks
}
