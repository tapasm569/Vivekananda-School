package com.vidyamandir.schoolmanager.ui

import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.data.Student
import com.vidyamandir.schoolmanager.util.ImageHelper
import android.widget.ImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StudentProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(18, 12, 18, 18) }
        val toolbar = MaterialToolbar(this).apply { title = "Student Profile"; setNavigationIcon(android.R.drawable.ic_media_previous); setNavigationOnClickListener { finish() } }
        root.addView(toolbar)
        val card = MaterialCardView(this).apply { radius = 18f; cardElevation = 3f; setContentPadding(18, 18, 18, 18) }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
        val photo = ImageView(this).apply { layoutParams = LinearLayout.LayoutParams(112,112); scaleType = ImageView.ScaleType.CENTER_CROP }
        content.addView(photo)
        val details = TextView(this).apply { textSize = 16f; setTextColor(android.graphics.Color.BLACK); setPadding(4,18,4,4) }
        content.addView(details); card.addView(content)
        root.addView(card, LinearLayout.LayoutParams(-1,-2))
        setContentView(root)
        val id = intent.getLongExtra("STUDENT_ID", -1L)
        lifecycleScope.launch {
            val student = withContext(Dispatchers.IO) { AppDatabase.getDatabase(this@StudentProfileActivity, lifecycleScope).schoolDao().getStudentById(id) }
            if (student == null) { details.text = "Student record not found"; return@launch }
            ImageHelper.loadProfilePhoto(student.photoUri, photo, android.R.drawable.sym_def_app_icon)
            details.text = "${student.fullName}\nAdmission No: ${student.admissionNo}\nClass: ${student.displayClass}\nRoll: ${student.rollNo}\nFather: ${student.fatherName}\nMother: ${student.motherName}\nParent Phone: ${student.parentPhone}\nMonthly Fee: ₹${student.monthlyFee}\nAdmission Month: ${student.admissionMonth}\nAddress: ${student.address}"
        }
    }
}
