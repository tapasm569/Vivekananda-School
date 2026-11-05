package in.vidyamandir.schoolmanager.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SchoolDao {

    // --- STUDENT OPERATIONS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student): Long

    @Update
    suspend fun updateStudent(student: Student)

    @Query("SELECT * FROM students WHERE isActive = 1 ORDER BY className ASC, rollNo ASC")
    fun getAllActiveStudents(): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE id = :studentId LIMIT 1")
    suspend fun getStudentById(studentId: Long): Student?

    @Query("SELECT * FROM students WHERE className = :className AND isActive = 1 ORDER BY rollNo ASC")
    suspend fun getStudentsByClass(className: String): List<Student>

    // --- FEE PAYMENT OPERATIONS ---
    @Insert
    suspend fun recordPayment(payment: FeePayment): Long

    @Query("SELECT * FROM fee_payments WHERE studentId = :studentId AND academicYear = :year ORDER BY paymentDate DESC")
    fun getStudentPayments(studentId: Long, year: Int): Flow<List<FeePayment>>

    @Query("SELECT * FROM fee_payments WHERE academicYear = :year ORDER BY paymentDate DESC")
    fun getAllPaymentsForYear(year: Int): Flow<List<FeePayment>>

    // --- LATE FEE RULES ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLateFeeRule(rule: LateFeeRule): Long

    @Query("SELECT * FROM late_fee_rules ORDER BY dayFrom ASC")
    suspend fun getAllLateFeeRules(): List<LateFeeRule>

    @Query("DELETE FROM late_fee_rules")
    suspend fun clearLateFeeRules()

    // --- EXAM RESULTS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: ExamResult): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResults(results: List<ExamResult>)

    @Query("SELECT * FROM exam_results WHERE studentId = :studentId AND examName = :examName AND academicYear = :year")
    suspend fun getStudentExamResults(studentId: Long, examName: String, year: Int): List<ExamResult>

    @Query("SELECT * FROM exam_results WHERE studentId = :studentId AND academicYear = :year")
    suspend fun getAllResultsForStudent(studentId: Long, year: Int): List<ExamResult>
}
