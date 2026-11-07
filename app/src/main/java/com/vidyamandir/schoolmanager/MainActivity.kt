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
import com.vidyamandir.schoolmanager.data.LateFeeRule
import androidx.lifecycle.lifecycleScope
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.view.ViewGroup
import android.view.View
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
        val dialog = android.app.Dialog(this)
        val navy = android.graphics.Color.parseColor("#0F3D6E")
        val amber = android.graphics.Color.parseColor("#D97706")
        val dark = android.graphics.Color.parseColor("#172B4D")
        val muted = android.graphics.Color.parseColor("#718096")

        fun rounded(color: Int, radius: Int): android.graphics.drawable.GradientDrawable {
            return android.graphics.drawable.GradientDrawable().apply {
                setColor(color)
                cornerRadius = dp(radius).toFloat()
            }
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(24))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.parseColor("#F5F7FB"))
                cornerRadii = floatArrayOf(
                    dp(26).toFloat(), dp(26).toFloat(),
                    dp(26).toFloat(), dp(26).toFloat(),
                    0f, 0f, 0f, 0f
                )
            }
        }

        val handle = View(this).apply {
            background = rounded(android.graphics.Color.parseColor("#CBD5E1"), 10)
        }
        root.addView(handle, LinearLayout.LayoutParams(dp(42), dp(5)).apply {
            gravity = Gravity.CENTER
            bottomMargin = dp(18)
        })

        val schoolName = profilePrefs().getString(
            "school_name", DEFAULT_SCHOOL_NAME
        ) ?: DEFAULT_SCHOOL_NAME

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(18), dp(12), dp(18))
            background = rounded(navy, 20)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                dialog.dismiss()
                showSchoolProfileDialog()
            }
        }

        val schoolIcon = TextView(this).apply {
            text = "🏫"
            textSize = 27f
            gravity = Gravity.CENTER
            background = rounded(android.graphics.Color.parseColor("#FFFFFF"), 14)
        }
        header.addView(schoolIcon, LinearLayout.LayoutParams(dp(54), dp(54)))

        val heading = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, dp(6), 0)
        }

        heading.addView(TextView(this).apply {
            text = schoolName
            textSize = 17f
            maxLines = 2
            setTextColor(android.graphics.Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
        })

        heading.addView(TextView(this).apply {
            text = "SCHOOL ADMINISTRATION  •  EDIT PROFILE"
            textSize = 9f
            setTextColor(android.graphics.Color.parseColor("#D7E7F8"))
            setPadding(0, dp(5), 0, 0)
        })

        header.addView(heading, LinearLayout.LayoutParams(0, -2, 1f))

        header.addView(TextView(this).apply {
            text = "✎"
            textSize = 23f
            setTextColor(android.graphics.Color.WHITE)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(dp(35), dp(45)))

        root.addView(header, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(20)
        })

        root.addView(TextView(this).apply {
            text = "QUICK SETTINGS"
            textSize = 11f
            letterSpacing = 1.2f
            setTextColor(muted)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(12)
        })

        val grid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val penalty = createMenuCard(
            "⚙", "Set Penalty", "Late fee settings",
            amber
        ) {
            dialog.dismiss()
            showPenaltyDialog()
        }

        val backup = createMenuCard(
            "☁", "BackUp Data", "Save school records",
            navy
        ) {
            dialog.dismiss()
            startBackup()
        }

        val restore = createMenuCard(
            "↻", "Restore Data", "Recover saved records",
            android.graphics.Color.parseColor("#0D9488")
        ) {
            dialog.dismiss()
            startRestore()
        }

        val marksheet = createMenuCard(
            "▤", "Export Marksheet", "Create class PDF",
            android.graphics.Color.parseColor("#7C3AED")
        ) {
            dialog.dismiss()
            startMarksheetExport()
        }

        addMenuCardRow(grid, penalty, backup)
        addMenuCardRow(grid, restore, marksheet)
        root.addView(grid)

        val logout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = rounded(android.graphics.Color.parseColor("#FEECEC"), 15)
            setPadding(dp(12), dp(14), dp(12), dp(14))
            isClickable = true
            isFocusable = true
            setOnClickListener {
                dialog.dismiss()
                logout()
            }
        }

        logout.addView(TextView(this).apply {
            text = "⇥"
            textSize = 22f
            setTextColor(android.graphics.Color.parseColor("#DC2626"))
        })

        logout.addView(TextView(this).apply {
            text = "Logout"
            textSize = 15f
            setTextColor(android.graphics.Color.parseColor("#DC2626"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(dp(10), 0, 0, 0)
        })

        root.addView(logout, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(18)
        })

        dialog.setContentView(root)
        dialog.setCanceledOnTouchOutside(true)
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setDimAmount(0.48f)
            addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            attributes = attributes.apply {
                gravity = Gravity.BOTTOM
            }
        }

        dialog.show()
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setGravity(Gravity.BOTTOM)
    }

    private fun createMenuCard(
        icon: String,
        title: String,
        subtitle: String,
        accent: Int,
        action: () -> Unit
    ): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(13), dp(15), dp(10), dp(14))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.WHITE)
                cornerRadius = dp(17).toFloat()
                setStroke(dp(1), android.graphics.Color.parseColor("#E7ECF3"))
            }
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }

        card.addView(TextView(this).apply {
            text = icon
            textSize = 23f
            gravity = Gravity.CENTER
            setTextColor(accent)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(accent and 0x20FFFFFF)
                cornerRadius = dp(12).toFloat()
            }
        }, LinearLayout.LayoutParams(dp(43), dp(43)).apply {
            bottomMargin = dp(11)
        })

        card.addView(TextView(this).apply {
            text = title
            textSize = 14f
            maxLines = 2
            setTextColor(android.graphics.Color.parseColor("#172B4D"))
            setTypeface(null, android.graphics.Typeface.BOLD)
        })

        card.addView(TextView(this).apply {
            text = subtitle
            textSize = 10f
            setTextColor(android.graphics.Color.parseColor("#718096"))
            setPadding(0, dp(5), 0, 0)
        })

        return card
    }

    private fun addMenuCardRow(
        parent: LinearLayout,
        first: View,
        second: View
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        row.addView(first, LinearLayout.LayoutParams(0, dp(142), 1f).apply {
            rightMargin = dp(6)
            bottomMargin = dp(12)
        })

        row.addView(second, LinearLayout.LayoutParams(0, dp(142), 1f).apply {
            leftMargin = dp(6)
            bottomMargin = dp(12)
        })

        parent.addView(row)
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
        lifecycleScope.launch {
            try {
                val database = AppDatabase.getDatabase(
                    this@MainActivity, lifecycleScope
                )

                var rules = withContext(Dispatchers.IO) {
                    database.schoolDao().getAllLateFeeRules()
                }

                val isOldDefault =
                    rules.size == 3 &&
                    rules[0].dayFrom == 1 && rules[0].dayTo == 10 &&
                    rules[0].fineAmount == 0.0 &&
                    rules[1].dayFrom == 11 && rules[1].dayTo == 20 &&
                    rules[1].fineAmount == 50.0 &&
                    rules[2].dayFrom == 21 && rules[2].dayTo == 30 &&
                    rules[2].fineAmount == 100.0

                if (rules.isEmpty() || isOldDefault) {
                    rules = listOf(
                        LateFeeRule(dayFrom = 1, dayTo = 4, fineAmount = 0.0),
                        LateFeeRule(dayFrom = 5, dayTo = 9, fineAmount = 20.0),
                        LateFeeRule(dayFrom = 10, dayTo = 19, fineAmount = 40.0),
                        LateFeeRule(dayFrom = 20, dayTo = 29, fineAmount = 60.0),
                        LateFeeRule(dayFrom = 30, dayTo = 9999, fineAmount = 100.0)
                    )
                }

                val container = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(16), dp(8), dp(16), dp(8))
                }

                val explanation = TextView(this@MainActivity).apply {
                    text = "Penalty is calculated separately for each unpaid month. " +
                        "Counting starts on the 1st of the following month."
                    textSize = 13f
                    setPadding(0, 0, 0, dp(12))
                }
                container.addView(explanation)

                val header = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                }

                listOf("From", "To", "Penalty ₹", "").forEach {
                    val label = TextView(this@MainActivity).apply {
                        text = it
                        textSize = 12f
                        setPadding(dp(3), dp(5), dp(3), dp(5))
                    }
                    header.addView(
                        label,
                        LinearLayout.LayoutParams(0, dp(35), 1f)
                    )
                }
                container.addView(header)

                val rowsContainer = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                }

                val rows = mutableListOf<List<EditText>>()

                fun addSlabRow(from: String, to: String, amount: String) {
                    val row = LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                    }

                    fun numberField(value: String, hintText: String): EditText {
                        return EditText(this@MainActivity).apply {
                            hint = hintText
                            setText(value)
                            textSize = 14f
                            inputType = InputType.TYPE_CLASS_NUMBER or
                                InputType.TYPE_NUMBER_FLAG_DECIMAL
                            setSingleLine(true)
                            setPadding(dp(5), 0, dp(5), 0)
                        }
                    }

                    val fromInput = numberField(from, "1")
                    val toInput = numberField(to, "4")
                    val amountInput = numberField(amount, "0")

                    row.addView(fromInput, LinearLayout.LayoutParams(0, dp(48), 1f))
                    row.addView(toInput, LinearLayout.LayoutParams(0, dp(48), 1f))
                    row.addView(amountInput, LinearLayout.LayoutParams(0, dp(48), 1f))

                    val deleteButton = Button(this@MainActivity).apply {
                        text = "×"
                        textSize = 18f
                        minWidth = dp(42)
                        setPadding(0, 0, 0, 0)
                    }

                    val fields = listOf(fromInput, toInput, amountInput)
                    rows.add(fields)

                    deleteButton.setOnClickListener {
                        rows.remove(fields)
                        rowsContainer.removeView(row)
                    }

                    row.addView(
                        deleteButton,
                        LinearLayout.LayoutParams(dp(42), dp(44))
                    )

                    rowsContainer.addView(row)
                }

                rules.sortedBy { it.dayFrom }.forEach {
                    addSlabRow(
                        it.dayFrom.toString(),
                        if (it.dayTo >= 9999) "" else it.dayTo.toString(),
                        it.fineAmount.toString()
                    )
                }

                val scroll = ScrollView(this@MainActivity).apply {
                    addView(rowsContainer)
                    isFillViewport = true
                }

                container.addView(
                    scroll,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(260)
                    )
                )

                val addButton = Button(this@MainActivity).apply {
                    text = "+ Add Penalty Slab"
                    setOnClickListener {
                        addSlabRow("", "", "")
                    }
                }
                container.addView(addButton)

                val dialog = MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle("Day-wise Penalty Slabs")
                    .setView(container)
                    .setPositiveButton("Save", null)
                    .setNegativeButton("Cancel", null)
                    .create()

                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener {
                            if (rows.isEmpty()) {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Add at least one penalty slab",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@setOnClickListener
                            }

                            val parsed = mutableListOf<LateFeeRule>()

                            for (fields in rows) {
                                val from = fields[0].text.toString().toIntOrNull()
                                val toText = fields[1].text.toString().trim()
                                val to = if (toText.isEmpty()) 9999
                                    else toText.toIntOrNull()
                                val amount = fields[2].text.toString().toDoubleOrNull()

                                if (from == null || from < 1 ||
                                    to == null || to < from ||
                                    amount == null || amount < 0.0
                                ) {
                                    fields[0].error = "Check slab values"
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Enter valid day ranges and penalty amounts",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    return@setOnClickListener
                                }

                                parsed.add(
                                    LateFeeRule(
                                        dayFrom = from,
                                        dayTo = to,
                                        fineAmount = amount
                                    )
                                )
                            }

                            val sorted = parsed.sortedBy { it.dayFrom }

                            for (i in 1 until sorted.size) {
                                if (sorted[i].dayFrom <= sorted[i - 1].dayTo) {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Penalty day ranges must not overlap",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    return@setOnClickListener
                                }
                            }

                            lifecycleScope.launch {
                                try {
                                    withContext(Dispatchers.IO) {
                                        database.schoolDao().replaceAllLateFeeRules(sorted)
                                    }

                                    Toast.makeText(
                                        this@MainActivity,
                                        "Penalty slabs saved successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    dialog.dismiss()
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Could not save slabs: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                }

                dialog.show()

            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Unable to open penalty settings: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
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
