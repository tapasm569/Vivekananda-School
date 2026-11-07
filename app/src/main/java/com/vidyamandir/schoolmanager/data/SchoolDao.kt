package com.vidyamandir.schoolmanager.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SchoolDao {

    @Query("SELECT COUNT(*) FROM students WHERE isActive = 1")
    fun getStudentCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM classes")
    fun getClassCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM teachers")
    fun getTeacherCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM exam_results WHERE approvalStatus = 'PENDING'")
    fun getPendingMarksheetCount(): Flow<Int>

    // Classes
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClass(schoolClass: SchoolClass): Long

    @Update
    suspend fun updateClass(schoolClass: SchoolClass)

    @Query("SELECT * FROM classes ORDER BY id ASC")
    fun getAllClasses(): Flow<List<SchoolClass>>

    @Query("SELECT * FROM classes WHERE className = :name LIMIT 1")
    suspend fun getClassByName(name: String): SchoolClass?

    // Teachers
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeacher(teacher: Teacher): Long

    @Query("SELECT * FROM teachers ORDER BY name ASC")
    fun getAllTeachers(): Flow<List<Teacher>>

    @Query("SELECT * FROM teachers WHERE password = :pass LIMIT 1")
    suspend fun getTeacherByPassword(pass: String): Teacher?

    // Students
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student): Long
    @Update
    suspend fun updateStudent(student: Student)


    @Query("SELECT * FROM students WHERE isActive = 1 ORDER BY className ASC, rollNo ASC")
    fun getAllActiveStudents(): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE id = :studentId LIMIT 1")
    suspend fun getStudentById(studentId: Long): Student?

    // Exam Definitions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamDefinition): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExamSubjects(subjects: List<ExamSubject>): List<Long>

    @Query("SELECT * FROM exams ORDER BY id DESC")
    fun getAllExams(): Flow<List<ExamDefinition>>

    @Query("""
        SELECT * FROM exams
        WHERE examName = :examName
        AND targetClass = :targetClass
        AND academicYear = :academicYear
        LIMIT 1
    """)
    suspend fun findExistingExam(
        examName: String,
        targetClass: String,
        academicYear: Int
    ): ExamDefinition?

    @Query("SELECT * FROM exams WHERE targetClass = :targetClass ORDER BY id DESC")
    suspend fun getExamsForClass(targetClass: String): List<ExamDefinition>

    @Query("SELECT * FROM exam_subjects WHERE examId = :examId ORDER BY id ASC")
    suspend fun getSubjectsForExam(examId: Long): List<ExamSubject>

    @Delete
    suspend fun deleteExam(exam: ExamDefinition)

    // Exam Results
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResults(results: List<ExamResult>)

    @Query("SELECT * FROM exam_results WHERE studentId = :studentId AND examName = :examName")
    suspend fun getStudentExamResults(studentId: Long, examName: String): List<ExamResult>

    @Query("""
        SELECT * FROM exam_results
        WHERE studentId = :studentId
        AND examName = :examName
        AND academicYear = :academicYear
        ORDER BY id ASC
    """)
    suspend fun getStudentExamResultsForYear(
        studentId: Long,
        examName: String,
        academicYear: Int
    ): List<ExamResult>

    @Query("""
        SELECT * FROM exam_results
        WHERE studentId = :studentId
        AND examName = :examName
        AND academicYear = :academicYear
        AND subjectName = :subjectName
        LIMIT 1
    """)
    suspend fun getExistingExamResult(
        studentId: Long,
        examName: String,
        academicYear: Int,
        subjectName: String
    ): ExamResult?

    @Query("""
        SELECT * FROM exam_results
        WHERE examName = :examName
        AND academicYear = :academicYear
        ORDER BY studentId ASC, id ASC
    """)
    suspend fun getResultsForExam(
        examName: String,
        academicYear: Int
    ): List<ExamResult>

    @Query("SELECT * FROM exam_results WHERE approvalStatus = 'PENDING'")
    fun getPendingMarksheets(): Flow<List<ExamResult>>

    @Query("UPDATE exam_results SET approvalStatus = :status WHERE id = :id")
    suspend fun updateMarksheetStatus(id: Long, status: String)

    // Exam component configuration
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExamSubjectComponents(
        components: List<ExamSubjectComponent>
    )

    @Query("SELECT * FROM exam_subject_components WHERE examSubjectId = :subjectId ORDER BY position ASC")
    suspend fun getExamSubjectComponents(subjectId: Long): List<ExamSubjectComponent>

    // Student component marks
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExamResult(result: ExamResult): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExamResultComponents(
        components: List<ExamResultComponent>
    )

    @Query("SELECT * FROM exam_result_components WHERE examResultId = :resultId ORDER BY position ASC")
    suspend fun getExamResultComponents(resultId: Long): List<ExamResultComponent>

    // Payments
    @Insert
    suspend fun recordPayment(payment: FeePayment): Long

    @Query("SELECT * FROM fee_payments ORDER BY paymentDate DESC")
    fun getAllPayments(): Flow<List<FeePayment>>
    @Query("SELECT * FROM fee_payments WHERE studentId = :studentId ORDER BY paymentDate DESC")
    fun getAllStudentPayments(studentId: Long): Flow<List<FeePayment>>


    @Query("SELECT * FROM fee_payments WHERE paymentDate >= :startOfDayTimestamp ORDER BY paymentDate DESC")
    fun getTodayPayments(startOfDayTimestamp: Long): Flow<List<FeePayment>>

    @Query("SELECT * FROM fee_payments WHERE studentId = :studentId AND academicYear = :year ORDER BY paymentDate DESC")
    fun getStudentPayments(studentId: Long, year: Int): Flow<List<FeePayment>>

    // Late Fee Rules
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLateFeeRule(rule: LateFeeRule): Long

    @Query("SELECT * FROM late_fee_rules ORDER BY dayFrom ASC")
    suspend fun getAllLateFeeRules(): List<LateFeeRule>

    @Query("DELETE FROM late_fee_rules")
    suspend fun deleteAllLateFeeRules()

    @Transaction
    suspend fun replaceAllLateFeeRules(rules: List<LateFeeRule>) {
        deleteAllLateFeeRules()
        rules.forEach { insertLateFeeRule(it) }
    }
}
