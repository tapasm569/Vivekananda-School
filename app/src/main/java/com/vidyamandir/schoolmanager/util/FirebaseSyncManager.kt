package com.vidyamandir.schoolmanager.util

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*
import com.vidyamandir.schoolmanager.data.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object FirebaseSyncManager {
    private const val TAG = "FirebaseSyncManager"
    const val DATABASE_URL = "https://vidyamandir-a3340-default-rtdb.firebaseio.com"

    private var initialized = false
    private var isListening = false

    fun getDatabase(): FirebaseDatabase {
        return try {
            FirebaseDatabase.getInstance(DATABASE_URL)
        } catch (_: Exception) {
            FirebaseDatabase.getInstance()
        }
    }

    fun init(context: Context) {
        if (initialized) return
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            val db = getDatabase()
            try {
                db.setPersistenceEnabled(true)
            } catch (e: Exception) {
                Log.d(TAG, "Persistence already enabled: ${e.message}")
            }

            listOf(
                "classes", "teachers", "students", "exams", "exam_subjects",
                "exam_subject_components", "exam_results", "exam_result_components",
                "fee_payments", "late_fee_rules", "school_profile"
            ).forEach { path ->
                db.getReference(path).keepSynced(true)
            }

            initialized = true
            startRealtimeListener(context)
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization error: ${e.message}", e)
        }
    }

    fun startRealtimeListener(context: Context) {
        if (isListening) return
        isListening = true

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
                            try {
                                onData(list)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error saving synced $path: ${e.message}")
                            }
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
                Log.w(TAG, "Listener cancelled for school_profile: ${error.message}")
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

                val classes = dao.getAllClasses().first()
                classes.forEach { rtdb.getReference("classes").child(it.id.toString()).setValue(it) }

                val teachers = dao.getAllTeachers().first()
                teachers.forEach { rtdb.getReference("teachers").child(it.id.toString()).setValue(it) }

                val students = dao.getAllActiveStudents().first()
                students.forEach { rtdb.getReference("students").child(it.id.toString()).setValue(it) }

                val exams = dao.getAllExams().first()
                exams.forEach { exam ->
                    rtdb.getReference("exams").child(exam.id.toString()).setValue(exam)
                }

                val subjects = dao.getAllExamSubjects()
                subjects.forEach { rtdb.getReference("exam_subjects").child(it.id.toString()).setValue(it) }

                val subjectComponents = dao.getAllExamSubjectComponents()
                subjectComponents.forEach { rtdb.getReference("exam_subject_components").child(it.id.toString()).setValue(it) }

                val results = dao.getAllExamResults()
                results.forEach { rtdb.getReference("exam_results").child(it.id.toString()).setValue(it) }

                val resultComponents = dao.getAllExamResultComponents()
                resultComponents.forEach { rtdb.getReference("exam_result_components").child(it.id.toString()).setValue(it) }

                val payments = dao.getAllPayments().first()
                payments.forEach { rtdb.getReference("fee_payments").child(it.id.toString()).setValue(it) }

                val rules = dao.getAllLateFeeRules()
                rules.forEach { rtdb.getReference("late_fee_rules").child(it.id.toString()).setValue(it) }

                val profilePrefs = context.getSharedPreferences("vv_school_profile", Context.MODE_PRIVATE)
                val profileMap = mapOf(
                    "school_name" to profilePrefs.getString("school_name", "Vivekananda Vidyamandir"),
                    "contact_number" to profilePrefs.getString("contact_number", ""),
                    "email" to profilePrefs.getString("email", ""),
                    "address" to profilePrefs.getString("address", ""),
                    "admin_password" to profilePrefs.getString("admin_password", "India360@")
                )
                rtdb.getReference("school_profile").updateChildren(profileMap)

                withContext(Dispatchers.Main) {
                    onComplete?.invoke(true, "All school records backed up to Firebase Realtime Database!")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sync to cloud failed: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(false, "Sync failed: ${e.message}")
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
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload student: ${e.message}")
        }
    }

    fun syncPayment(payment: FeePayment) {
        try {
            getDatabase().getReference("fee_payments").child(payment.id.toString()).setValue(payment)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload payment: ${e.message}")
        }
    }

    fun syncClass(cls: SchoolClass) {
        try {
            getDatabase().getReference("classes").child(cls.id.toString()).setValue(cls)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload class: ${e.message}")
        }
    }

    fun syncTeacher(teacher: Teacher) {
        try {
            getDatabase().getReference("teachers").child(teacher.id.toString()).setValue(teacher)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload teacher: ${e.message}")
        }
    }
}
