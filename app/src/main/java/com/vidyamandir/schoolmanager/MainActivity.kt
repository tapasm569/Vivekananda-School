package com.vidyamandir.schoolmanager

import android.content.Context
import java.util.Locale
import java.util.Date
import java.text.SimpleDateFormat
import java.io.FileInputStream
import java.io.File
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import com.vidyamandir.schoolmanager.util.ResultPdfGenerator
import com.vidyamandir.schoolmanager.util.SchoolDataBackup
import com.vidyamandir.schoolmanager.data.ExamDefinition
import com.vidyamandir.schoolmanager.data.Student
import com.vidyamandir.schoolmanager.data.AppDatabase
import androidx.lifecycle.lifecycleScope
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.view.ViewGroup
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.vidyamandir.schoolmanager.databinding.ActivityMainBinding
import com.vidyamandir.schoolmanager.ui.DashboardFragment
import com.vidyamandir.schoolmanager.ui.DueListFragment
import com.vidyamandir.schoolmanager.ui.LoginActivity
import com.vidyamandir.schoolmanager.ui.PaidListFragment
import com.vidyamandir.schoolmanager.ui.SearchFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var pendingMarksheetFile: File? = null

    private val backupLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) performBackup(uri)
    }

    private val restoreLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) confirmRestore(uri)
    }

    private val marksheetSaveLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) savePendingMarksheet(uri)
        else pendingMarksheetFile = null
    }


    companion object {
        private const val PROFILE_PREFS = "vv_school_profile"
        private const val DEFAULT_SCHOOL_NAME = "Vivekananda Vidyamandir"

        private const val MENU_PROFILE = 101
        private const val MENU_PENALTY = 102
        private const val MENU_BACKUP = 103
        private const val MENU_RESTORE = 104
        private const val MENU_MARKSHEET = 105
        private const val MENU_LOGOUT = 106
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updateToolbarSchoolName()
        setupToolbar()
        setupViewPagerWithBottomNav()
    }

    private fun profilePrefs() =
        getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE)

    private fun updateToolbarSchoolName() {
        binding.topAppBar.title = profilePrefs().getString(
            "school_name",
            DEFAULT_SCHOOL_NAME
        )
    }

    private fun setupToolbar() {
        binding.topAppBar.navigationIcon = null
        binding.topAppBar.menu.clear()

        val menuItem = binding.topAppBar.menu.add(
            0, MENU_LOGOUT, 0, "Menu"
        )

        menuItem.setIcon(R.drawable.ic_menu_lines)
        menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

        menuItem.setOnMenuItemClickListener {
            showMainMenu()
            true
        }
    }

    private fun showMainMenu() {
        val schoolName = profilePrefs().getString(
            "school_name",
            DEFAULT_SCHOOL_NAME
        ) ?: DEFAULT_SCHOOL_NAME

        val popup = PopupMenu(this, binding.topAppBar, Gravity.END)

        popup.menu.add(0, MENU_PROFILE, 0, schoolName)
        popup.menu.add(0, MENU_PENALTY, 1, "Set Penalty")
        popup.menu.add(0, MENU_BACKUP, 2, "BackUp Data")
        popup.menu.add(0, MENU_RESTORE, 3, "Restore Data")
        popup.menu.add(0, MENU_MARKSHEET, 4, "Export Marksheet")
        popup.menu.add(0, MENU_LOGOUT, 5, "Logout")

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_PROFILE -> {
                    showSchoolProfileDialog()
                    true
                }

                MENU_PENALTY -> {
                    showPenaltyDialog()
                    true
                }

                MENU_BACKUP -> {
                    startBackup()
                    true
                }

                MENU_RESTORE -> {
                    startRestore()
                    true
                }

                MENU_MARKSHEET -> {
                    startMarksheetExport()
                    true
                }

                MENU_LOGOUT -> {
                    logout()
                    true
                }

                else -> false
            }
        }

        popup.show()
    }

    private fun showSchoolProfileDialog() {
        val prefs = profilePrefs()

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(8))
        }

        val scrollView = ScrollView(this)
        scrollView.addView(container)

        fun addField(
            label: String,
            value: String,
            inputType: Int = InputType.TYPE_CLASS_TEXT
        ): EditText {
            val heading = TextView(this).apply {
                text = label
                textSize = 13f
                setPadding(0, dp(10), 0, dp(4))
            }

            val field = EditText(this).apply {
                setSingleLine(true)
                setText(value)
                hint = label
                this.inputType = inputType
                textSize = 15f
                setPadding(dp(12), dp(10), dp(12), dp(10))
            }

            container.addView(heading)
            container.addView(
                field,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(52)
                )
            )

            return field
        }

        val schoolName = addField(
            "School Name",
            prefs.getString("school_name", DEFAULT_SCHOOL_NAME)
                ?: DEFAULT_SCHOOL_NAME
        )

        val contact = addField(
            "Contact Number",
            prefs.getString("contact_number", "") ?: ""
        )

        val email = addField(
            "E-mail ID",
            prefs.getString("email", "") ?: "",
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        )

        val address = addField(
            "Address",
            prefs.getString("address", "") ?: "",
            InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE
        )

        val password = addField(
            "New Admin Password",
            "",
            InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_PASSWORD
        )
        password.hint = "Leave blank to keep current password"

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("School Profile")
            .setView(scrollView)
            .setPositiveButton("Save Changes", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {
                    val name = schoolName.text.toString().trim()
                    val newPassword = password.text.toString().trim()

                    if (name.isBlank()) {
                        schoolName.error = "School name is required"
                        return@setOnClickListener
                    }

                    if (newPassword.isNotEmpty() &&
                        newPassword.length < 4
                    ) {
                        password.error = "Use at least 4 characters"
                        return@setOnClickListener
                    }

                    val editor = prefs.edit()
                        .putString("school_name", name)
                        .putString(
                            "contact_number",
                            contact.text.toString().trim()
                        )
                        .putString(
                            "email",
                            email.text.toString().trim()
                        )
                        .putString(
                            "address",
                            address.text.toString().trim()
                        )

                    if (newPassword.isNotEmpty()) {
                        editor.putString("admin_password", newPassword)
                    }

                    editor.apply()

                    updateToolbarSchoolName()

                    Toast.makeText(
                        this,
                        "School profile updated successfully",
                        Toast.LENGTH_SHORT
                    ).show()

                    dialog.dismiss()
                }
        }

        dialog.show()
    }

    private fun showPenaltyDialog() {
        val prefs = profilePrefs()

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(8))
        }

        val graceInput = EditText(this).apply {
            hint = "Grace days after month-end"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(
                prefs.getInt("penalty_grace_days", 10).toString()
            )
        }

        val amountInput = EditText(this).apply {
            hint = "Penalty amount (₹)"
            inputType =
                android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(
                prefs.getFloat("penalty_amount", 50f).toString()
            )
        }

        container.addView(graceInput)
        container.addView(amountInput)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Late Fee Penalty Settings")
            .setMessage(
                "Set how many days after each month's end " +
                "the penalty should apply."
            )
            .setView(container)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {
                    val grace = graceInput.text.toString()
                        .toIntOrNull()
                    val amount = amountInput.text.toString()
                        .toFloatOrNull()

                    if (grace == null || grace < 0) {
                        graceInput.error = "Enter valid grace days"
                        return@setOnClickListener
                    }

                    if (amount == null || amount < 0f) {
                        amountInput.error = "Enter valid penalty amount"
                        return@setOnClickListener
                    }

                    prefs.edit()
                        .putInt("penalty_grace_days", grace)
                        .putFloat("penalty_amount", amount)
                        .apply()

                    Toast.makeText(
                        this,
                        "Penalty settings saved",
                        Toast.LENGTH_SHORT
                    ).show()

                    dialog.dismiss()
                }
        }

        dialog.show()
    }


    private fun startBackup() {
        val timestamp = SimpleDateFormat(
            "yyyyMMdd_HHmm", Locale.getDefault()
        ).format(Date())

        backupLauncher.launch(
            "Vivekananda_School_Backup_$timestamp.zip"
        )
    }

    private fun performBackup(uri: Uri) {
        lifecycleScope.launch {
            Toast.makeText(
                this@MainActivity,
                "Creating school backup...",
                Toast.LENGTH_SHORT
            ).show()

            try {
                withContext(Dispatchers.IO) {
                    SchoolDataBackup.createBackup(this@MainActivity, uri)
                }

                Toast.makeText(
                    this@MainActivity,
                    "School backup created successfully",
                    Toast.LENGTH_LONG
                ).show()
            } catch (error: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Backup failed: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun startRestore() {
        restoreLauncher.launch(
            arrayOf("application/zip", "application/octet-stream")
        )
    }

    private fun confirmRestore(uri: Uri) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Restore School Data?")
            .setMessage(
                "Restoring this backup will replace the current " +
                "students, fees, exams, results and school settings. " +
                "The current database will be replaced. Continue?"
            )
            .setPositiveButton("Restore") { _, _ ->
                performRestore(uri)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performRestore(uri: Uri) {
        lifecycleScope.launch {
            Toast.makeText(
                this@MainActivity,
                "Restoring school data...",
                Toast.LENGTH_LONG
            ).show()

            try {
                withContext(Dispatchers.IO) {
                    SchoolDataBackup.restoreBackup(this@MainActivity, uri)
                }

                Toast.makeText(
                    applicationContext,
                    "Restore successful. Please log in again.",
                    Toast.LENGTH_LONG
                ).show()

                val intent = Intent(
                    this@MainActivity,
                    LoginActivity::class.java
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                }

                startActivity(intent)
                finish()

            } catch (error: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Restore failed: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()

                // Reopen the database if restoration closed it.
                AppDatabase.getDatabase(
                    applicationContext, lifecycleScope
                )

                // Recreate the screen so fragments use the reopened database.
                val intent = Intent(
                    this@MainActivity,
                    MainActivity::class.java
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                }

                startActivity(intent)
                finish()
            }
        }
    }

    private fun startMarksheetExport() {
        lifecycleScope.launch {
            try {
                val data = withContext(Dispatchers.IO) {
                    val db = AppDatabase.getDatabase(
                        this@MainActivity, lifecycleScope
                    )

                    Pair(
                        db.schoolDao().getAllActiveStudents().first(),
                        db.schoolDao().getAllExams().first()
                    )
                }

                val students = data.first
                val exams = data.second

                // Keep sections separate in the export class picker.
                val classes = students
                    .map { it.displayClass }
                    .distinct()
                    .sorted()

                if (classes.isEmpty()) {
                    Toast.makeText(
                        this@MainActivity,
                        "No active students found",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle("Select Class")
                    .setItems(classes.toTypedArray()) { _, position ->
                        val selectedClass = classes[position]

                        val selectedStudents = students.filter {
                            it.displayClass.equals(
                                selectedClass, ignoreCase = true
                            )
                        }

                        // An exam may target either the displayed section
                        // or the parent class containing that section.
                        val parentClassNames = selectedStudents
                            .map { it.className }
                            .distinct()

                        val classExams = exams.filter { exam ->
                            exam.targetClass.equals(
                                selectedClass, ignoreCase = true
                            ) || parentClassNames.any { parent ->
                                exam.targetClass.equals(
                                    parent, ignoreCase = true
                                )
                            }
                        }

                        if (classExams.isEmpty()) {
                            Toast.makeText(
                                this@MainActivity,
                                "No exams found for $selectedClass",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            showExamSelection(
                                selectedClass,
                                students,
                                classExams
                            )
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()

            } catch (error: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Could not load classes: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showExamSelection(
        selectedClass: String,
        allStudents: List<Student>,
        exams: List<ExamDefinition>
    ) {
        val examNames = exams.map {
            "${it.examName} (${it.academicYear})"
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Select Exam - $selectedClass")
            .setItems(examNames.toTypedArray()) { _, position ->
                generateClassMarksheets(
                    selectedClass,
                    allStudents,
                    exams[position]
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun generateClassMarksheets(
        selectedClass: String,
        allStudents: List<Student>,
        exam: ExamDefinition
    ) {
        lifecycleScope.launch {
            try {
                Toast.makeText(
                    this@MainActivity,
                    "Preparing approved marksheets...",
                    Toast.LENGTH_SHORT
                ).show()

                val outputFile = withContext(Dispatchers.IO) {
                    val db = AppDatabase.getDatabase(
                        this@MainActivity, lifecycleScope
                    )

                    val classStudents = allStudents.filter {
                        it.displayClass.equals(
                            selectedClass, ignoreCase = true
                        )
                    }.sortedBy { it.rollNo }

                    val resultsByStudent =
                        mutableMapOf<Long, List<com.vidyamandir.schoolmanager.data.ExamResult>>()

                    for (student in classStudents) {
                        val approved = db.schoolDao()
                            .getStudentExamResults(
                                student.id, exam.examName
                            )
                            .filter {
                                it.approvalStatus.equals(
                                    "APPROVED", ignoreCase = true
                                ) && it.academicYear == exam.academicYear
                            }

                        if (approved.isNotEmpty()) {
                            resultsByStudent[student.id] = approved
                        }
                    }

                    val file = File(
                        cacheDir,
                        "Class_Marksheets.pdf"
                    )

                    ResultPdfGenerator(this@MainActivity)
                        .generateClassReportCards(
                            classStudents,
                            exam.examName,
                            resultsByStudent,
                            file
                        )

                    file
                }

                pendingMarksheetFile = outputFile

                val safeClass = selectedClass.replace(
                    Regex("[^A-Za-z0-9_-]"), "_"
                )
                val safeExam = exam.examName.replace(
                    Regex("[^A-Za-z0-9_-]"), "_"
                )

                marksheetSaveLauncher.launch(
                    "${safeClass}_${safeExam}_Marksheets.pdf"
                )

            } catch (error: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Marksheet export failed: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun savePendingMarksheet(uri: Uri) {
        val source = pendingMarksheetFile ?: return
        pendingMarksheetFile = null

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(uri)?.use { output ->
                        FileInputStream(source).use { input ->
                            input.copyTo(output)
                        }
                    } ?: error("Cannot open selected PDF destination")
                }

                Toast.makeText(
                    this@MainActivity,
                    "Class marksheets exported successfully",
                    Toast.LENGTH_LONG
                ).show()

            } catch (error: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "PDF save failed: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun logout() {
        val prefs = getSharedPreferences(
            "vv_auth_session",
            Context.MODE_PRIVATE
        )

        prefs.edit().clear().apply()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun setupViewPagerWithBottomNav() {
        val pagerAdapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 4

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> DueListFragment()
                    1 -> PaidListFragment()
                    2 -> SearchFragment()
                    3 -> DashboardFragment()
                    else -> DashboardFragment()
                }
            }
        }

        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.offscreenPageLimit = 3

        binding.viewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    when (position) {
                        0 -> binding.bottomNav.selectedItemId = R.id.nav_due
                        1 -> binding.bottomNav.selectedItemId = R.id.nav_paid
                        2 -> binding.bottomNav.selectedItemId = R.id.nav_search
                        3 -> binding.bottomNav.selectedItemId = R.id.nav_dashboard
                    }
                }
            }
        )

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_due -> {
                    binding.viewPager.setCurrentItem(0, true)
                    true
                }
                R.id.nav_paid -> {
                    binding.viewPager.setCurrentItem(1, true)
                    true
                }
                R.id.nav_search -> {
                    binding.viewPager.setCurrentItem(2, true)
                    true
                }
                R.id.nav_dashboard -> {
                    binding.viewPager.setCurrentItem(3, true)
                    true
                }
                else -> false
            }
        }
    }
}
