package com.vidyamandir.schoolmanager.util

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*
import com.vidyamandir.schoolmanager.data.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

object FirebaseSyncManager {
    private const val TAG = "FirebaseSyncManager"
    const val DATABASE_URL = "https://vidyamandir-a3340-default-rtdb.asia-southeast1.firebasedatabase.app"

    private var rtdbInstance: FirebaseDatabase? = null
    private var initialized = false

    fun getDatabase(): FirebaseDatabase {
        rtdbInstance?.let { return it }
        val db = FirebaseDatabase.getInstance(DATABASE_URL)
        rtdbInstance = db
        return db
    }

    fun init(context: Context) {
        if (initialized) return
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            val db = getDatabase()
            initialized = true
            startRealtimeListener(context)
            Log.d(TAG, "Connected to Firebase RTDB: $DATABASE_URL")
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization error: ${e.message}", e)
        }
    }

    fun startRealtimeListener(context: Context) {
        val db = getDatabase()
        val appContext = context.applicationContext
        val scope = CoroutineScope(Dispatchers.IO)

        fun <T : Any> listenList(path: String, clazz: Class<T>, onData: suspend (List<T>) -> Unit) {
            db.getReference(path).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<T>()
                    for (child in snapshot.children) {
                        try {
                            val item = child.getValue(clazz)
                            if (item != null) list.add(item)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing item in $path: ${e.message}")
                        }
                    }
                    if (list.isNotEmpty()) {
                        scope.launch {
                            try { onData(list) } catch (_: Exception) {}
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Listener cancelled for $path: ${error.message}")
                }
            })
        }

        val room = AppDatabase.getDatabase(appContext, scope)
        val dao = room.schoolDao()

        listenList("classes", SchoolClass::class.java) { classes ->
            classes.forEach { dao.insertClass(it) }
        }

        listenList("teachers", Teacher::class.java) { teachers ->
            teachers.forEach { dao.insertTeacher(it) }
        }

        listenList("students", Student::class.java) { students ->
            students.forEach { dao.insertStudent(it) }
        }

        listenList("exams", ExamDefinition::class.java) { exams ->
            exams.forEach { dao.insertExam(it) }
        }

        listenList("exam_subjects", ExamSubject::class.java) { subjects ->
            dao.insertExamSubjects(subjects)
        }

        listenList("exam_subject_components", ExamSubjectComponent::class.java) { components ->
            dao.insertExamSubjectComponents(components)
        }

        listenList("exam_results", ExamResult::class.java) { results ->
            dao.insertResults(results)
        }

        listenList("exam_result_components", ExamResultComponent::class.java) { components ->
            dao.insertExamResultComponents(components)
        }

        listenList("fee_payments", FeePayment::class.java) { payments ->
            payments.forEach { dao.recordPayment(it) }
        }

        listenList("late_fee_rules", LateFeeRule::class.java) { rules ->
            dao.replaceAllLateFeeRules(rules)
        }

        db.getReference("school_profile").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val prefs = appContext.getSharedPreferences("vv_school_profile", Context.MODE_PRIVATE)
                val editor = prefs.edit()
                snapshot.children.forEach { child ->
                    val k = child.key ?: return@forEach
                    val v = child.value?.toString() ?: return@forEach
                    editor.putString(k, v)
                }
                editor.apply()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Listener error for school_profile: ${error.message}")
            }
        })
    }

    fun syncAllLocalToCloud(
        context: Context,
        scope: CoroutineScope,
        onComplete: ((Boolean, String) -> Unit)? = null
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(context.applicationContext, scope)
                val dao = db.schoolDao()
                val rtdb = getDatabase()
                val tasks = mutableListOf<Task<Void>>()

                val classes = dao.getAllClasses().first()
                classes.forEach {
                    tasks.add(rtdb.getReference("classes").child(it.id.toString()).setValue(it))
                }

                val teachers = dao.getAllTeachers().first()
                teachers.forEach {
                    tasks.add(rtdb.getReference("teachers").child(it.id.toString()).setValue(it))
                }

                val students = dao.getAllActiveStudents().first()
                students.forEach {
                    tasks.add(rtdb.getReference("students").child(it.id.toString()).setValue(it))
                }

                val exams = dao.getAllExams().first()
                exams.forEach { exam ->
                    tasks.add(rtdb.getReference("exams").child(exam.id.toString()).setValue(exam))
                }

                val subjects = dao.getAllExamSubjects()
                subjects.forEach {
                    tasks.add(rtdb.getReference("exam_subjects").child(it.id.toString()).setValue(it))
                }

                val subjectComponents = dao.getAllExamSubjectComponents()
                subjectComponents.forEach {
                    tasks.add(rtdb.getReference("exam_subject_components").child(it.id.toString()).setValue(it))
                }

                val results = dao.getAllExamResults()
                results.forEach {
                    tasks.add(rtdb.getReference("exam_results").child(it.id.toString()).setValue(it))
                }

                val resultComponents = dao.getAllExamResultComponents()
                resultComponents.forEach {
                    tasks.add(rtdb.getReference("exam_result_components").child(it.id.toString()).setValue(it))
                }

                val payments = dao.getAllPayments().first()
                payments.forEach {
                    tasks.add(rtdb.getReference("fee_payments").child(it.id.toString()).setValue(it))
                }

                val rules = dao.getAllLateFeeRules()
                rules.forEach {
                    tasks.add(rtdb.getReference("late_fee_rules").child(it.id.toString()).setValue(it))
                }

                val profilePrefs = context.getSharedPreferences("vv_school_profile", Context.MODE_PRIVATE)
                val profileMap = mapOf(
                    "school_name" to profilePrefs.getString("school_name", "Vivekananda Vidyamandir"),
                    "contact_number" to profilePrefs.getString("contact_number", ""),
                    "email" to profilePrefs.getString("email", ""),
                    "address" to profilePrefs.getString("address", ""),
                    "admin_password" to profilePrefs.getString("admin_password", "India360@")
                )
                tasks.add(rtdb.getReference("school_profile").updateChildren(profileMap))

                // Block until all network write tasks are complete (or timeout after 15s)
                if (tasks.isNotEmpty()) {
                    Tasks.await(Tasks.whenAll(tasks), 15, TimeUnit.SECONDS)
                }

                withContext(Dispatchers.Main) {
                    onComplete?.invoke(true, "All ${tasks.size} records successfully saved to Firebase Cloud!")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sync to cloud failed: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(false, "Sync failed: ${e.message ?: "Network error"}")
                }
            }
        }
    }

    fun syncAllCloudToLocal(
        context: Context,
        scope: CoroutineScope,
        onComplete: ((Boolean, String) -> Unit)? = null
    ) {
        val rtdb = getDatabase()
        rtdb.reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                scope.launch(Dispatchers.IO) {
                    try {
                        val db = AppDatabase.getDatabase(context.applicationContext, scope)
                        val dao = db.schoolDao()

                        val classes = snapshot.child("classes").children.mapNotNull { it.getValue(SchoolClass::class.java) }
                        classes.forEach { dao.insertClass(it) }

                        val teachers = snapshot.child("teachers").children.mapNotNull { it.getValue(Teacher::class.java) }
                        teachers.forEach { dao.insertTeacher(it) }

                        val students = snapshot.child("students").children.mapNotNull { it.getValue(Student::class.java) }
                        students.forEach { dao.insertStudent(it) }

                        val exams = snapshot.child("exams").children.mapNotNull { it.getValue(ExamDefinition::class.java) }
                        exams.forEach { dao.insertExam(it) }

                        val subjects = snapshot.child("exam_subjects").children.mapNotNull { it.getValue(ExamSubject::class.java) }
                        if (subjects.isNotEmpty()) dao.insertExamSubjects(subjects)

                        val subComponents = snapshot.child("exam_subject_components").children.mapNotNull { it.getValue(ExamSubjectComponent::class.java) }
                        if (subComponents.isNotEmpty()) dao.insertExamSubjectComponents(subComponents)

                        val results = snapshot.child("exam_results").children.mapNotNull { it.getValue(ExamResult::class.java) }
                        if (results.isNotEmpty()) dao.insertResults(results)

                        val resComponents = snapshot.child("exam_result_components").children.mapNotNull { it.getValue(ExamResultComponent::class.java) }
                        if (resComponents.isNotEmpty()) dao.insertExamResultComponents(resComponents)

                        val payments = snapshot.child("fee_payments").children.mapNotNull { it.getValue(FeePayment::class.java) }
                        payments.forEach { dao.recordPayment(it) }

                        val rules = snapshot.child("late_fee_rules").children.mapNotNull { it.getValue(LateFeeRule::class.java) }
                        if (rules.isNotEmpty()) dao.replaceAllLateFeeRules(rules)

                        val profileSnap = snapshot.child("school_profile")
                        if (profileSnap.exists()) {
                            val prefs = context.getSharedPreferences("vv_school_profile", Context.MODE_PRIVATE)
                            val editor = prefs.edit()
                            profileSnap.children.forEach { child ->
                                val k = child.key ?: return@forEach
                                val v = child.value?.toString() ?: return@forEach
                                editor.putString(k, v)
                            }
                            editor.apply()
                        }

                        withContext(Dispatchers.Main) {
                            onComplete?.invoke(true, "Cloud records successfully synchronized to local database!")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Pull error: ${e.message}", e)
                        withContext(Dispatchers.Main) {
                            onComplete?.invoke(false, "Failed to pull records: ${e.message}")
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                scope.launch(Dispatchers.Main) {
                    onComplete?.invoke(false, "Firebase error: ${error.message}")
                }
            }
        })
    }

    fun syncStudent(student: Student) {
        try {
            getDatabase().getReference("students").child(student.id.toString()).setValue(student)
        } catch (_: Exception) {}
    }

    fun syncPayment(payment: FeePayment) {
        try {
            getDatabase().getReference("fee_payments").child(payment.id.toString()).setValue(payment)
        } catch (_: Exception) {}
    }

    fun syncClass(cls: SchoolClass) {
        try {
            getDatabase().getReference("classes").child(cls.id.toString()).setValue(cls)
        } catch (_: Exception) {}
    }

    fun syncTeacher(teacher: Teacher) {
        try {
            getDatabase().getReference("teachers").child(teacher.id.toString()).setValue(teacher)
        } catch (_: Exception) {}
    }
}
