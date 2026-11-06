#!/usr/bin/env bash
set -e

echo "=== [1/7] Ensuring Directories Exist ==="
mkdir -p app/src/main/res/layout
mkdir -p app/src/main/java/com/vidyamandir/schoolmanager/data
mkdir -p app/src/main/java/com/vidyamandir/schoolmanager/domain
mkdir -p app/src/main/java/com/vidyamandir/schoolmanager/util
mkdir -p app/src/main/java/com/vidyamandir/schoolmanager/ui

echo "=== [2/7] Updating Database Entities (Version 4 with Exam Definitions & Subjects) ==="
cat << 'FILE_EOF' > app/src/main/java/com/vidyamandir/schoolmanager/data/Entities.kt
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
FILE_EOF

cat << 'FILE_EOF' > app/src/main/java/com/vidyamandir/schoolmanager/data/SchoolDao.kt
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

    @Query("SELECT * FROM students WHERE isActive = 1 ORDER BY className ASC, rollNo ASC")
    fun getAllActiveStudents(): Flow<List<Student>>

    // Exam Definitions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamDefinition): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExamSubjects(subjects: List<ExamSubject>)

    @Query("SELECT * FROM exams ORDER BY id DESC")
    fun getAllExams(): Flow<List<ExamDefinition>>

    @Query("SELECT * FROM exam_subjects WHERE examId = :examId ORDER BY id ASC")
    suspend fun getSubjectsForExam(examId: Long): List<ExamSubject>

    @Delete
    suspend fun deleteExam(exam: ExamDefinition)

    // Exam Results
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResults(results: List<ExamResult>)

    @Query("SELECT * FROM exam_results WHERE studentId = :studentId AND examName = :examName")
    suspend fun getStudentExamResults(studentId: Long, examName: String): List<ExamResult>

    @Query("SELECT * FROM exam_results WHERE approvalStatus = 'PENDING'")
    fun getPendingMarksheets(): Flow<List<ExamResult>>

    @Query("UPDATE exam_results SET approvalStatus = :status WHERE id = :id")
    suspend fun updateMarksheetStatus(id: Long, status: String)

    // Payments
    @Insert
    suspend fun recordPayment(payment: FeePayment): Long

    @Query("SELECT * FROM fee_payments ORDER BY paymentDate DESC")
    fun getAllPayments(): Flow<List<FeePayment>>

    @Query("SELECT * FROM fee_payments WHERE paymentDate >= :startOfDayTimestamp ORDER BY paymentDate DESC")
    fun getTodayPayments(startOfDayTimestamp: Long): Flow<List<FeePayment>>

    @Query("SELECT * FROM fee_payments WHERE studentId = :studentId AND academicYear = :year ORDER BY paymentDate DESC")
    fun getStudentPayments(studentId: Long, year: Int): Flow<List<FeePayment>>

    // Late Fee Rules
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLateFeeRule(rule: LateFeeRule): Long

    @Query("SELECT * FROM late_fee_rules ORDER BY dayFrom ASC")
    suspend fun getAllLateFeeRules(): List<LateFeeRule>
}
FILE_EOF

cat << 'FILE_EOF' > app/src/main/java/com/vidyamandir/schoolmanager/data/AppDatabase.kt
package com.vidyamandir.schoolmanager.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        SchoolClass::class,
        Teacher::class,
        Student::class,
        ExamDefinition::class,
        ExamSubject::class,
        ExamResult::class,
        FeePayment::class,
        LateFeeRule::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun schoolDao(): SchoolDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vivekananda_vidyamandir.db"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        val dao = database.schoolDao()
                        dao.insertLateFeeRule(LateFeeRule(dayFrom = 1, dayTo = 10, fineAmount = 0.0))
                        dao.insertLateFeeRule(LateFeeRule(dayFrom = 11, dayTo = 20, fineAmount = 50.0))
                        dao.insertLateFeeRule(LateFeeRule(dayFrom = 21, dayTo = 30, fineAmount = 100.0))
                    }
                }
            }
        }
    }
}
FILE_EOF

echo "=== [3/7] Writing Set Exams Layouts and Activity ==="
cat << 'FILE_EOF' > app/src/main/res/layout/activity_set_exams.xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#F8FAFC">

    <com.google.android.material.appbar.AppBarLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content">

        <com.google.android.material.appbar.MaterialToolbar
            android:id="@+id/toolbar"
            android:layout_width="match_parent"
            android:layout_height="?attr/actionBarSize"
            android:background="#0F3D6E"
            app:title="Set &amp; Configure Exams"
            app:titleTextColor="@android:color/white"
            app:navigationIcon="@android:drawable/ic_menu_revert" />
    </com.google.android.material.appbar.AppBarLayout>

    <androidx.core.widget.NestedScrollView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:layout_behavior="@string/appbar_scrolling_view_behavior">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="16dp">

            <!-- Card 1: Exam Info -->
            <com.google.android.material.card.MaterialCardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                app:cardCornerRadius="16dp"
                app:cardBackgroundColor="@android:color/white"
                app:strokeWidth="1dp"
                app:strokeColor="#CBD5E1">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="16dp">

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="1. Exam Details"
                        android:textColor="#0F3D6E"
                        android:textSize="15sp"
                        android:textStyle="bold" />

                    <com.google.android.material.textfield.TextInputLayout
                        style="@style/Widget.Material3.TextInputLayout.OutlinedBox.ExposedDropdownMenu"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="10dp"
                        android:hint="Select Class">

                        <AutoCompleteTextView
                            android:id="@+id/actvClassSelect"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:inputType="none" />
                    </com.google.android.material.textfield.TextInputLayout>

                    <com.google.android.material.textfield.TextInputLayout
                        style="@style/Widget.Material3.TextInputLayout.OutlinedBox"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="8dp"
                        android:hint="Exam Name (e.g. 1st Summative Evaluation)">

                        <com.google.android.material.textfield.TextInputEditText
                            android:id="@+id/etExamName"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:text="1st Summative Evaluation 2026" />
                    </com.google.android.material.textfield.TextInputLayout>

                    <com.google.android.material.textfield.TextInputLayout
                        style="@style/Widget.Material3.TextInputLayout.OutlinedBox"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="8dp"
                        android:hint="Exam Date (YYYY-MM-DD)">

                        <com.google.android.material.textfield.TextInputEditText
                            android:id="@+id/etExamDate"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:text="2026-05-10" />
                    </com.google.android.material.textfield.TextInputLayout>

                </LinearLayout>
            </com.google.android.material.card.MaterialCardView>

            <!-- Card 2: Subject Template Builder -->
            <com.google.android.material.card.MaterialCardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="14dp"
                app:cardCornerRadius="16dp"
                app:cardBackgroundColor="@android:color/white"
                app:strokeWidth="1dp"
                app:strokeColor="#CBD5E1">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="16dp">

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="horizontal"
                        android:gravity="center_vertical">

                        <TextView
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:text="2. Configure Marksheet Subjects"
                            android:textColor="#0F3D6E"
                            android:textSize="15sp"
                            android:textStyle="bold" />

                        <com.google.android.material.button.MaterialButton
                            android:id="@+id/btnAddSubject"
                            style="@style/Widget.Material3.Button.TonalButton"
                            android:layout_width="wrap_content"
                            android:layout_height="40dp"
                            android:text="+ Add Subject"
                            android:textSize="11sp"
                            android:textStyle="bold"
                            app:backgroundTint="#FEF3C7"
                            app:cornerRadius="8dp" />
                    </LinearLayout>

                    <androidx.recyclerview.widget.RecyclerView
                        android:id="@+id/rvConfiguredSubjects"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="8dp"
                        android:nestedScrollingEnabled="false" />

                    <com.google.android.material.button.MaterialButton
                        android:id="@+id/btnSaveExamConfig"
                        android:layout_width="match_parent"
                        android:layout_height="52dp"
                        android:layout_marginTop="14dp"
                        android:text="Save Exam &amp; Marksheet Setup"
                        android:textStyle="bold"
                        app:cornerRadius="10dp"
                        app:backgroundTint="#0F3D6E" />

                </LinearLayout>
            </com.google.android.material.card.MaterialCardView>

            <!-- Card 3: Existing Configured Exams -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="16dp"
                android:layout_marginBottom="6dp"
                android:text="Active Configured Exams"
                android:textColor="#1E293B"
                android:textSize="14sp"
                android:textStyle="bold" />

            <androidx.recyclerview.widget.RecyclerView
                android:id="@+id/rvExistingExams"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:nestedScrollingEnabled="false" />

        </LinearLayout>
    </androidx.core.widget.NestedScrollView>
</androidx.coordinatorlayout.widget.CoordinatorLayout>
FILE_EOF

cat << 'FILE_EOF' > app/src/main/res/layout/item_exam_subject_config.xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.card.MaterialCardView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginVertical="4dp"
    app:cardCornerRadius="12dp"
    app:strokeWidth="1dp"
    app:strokeColor="#E2E8F0"
    app:cardBackgroundColor="#F8FAFC">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="12dp">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical">

            <com.google.android.material.textfield.TextInputLayout
                style="@style/Widget.Material3.TextInputLayout.OutlinedBox.Dense"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1.6"
                android:hint="Subject Name">

                <com.google.android.material.textfield.TextInputEditText
                    android:id="@+id/etSubjectName"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content" />
            </com.google.android.material.textfield.TextInputLayout>

            <com.google.android.material.textfield.TextInputLayout
                style="@style/Widget.Material3.TextInputLayout.OutlinedBox.Dense"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="0.9"
                android:layout_marginStart="6dp"
                android:hint="Full Marks">

                <com.google.android.material.textfield.TextInputEditText
                    android:id="@+id/etFullMarks"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:inputType="number"
                    android:text="100" />
            </com.google.android.material.textfield.TextInputLayout>

            <ImageButton
                android:id="@+id/btnDeleteSubject"
                android:layout_width="38dp"
                android:layout_height="38dp"
                android:layout_marginStart="4dp"
                android:background="?attr/selectableItemBackgroundBorderless"
                android:src="@android:drawable/ic_menu_delete" />
        </LinearLayout>

        <!-- Components Breakdown -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:orientation="horizontal"
            android:gravity="center_vertical">

            <com.google.android.material.checkbox.MaterialCheckBox
                android:id="@+id/cbWritten"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:checked="true"
                android:text="Written"
                android:textSize="12sp" />

            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/etMaxWritten"
                android:layout_width="50dp"
                android:layout_height="wrap_content"
                android:textSize="12sp"
                android:inputType="number"
                android:text="70" />

            <com.google.android.material.checkbox.MaterialCheckBox
                android:id="@+id/cbOral"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginStart="8dp"
                android:text="Oral"
                android:textSize="12sp" />

            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/etMaxOral"
                android:layout_width="50dp"
                android:layout_height="wrap_content"
                android:textSize="12sp"
                android:inputType="number"
                android:text="10" />

            <com.google.android.material.checkbox.MaterialCheckBox
                android:id="@+id/cbPractical"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginStart="8dp"
                android:text="Practical"
                android:textSize="12sp" />

            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/etMaxPractical"
                android:layout_width="50dp"
                android:layout_height="wrap_content"
                android:textSize="12sp"
                android:inputType="number"
                android:text="20" />
        </LinearLayout>

    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
FILE_EOF

cat << 'FILE_EOF' > app/src/main/java/com/vidyamandir/schoolmanager/ui/SetExamsActivity.kt
package com.vidyamandir.schoolmanager.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.data.ExamDefinition
import com.vidyamandir.schoolmanager.data.ExamSubject
import com.vidyamandir.schoolmanager.databinding.ActivitySetExamsBinding
import com.vidyamandir.schoolmanager.databinding.ItemExamSubjectConfigBinding
import com.vidyamandir.schoolmanager.databinding.ItemMonthFeeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SubjectConfigItem(
    var name: String,
    var fullMarks: Int = 100,
    var hasWritten: Boolean = true,
    var maxWritten: Int = 70,
    var hasOral: Boolean = false,
    var maxOral: Int = 10,
    var hasPractical: Boolean = false,
    var maxPractical: Int = 20
)

class SetExamsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetExamsBinding
    private lateinit var db: AppDatabase
    private val configuredSubjects = mutableListOf<SubjectConfigItem>()
    private lateinit var subjectAdapter: SubjectConfigAdapter
    private var availableClassUnits = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetExamsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this, lifecycleScope)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupSubjectsRecycler()
        loadClasses()
        observeExams()

        binding.btnAddSubject.setOnClickListener {
            configuredSubjects.add(SubjectConfigItem("New Subject", 100, true, 70, false, 10, false, 20))
            subjectAdapter.notifyItemInserted(configuredSubjects.size - 1)
        }

        binding.btnSaveExamConfig.setOnClickListener {
            saveExamSetup()
        }
    }

    private fun setupSubjectsRecycler() {
        // Preload standard subjects
        configuredSubjects.add(SubjectConfigItem("First Language (Bengali)", 100, true, 70, true, 10, true, 20))
        configuredSubjects.add(SubjectConfigItem("Second Language (English)", 100, true, 80, true, 20, false, 0))
        configuredSubjects.add(SubjectConfigItem("Mathematics", 100, true, 80, false, 0, true, 20))
        configuredSubjects.add(SubjectConfigItem("Physical Science", 100, true, 70, false, 0, true, 30))
        configuredSubjects.add(SubjectConfigItem("Life Science", 100, true, 70, false, 0, true, 30))
        configuredSubjects.add(SubjectConfigItem("History", 100, true, 80, true, 20, false, 0))
        configuredSubjects.add(SubjectConfigItem("Geography", 100, true, 70, false, 0, true, 30))

        subjectAdapter = SubjectConfigAdapter(configuredSubjects)
        binding.rvConfiguredSubjects.layoutManager = LinearLayoutManager(this)
        binding.rvConfiguredSubjects.adapter = subjectAdapter
    }

    private fun loadClasses() {
        lifecycleScope.launch(Dispatchers.IO) {
            val classes = db.schoolDao().getAllClasses().first()
            availableClassUnits.clear()
            for (cls in classes) {
                for (unit in cls.getDisplayUnits()) {
                    val label = if (unit.second.isNotBlank()) "${unit.first} (Sec ${unit.second})" else unit.first
                    availableClassUnits.add(label)
                }
            }
            withContext(Dispatchers.Main) {
                if (availableClassUnits.isNotEmpty()) {
                    val adapter = ArrayAdapter(this@SetExamsActivity, android.R.layout.simple_dropdown_item_1line, availableClassUnits)
                    binding.actvClassSelect.setAdapter(adapter)
                    binding.actvClassSelect.setText(availableClassUnits[0], false)
                }
            }
        }
    }

    private fun saveExamSetup() {
        val selectedClass = binding.actvClassSelect.text?.toString()?.trim() ?: ""
        val examName = binding.etExamName.text?.toString()?.trim() ?: ""
        val examDate = binding.etExamDate.text?.toString()?.trim() ?: "2026-05-10"

        if (selectedClass.isEmpty() || examName.isEmpty()) {
            Toast.makeText(this, "Please select class and exam name", Toast.LENGTH_SHORT).show()
            return
        }

        if (configuredSubjects.isEmpty()) {
            Toast.makeText(this, "Please add at least one subject", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val examId = db.schoolDao().insertExam(
                ExamDefinition(
                    examName = examName,
                    targetClass = selectedClass,
                    examDate = examDate,
                    academicYear = 2026
                )
            )

            val subjectEntities = configuredSubjects.map {
                ExamSubject(
                    examId = examId,
                    subjectName = it.name,
                    fullMarks = it.fullMarks,
                    hasWritten = it.hasWritten,
                    maxWritten = it.maxWritten,
                    hasOral = it.hasOral,
                    maxOral = it.maxOral,
                    hasPractical = it.hasPractical,
                    maxPractical = it.maxPractical
                )
            }
            db.schoolDao().insertExamSubjects(subjectEntities)

            withContext(Dispatchers.Main) {
                Toast.makeText(this@SetExamsActivity, "Exam Configuration Saved for $selectedClass!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeExams() {
        binding.rvExistingExams.layoutManager = LinearLayoutManager(this)
        lifecycleScope.launch {
            db.schoolDao().getAllExams().collect { exams ->
                binding.rvExistingExams.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                        val b = ItemMonthFeeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                        return object : RecyclerView.ViewHolder(b.root) {}
                    }
                    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
                        val ex = exams[pos]
                        val b = ItemMonthFeeBinding.bind(holder.itemView)
                        b.tvMonthName.text = "${ex.examName} (${ex.targetClass})"
                        b.tvFeeBreakdown.text = "Date: ${ex.examDate} • Academic Year: ${ex.academicYear}"
                        b.tvStatusBadge.text = "READY"
                        b.cbSelectMonth.visibility = android.view.View.GONE
                    }
                    override fun getItemCount() = exams.size
                }
            }
        }
    }

    class SubjectConfigAdapter(private val list: MutableList<SubjectConfigItem>) :
        RecyclerView.Adapter<SubjectConfigAdapter.ViewHolder>() {

        class ViewHolder(val b: ItemExamSubjectConfigBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val b = ItemExamSubjectConfigBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(b)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.b.etSubjectName.setText(item.name)
            holder.b.etFullMarks.setText(item.fullMarks.toString())

            holder.b.cbWritten.isChecked = item.hasWritten
            holder.b.etMaxWritten.setText(item.maxWritten.toString())

            holder.b.cbOral.isChecked = item.hasOral
            holder.b.etMaxOral.setText(item.maxOral.toString())

            holder.b.cbPractical.isChecked = item.hasPractical
            holder.b.etMaxPractical.setText(item.maxPractical.toString())

            holder.b.etSubjectName.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    item.name = s.toString()
                }
                override fun afterTextChanged(s: Editable?) {}
            })

            holder.b.cbWritten.setOnCheckedChangeListener { _, isChecked -> item.hasWritten = isChecked }
            holder.b.cbOral.setOnCheckedChangeListener { _, isChecked -> item.hasOral = isChecked }
            holder.b.cbPractical.setOnCheckedChangeListener { _, isChecked -> item.hasPractical = isChecked }

            holder.b.btnDeleteSubject.setOnClickListener {
                val currentPos = holder.adapterPosition
                if (currentPos in list.indices) {
                    list.removeAt(currentPos)
                    notifyItemRemoved(currentPos)
                }
            }
        }

        override fun getItemCount() = list.size
    }
}
FILE_EOF

echo "=== [4/7] Writing Dynamic Mark Entry Layouts & ResultPublishActivity ==="
cat << 'FILE_EOF' > app/src/main/res/layout/activity_result_publish.xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#F8FAFC">

    <com.google.android.material.appbar.AppBarLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content">

        <com.google.android.material.appbar.MaterialToolbar
            android:id="@+id/toolbar"
            android:layout_width="match_parent"
            android:layout_height="?attr/actionBarSize"
            android:background="#0F3D6E"
            app:title="Publish Exam Results"
            app:titleTextColor="@android:color/white"
            app:navigationIcon="@android:drawable/ic_menu_revert" />
    </com.google.android.material.appbar.AppBarLayout>

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical"
        android:padding="14dp"
        app:layout_behavior="@string/appbar_scrolling_view_behavior">

        <!-- Select Exam Dropdown -->
        <com.google.android.material.textfield.TextInputLayout
            style="@style/Widget.Material3.TextInputLayout.OutlinedBox.ExposedDropdownMenu"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="Select Configured Exam">

            <AutoCompleteTextView
                android:id="@+id/actvExamSelect"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:inputType="none" />
        </com.google.android.material.textfield.TextInputLayout>

        <!-- Auto-selected Class Banner -->
        <TextView
            android:id="@+id/tvTargetClassBanner"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="6dp"
            android:background="#FEF3C7"
            android:padding="8dp"
            android:text="Target Class: Select Exam above"
            android:textColor="#92400E"
            android:textSize="12sp"
            android:textStyle="bold" />

        <!-- Student Dropdown -->
        <com.google.android.material.textfield.TextInputLayout
            style="@style/Widget.Material3.TextInputLayout.OutlinedBox.ExposedDropdownMenu"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:hint="Select Student">

            <AutoCompleteTextView
                android:id="@+id/actvStudentSelect"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:inputType="none" />
        </com.google.android.material.textfield.TextInputLayout>

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="10dp"
            android:layout_marginBottom="4dp"
            android:text="Enter Marks (Auto-Calculates Grade &amp; Total)"
            android:textColor="#1E293B"
            android:textSize="13sp"
            android:textStyle="bold" />

        <!-- Subjects List -->
        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/rvStudentMarks"
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_weight="1" />

        <!-- Action Buttons -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:orientation="horizontal">

            <com.google.android.material.button.MaterialButton
                android:id="@+id/btnSaveMarks"
                style="@style/Widget.Material3.Button.OutlinedButton"
                android:layout_width="0dp"
                android:layout_height="50dp"
                android:layout_weight="1"
                android:layout_marginEnd="4dp"
                android:text="Save"
                android:textColor="#0F3D6E"
                app:cornerRadius="10dp"
                app:strokeColor="#0F3D6E" />

            <com.google.android.material.button.MaterialButton
                android:id="@+id/btnSaveAndNext"
                android:layout_width="0dp"
                android:layout_height="50dp"
                android:layout_weight="1.3"
                android:layout_marginHorizontal="4dp"
                android:text="Save &amp; Next"
                android:textStyle="bold"
                app:backgroundTint="#0F3D6E"
                app:cornerRadius="10dp" />

            <com.google.android.material.button.MaterialButton
                android:id="@+id/btnGeneratePdf"
                android:layout_width="0dp"
                android:layout_height="50dp"
                android:layout_weight="1.1"
                android:layout_marginStart="4dp"
                android:text="Print PDF"
                android:textStyle="bold"
                app:backgroundTint="#D97706"
                app:cornerRadius="10dp" />
        </LinearLayout>

    </LinearLayout>
</androidx.coordinatorlayout.widget.CoordinatorLayout>
FILE_EOF

cat << 'FILE_EOF' > app/src/main/res/layout/item_publish_mark_input.xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.card.MaterialCardView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginHorizontal="4dp"
    android:layout_marginVertical="4dp"
    app:cardCornerRadius="12dp"
    app:strokeWidth="1dp"
    app:strokeColor="#CBD5E1"
    app:cardBackgroundColor="@android:color/white">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="12dp">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical">

            <TextView
                android:id="@+id/tvSubjectTitle"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="Subject Name"
                android:textColor="#0F3D6E"
                android:textSize="14sp"
                android:textStyle="bold" />

            <TextView
                android:id="@+id/tvGradeBadge"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:background="#DCFCE7"
                android:paddingHorizontal="8dp"
                android:paddingVertical="2dp"
                android:text="Grade: --"
                android:textColor="#16A34A"
                android:textSize="11sp"
                android:textStyle="bold" />
        </LinearLayout>

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:orientation="horizontal">

            <com.google.android.material.textfield.TextInputLayout
                android:id="@+id/tilWritten"
                style="@style/Widget.Material3.TextInputLayout.OutlinedBox.Dense"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:layout_marginEnd="3dp"
                android:hint="Written">

                <com.google.android.material.textfield.TextInputEditText
                    android:id="@+id/etWritten"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:inputType="number"
                    android:text="0" />
            </com.google.android.material.textfield.TextInputLayout>

            <com.google.android.material.textfield.TextInputLayout
                android:id="@+id/tilOral"
                style="@style/Widget.Material3.TextInputLayout.OutlinedBox.Dense"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:layout_marginHorizontal="3dp"
                android:hint="Oral">

                <com.google.android.material.textfield.TextInputEditText
                    android:id="@+id/etOral"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:inputType="number"
                    android:text="0" />
            </com.google.android.material.textfield.TextInputLayout>

            <com.google.android.material.textfield.TextInputLayout
                android:id="@+id/tilPractical"
                style="@style/Widget.Material3.TextInputLayout.OutlinedBox.Dense"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:layout_marginStart="3dp"
                android:hint="Practical">

                <com.google.android.material.textfield.TextInputEditText
                    android:id="@+id/etPractical"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:inputType="number"
                    android:text="0" />
            </com.google.android.material.textfield.TextInputLayout>
        </LinearLayout>

    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
FILE_EOF

cat << 'FILE_EOF' > app/src/main/java/com/vidyamandir/schoolmanager/ui/ResultPublishActivity.kt
package com.vidyamandir.schoolmanager.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.data.ExamDefinition
import com.vidyamandir.schoolmanager.data.ExamResult
import com.vidyamandir.schoolmanager.data.ExamSubject
import com.vidyamandir.schoolmanager.data.Student
import com.vidyamandir.schoolmanager.databinding.ActivityResultPublishBinding
import com.vidyamandir.schoolmanager.databinding.ItemPublishMarkInputBinding
import com.vidyamandir.schoolmanager.util.ResultPdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class PublishSubjectRow(
    val subjectName: String,
    val fullMarks: Int,
    val hasWritten: Boolean,
    val maxWritten: Int,
    var writtenMarks: Int = 0,
    val hasOral: Boolean,
    val maxOral: Int,
    var oralMarks: Int = 0,
    val hasPractical: Boolean,
    val maxPractical: Int,
    var practicalMarks: Int = 0
) {
    val totalObtained: Int get() = writtenMarks + oralMarks + practicalMarks

    val grade: String
        get() {
            val pct = if (fullMarks > 0) (totalObtained.toDouble() / fullMarks * 100) else 0.0
            return when {
                pct >= 90 -> "AA"
                pct >= 80 -> "A+"
                pct >= 70 -> "A"
                pct >= 60 -> "B+"
                pct >= 50 -> "B"
                pct >= 35 -> "C"
                pct >= 25 -> "D"
                else -> "F"
            }
        }
}

class ResultPublishActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultPublishBinding
    private lateinit var db: AppDatabase
    private lateinit var adapter: PublishMarkAdapter

    private var examsList: List<ExamDefinition> = emptyList()
    private var selectedExam: ExamDefinition? = null
    private var classStudents: List<Student> = emptyList()
    private var currentStudentIndex = 0

    private val subjectRows = mutableListOf<PublishSubjectRow>()
    private var userRole = "ADMIN"
    private var loggedInTeacherName = "Admin"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultPublishBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this, lifecycleScope)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val prefs = getSharedPreferences("vv_auth_session", Context.MODE_PRIVATE)
        userRole = prefs.getString("session_role", "ADMIN") ?: "ADMIN"
        loggedInTeacherName = prefs.getString("teacher_name", "Admin") ?: "Admin"

        adapter = PublishMarkAdapter(subjectRows)
        binding.rvStudentMarks.layoutManager = LinearLayoutManager(this)
        binding.rvStudentMarks.adapter = adapter

        loadExams()
        setupListeners()
    }

    private fun loadExams() {
        lifecycleScope.launch(Dispatchers.IO) {
            examsList = db.schoolDao().getAllExams().first()
            withContext(Dispatchers.Main) {
                if (examsList.isNotEmpty()) {
                    val examNames = examsList.map { "${it.examName} (${it.targetClass})" }
                    val arrayAdapter = ArrayAdapter(this@ResultPublishActivity, android.R.layout.simple_dropdown_item_1line, examNames)
                    binding.actvExamSelect.setAdapter(arrayAdapter)
                    binding.actvExamSelect.setText(examNames[0], false)
                    onExamChosen(examsList[0])
                } else {
                    binding.tvTargetClassBanner.text = "No exams configured yet! Go to 'SET EXAMS' first."
                }
            }
        }
    }

    private fun onExamChosen(exam: ExamDefinition) {
        selectedExam = exam
        binding.tvTargetClassBanner.text = "Target Class: ${exam.targetClass} • Date: ${exam.examDate}"

        lifecycleScope.launch(Dispatchers.IO) {
            val subjects = db.schoolDao().getSubjectsForExam(exam.id)
            val allStudents = db.schoolDao().getAllActiveStudents().first()
            classStudents = allStudents.filter { it.displayClass.equals(exam.targetClass, ignoreCase = true) }

            withContext(Dispatchers.Main) {
                subjectRows.clear()
                subjects.forEach { s ->
                    subjectRows.add(
                        PublishSubjectRow(
                            subjectName = s.subjectName,
                            fullMarks = s.fullMarks,
                            hasWritten = s.hasWritten,
                            maxWritten = s.maxWritten,
                            hasOral = s.hasOral,
                            maxOral = s.maxOral,
                            hasPractical = s.hasPractical,
                            maxPractical = s.maxPractical
                        )
                    )
                }
                adapter.notifyDataSetChanged()

                if (classStudents.isNotEmpty()) {
                    val studentNames = classStudents.map { "${it.fullName} (Roll ${it.rollNo})" }
                    val studentAdapter = ArrayAdapter(this@ResultPublishActivity, android.R.layout.simple_dropdown_item_1line, studentNames)
                    binding.actvStudentSelect.setAdapter(studentAdapter)
                    binding.actvStudentSelect.setText(studentNames[0], false)
                    currentStudentIndex = 0
                }
            }
        }
    }

    private fun setupListeners() {
        binding.actvExamSelect.setOnItemClickListener { _, _, position, _ ->
            onExamChosen(examsList[position])
        }

        binding.actvStudentSelect.setOnItemClickListener { _, _, position, _ ->
            currentStudentIndex = position
        }

        binding.btnSaveMarks.setOnClickListener {
            saveCurrentStudentMarks(onComplete = {
                val statusMsg = if (userRole == "ADMIN") "Marks Saved & Approved!" else "Marks submitted for Admin verification!"
                Toast.makeText(this, statusMsg, Toast.LENGTH_SHORT).show()
            })
        }

        binding.btnSaveAndNext.setOnClickListener {
            saveCurrentStudentMarks(onComplete = {
                if (currentStudentIndex < classStudents.size - 1) {
                    currentStudentIndex++
                    val nextStudent = classStudents[currentStudentIndex]
                    binding.actvStudentSelect.setText("${nextStudent.fullName} (Roll ${nextStudent.rollNo})", false)
                    Toast.makeText(this, "Next: ${nextStudent.fullName}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "All students of this class completed!", Toast.LENGTH_LONG).show()
                }
            })
        }

        binding.btnGeneratePdf.setOnClickListener {
            saveCurrentStudentMarks(onComplete = {
                generateAndOpenPdf()
            })
        }
    }

    private fun saveCurrentStudentMarks(onComplete: () -> Unit) {
        val exam = selectedExam
        if (exam == null || classStudents.isEmpty()) {
            Toast.makeText(this, "No student or exam selected", Toast.LENGTH_SHORT).show()
            return
        }

        val student = classStudents[currentStudentIndex]
        val status = if (userRole == "ADMIN") "APPROVED" else "PENDING"

        val results = subjectRows.map {
            ExamResult(
                studentId = student.id,
                examName = exam.examName,
                academicYear = exam.academicYear,
                subjectName = it.subjectName,
                fullMarks = it.fullMarks,
                writtenMarks = it.writtenMarks,
                oralMarks = it.oralMarks,
                practicalMarks = it.practicalMarks,
                grade = it.grade,
                submittedByTeacher = loggedInTeacherName,
                approvalStatus = status
            )
        }

        lifecycleScope.launch(Dispatchers.IO) {
            db.schoolDao().insertResults(results)
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    private fun generateAndOpenPdf() {
        val student = classStudents[currentStudentIndex]
        val exam = selectedExam ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            val examResults = db.schoolDao().getStudentExamResults(student.id, exam.examName)
            withContext(Dispatchers.Main) {
                if (examResults.isEmpty()) {
                    Toast.makeText(this@ResultPublishActivity, "Please save marks first", Toast.LENGTH_SHORT).show()
                    return@withContext
                }

                try {
                    val pdfDir = File(cacheDir, "report_cards").apply { if (!exists()) mkdirs() }
                    val pdfFile = File(pdfDir, "Report_${student.fullName.replace(" ", "_")}.pdf")

                    val generator = ResultPdfGenerator(this@ResultPublishActivity)
                    generator.generateReportCard(student, exam.examName, examResults, pdfFile)

                    val uri: Uri = FileProvider.getUriForFile(
                        this@ResultPublishActivity,
                        "${packageName}.fileprovider",
                        pdfFile
                    )
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NO_HISTORY
                    }
                    startActivity(Intent.createChooser(intent, "Open Report Card"))
                } catch (e: Exception) {
                    Toast.makeText(this@ResultPublishActivity, "PDF Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    class PublishMarkAdapter(private val list: List<PublishSubjectRow>) :
        RecyclerView.Adapter<PublishMarkAdapter.ViewHolder>() {

        class ViewHolder(val b: ItemPublishMarkInputBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val b = ItemPublishMarkInputBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(b)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.b.tvSubjectTitle.text = "${item.subjectName} (Full: ${item.fullMarks})"

            // Written Field
            holder.b.tilWritten.visibility = if (item.hasWritten) View.VISIBLE else View.GONE
            holder.b.tilWritten.hint = "Written (/${item.maxWritten})"
            holder.b.etWritten.setText(item.writtenMarks.toString())

            // Oral Field
            holder.b.tilOral.visibility = if (item.hasOral) View.VISIBLE else View.GONE
            holder.b.tilOral.hint = "Oral (/${item.maxOral})"
            holder.b.etOral.setText(item.oralMarks.toString())

            // Practical Field
            holder.b.tilPractical.visibility = if (item.hasPractical) View.VISIBLE else View.GONE
            holder.b.tilPractical.hint = "Practical (/${item.maxPractical})"
            holder.b.etPractical.setText(item.practicalMarks.toString())

            val updateBadge = {
                holder.b.tvGradeBadge.text = "Grade: ${item.grade} (${item.totalObtained}/${item.fullMarks})"
            }
            updateBadge()

            holder.b.etWritten.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    val input = s.toString().toIntOrNull() ?: 0
                    item.writtenMarks = input.coerceAtMost(item.maxWritten)
                    updateBadge()
                }
                override fun afterTextChanged(s: Editable?) {}
            })

            holder.b.etOral.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    val input = s.toString().toIntOrNull() ?: 0
                    item.oralMarks = input.coerceAtMost(item.maxOral)
                    updateBadge()
                }
                override fun afterTextChanged(s: Editable?) {}
            })

            holder.b.etPractical.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    val input = s.toString().toIntOrNull() ?: 0
                    item.practicalMarks = input.coerceAtMost(item.maxPractical)
                    updateBadge()
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }

        override fun getItemCount() = list.size
    }
}
FILE_EOF

echo "=== [5/7] Updating ResultPdfGenerator with Oral, Written, Practical & Grade Table ==="
cat << 'FILE_EOF' > app/src/main/java/com/vidyamandir/schoolmanager/util/ResultPdfGenerator.kt
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
        paint.textSize = 24f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("VIVEKANANDA VIDYAMANDIR", (a4Width / 2).toFloat(), 95f, paint)

        paint.textSize = 12f
        paint.isFakeBoldText = false
        paint.color = Color.DKGRAY
        canvas.drawText("Dhupguri, Jalpaiguri, West Bengal - 735210", (a4Width / 2).toFloat(), 120f, paint)

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
FILE_EOF

echo "=== [6/7] Updating DashboardFragment & AndroidManifest ==="
cat << 'FILE_EOF' > app/src/main/java/com/vidyamandir/schoolmanager/ui/DashboardFragment.kt
package com.vidyamandir.schoolmanager.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.databinding.FragmentDashboardBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = AppDatabase.getDatabase(requireContext(), viewLifecycleOwner.lifecycleScope)

        observeStats()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        refreshTodayPayment()
    }

    private fun observeStats() {
        viewLifecycleOwner.lifecycleScope.launch {
            db.schoolDao().getStudentCount().collect { count ->
                _binding?.tvStudentCount?.text = count.toString()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            db.schoolDao().getClassCount().collect { count ->
                _binding?.tvClassCount?.text = count.toString()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            db.schoolDao().getTeacherCount().collect { count ->
                _binding?.tvTeacherCount?.text = count.toString()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            db.schoolDao().getPendingMarksheetCount().collect { count ->
                _binding?.tvPendingMarksheetCount?.text = "$count Pending"
            }
        }
    }

    private fun refreshTodayPayment() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val todayList = db.schoolDao().getTodayPayments(cal.timeInMillis).first()
            val todayTotal = todayList.sumOf { it.totalAmount }

            withContext(Dispatchers.Main) {
                _binding?.tvDailyCollection?.text = "₹${String.format("%.2f", todayTotal)}"
            }
        }
    }

    private fun setupListeners() {
        binding.cardCollectFee.setOnClickListener {
            startActivity(Intent(requireContext(), FeeCollectionActivity::class.java))
        }
        binding.btnQuickCollect.setOnClickListener {
            startActivity(Intent(requireContext(), FeeCollectionActivity::class.java))
        }

        binding.cardStudents.setOnClickListener {
            startActivity(Intent(requireContext(), StudentManagementActivity::class.java))
        }
        binding.cardClasses.setOnClickListener {
            startActivity(Intent(requireContext(), ClassManagementActivity::class.java))
        }
        binding.cardTeachers.setOnClickListener {
            startActivity(Intent(requireContext(), TeacherManagementActivity::class.java))
        }

        binding.cardTrackPayment.setOnClickListener {
            startActivity(Intent(requireContext(), TrackPaymentActivity::class.java))
        }
        binding.cardApprovedMarksheet.setOnClickListener {
            startActivity(Intent(requireContext(), MarksheetApprovalActivity::class.java))
        }

        binding.cardSetExams.setOnClickListener {
            startActivity(Intent(requireContext(), SetExamsActivity::class.java))
        }
        binding.cardPublishResult.setOnClickListener {
            startActivity(Intent(requireContext(), ResultPublishActivity::class.java))
        }

        binding.cardMessage.setOnClickListener {
            startActivity(Intent(requireContext(), WhatsAppMessageActivity::class.java))
        }
        binding.btnOpenMessage.setOnClickListener {
            startActivity(Intent(requireContext(), WhatsAppMessageActivity::class.java))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
FILE_EOF

cat << 'FILE_EOF' > app/src/main/AndroidManifest.xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:allowBackup="true"
        android:icon="@android:drawable/sym_def_app_icon"
        android:label="Vivekananda Vidyamandir"
        android:roundIcon="@android:drawable/sym_def_app_icon"
        android:supportsRtl="true"
        android:theme="@style/Theme.SchoolManager">

        <activity
            android:name=".ui.LoginActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <activity android:name=".MainActivity" android:exported="false" />
        <activity android:name=".ui.TeacherPortalActivity" android:exported="false" />
        <activity android:name=".ui.FeeCollectionActivity" android:exported="false" />
        <activity android:name=".ui.SetExamsActivity" android:exported="false" />
        <activity android:name=".ui.ResultPublishActivity" android:exported="false" />
        <activity android:name=".ui.StudentManagementActivity" android:exported="false" />
        <activity android:name=".ui.AddStudentActivity" android:exported="false" />
        <activity android:name=".ui.ClassManagementActivity" android:exported="false" />
        <activity android:name=".ui.TeacherManagementActivity" android:exported="false" />
        <activity android:name=".ui.AddTeacherActivity" android:exported="false" />
        <activity android:name=".ui.StudentDuesListActivity" android:exported="false" />
        <activity android:name=".ui.TrackPaymentActivity" android:exported="false" />
        <activity android:name=".ui.MarksheetApprovalActivity" android:exported="false" />
        <activity android:name=".ui.WhatsAppMessageActivity" android:exported="false" />

        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>

    </application>

</manifest>
FILE_EOF

echo "=== [7/7] Staging, Committing and Pushing to GitHub ==="
git add .
git commit -m "feat: complete Set Exams template builder, dynamic Marksheet publisher with role verification, and fast-track Save & Next"
git push origin main

echo "================================================================="
echo " SUCCESS! Set Exams & Publish Result pipeline pushed to GitHub. "
echo " Check GitHub Actions to download your compiled APK.             "
echo "================================================================="
