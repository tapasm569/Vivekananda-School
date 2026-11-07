package com.vidyamandir.schoolmanager.ui

import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.*
import android.text.InputType
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.data.Student
import com.vidyamandir.schoolmanager.util.ImageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StudentProfileActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private var student: Student? = null
    private val fields = mutableMapOf<String, EditText>()
    private var editing = false
    private lateinit var editButton: Button
    private lateinit var photo: ImageView
    private lateinit var status: TextView

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = AppDatabase.getDatabase(this, lifecycleScope)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(245, 247, 251))
        }

        val toolbar = MaterialToolbar(this).apply {
            title = "Student Profile"
            setTitleTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(22, 58, 105))
            setNavigationIcon(android.R.drawable.ic_media_previous)
            setNavigationOnClickListener { finish() }
        }
        root.addView(toolbar)

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(24))
        }

        val header = MaterialCardView(this).apply {
            radius = dp(18).toFloat()
            cardElevation = dp(3).toFloat()
            setCardBackgroundColor(Color.WHITE)
            setContentPadding(dp(18), dp(18), dp(18), dp(18))
        }

        val headerContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        photo = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(100), dp(100))
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        headerContent.addView(photo)

        status = TextView(this).apply {
            text = "STUDENT DETAILS"
            textSize = 18f
            setTextColor(Color.rgb(22, 58, 105))
            setPadding(0, dp(12), 0, dp(4))
            gravity = Gravity.CENTER
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        headerContent.addView(status)
        header.addView(headerContent)
        content.addView(header)

        val detailsCard = MaterialCardView(this).apply {
            radius = dp(16).toFloat()
            cardElevation = dp(2).toFloat()
            setCardBackgroundColor(Color.WHITE)
            setContentPadding(dp(16), dp(12), dp(16), dp(16))
        }

        val detailLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        fun addField(key: String, label: String, inputType: Int = InputType.TYPE_CLASS_TEXT) {
            val title = TextView(this).apply {
                text = label
                textSize = 12f
                setTextColor(Color.rgb(100, 116, 139))
                setPadding(0, dp(10), 0, dp(3))
            }
            detailLayout.addView(title)

            val input = EditText(this).apply {
                textSize = 15f
                setTextColor(Color.rgb(30, 41, 59))
                setSingleLine(key != "address")
                this.inputType = inputType
                isEnabled = false
                setPadding(dp(10), dp(8), dp(10), dp(8))
                setBackgroundColor(Color.rgb(245, 247, 251))
            }
            fields[key] = input
            detailLayout.addView(input, LinearLayout.LayoutParams(-1, -2))
        }

        addField("admissionNo", "Admission Number")
        addField("name", "Student Name")
        addField("className", "Class")
        addField("section", "Section")
        addField("roll", "Roll Number", InputType.TYPE_CLASS_NUMBER)
        addField("father", "Father's Name")
        addField("mother", "Mother's Name")
        addField("phone", "Parent Phone", InputType.TYPE_CLASS_PHONE)
        addField("address", "Address", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE)
        addField("fee", "Monthly Fee", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
        addField("month", "Admission Month", InputType.TYPE_CLASS_NUMBER)

        detailsCard.addView(detailLayout)
        content.addView(detailsCard, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(14)
        })

        editButton = Button(this).apply {
            text = "Update Student Details"
            setOnClickListener {
                if (editing) saveChanges() else setEditing(true)
            }
        }
        content.addView(editButton, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(16)
        })

        val receiptButton = Button(this).apply {
            text = "View Payment Receipts"
            setOnClickListener {
                student?.let {
                    startActivity(android.content.Intent(
                        this@StudentProfileActivity,
                        StudentReceiptHistoryActivity::class.java
                    ).putExtra("STUDENT_ID", it.id))
                }
            }
        }
        content.addView(receiptButton)

        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        loadStudent()
    }

    private fun loadStudent() {
        val id = intent.getLongExtra("STUDENT_ID", -1L)
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                db.schoolDao().getStudentById(id)
            }

            if (result == null) {
                Toast.makeText(this@StudentProfileActivity,
                    "Student record not found", Toast.LENGTH_LONG).show()
                finish()
                return@launch
            }

            student = result
            ImageHelper.loadProfilePhoto(
                result.photoUri, photo, android.R.drawable.sym_def_app_icon
            )

            fields["admissionNo"]?.setText(result.admissionNo)
            fields["name"]?.setText(result.fullName)
            fields["className"]?.setText(result.className)
            fields["section"]?.setText(result.section)
            fields["roll"]?.setText(result.rollNo.toString())
            fields["father"]?.setText(result.fatherName)
            fields["mother"]?.setText(result.motherName)
            fields["phone"]?.setText(result.parentPhone)
            fields["address"]?.setText(result.address)
            fields["fee"]?.setText(result.monthlyFee.toString())
            fields["month"]?.setText(result.admissionMonth.toString())
        }
    }

    private fun setEditing(enabled: Boolean) {
        editing = enabled
        fields.forEach { (key, field) ->
            field.isEnabled = enabled && key != "admissionNo"
            field.setBackgroundColor(
                if (field.isEnabled) Color.rgb(239, 246, 255)
                else Color.rgb(245, 247, 251)
            )
        }
        editButton.text = if (enabled) "Save Changes" else "Update Student Details"
        status.text = if (enabled) "EDIT STUDENT DETAILS" else "STUDENT DETAILS"
    }

    private fun saveChanges() {
        val old = student ?: return

        fun value(key: String) = fields[key]?.text?.toString()?.trim().orEmpty()

        val name = value("name")
        val phone = value("phone")
        val fee = value("fee").toDoubleOrNull()
        val roll = value("roll").toIntOrNull()
        val month = value("month").toIntOrNull()

        if (name.isBlank() || phone.isBlank() || fee == null ||
            fee < 0 || roll == null || month == null || month !in 1..12) {
            Toast.makeText(this, "Please check the entered details", Toast.LENGTH_LONG).show()
            return
        }

        val updated = old.copy(
            fullName = name,
            className = value("className"),
            section = value("section"),
            rollNo = roll,
            fatherName = value("father"),
            motherName = value("mother"),
            parentPhone = phone,
            address = value("address"),
            monthlyFee = fee,
            admissionMonth = month
        )

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    db.schoolDao().updateStudent(updated)
                }
                student = updated
                setEditing(false)
                Toast.makeText(this@StudentProfileActivity,
                    "Student details updated successfully", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this@StudentProfileActivity,
                    "Update failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
