#!/usr/bin/env bash
set -e

# 1. Update MainActivity.kt (Only controls ViewPager2 swiping and Footer sync)
cat << 'FILE_EOF' > app/src/main/java/com/vidyamandir/schoolmanager/MainActivity.kt
package com.vidyamandir.schoolmanager

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.vidyamandir.schoolmanager.databinding.ActivityMainBinding
import com.vidyamandir.schoolmanager.ui.DashboardFragment
import com.vidyamandir.schoolmanager.ui.DueListFragment
import com.vidyamandir.schoolmanager.ui.LoginActivity
import com.vidyamandir.schoolmanager.ui.PaidListFragment
import com.vidyamandir.schoolmanager.ui.SearchFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupViewPagerWithBottomNav()
    }

    private fun setupToolbar() {
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_logout -> {
                    val prefs = getSharedPreferences("vv_auth_session", Context.MODE_PRIVATE)
                    prefs.edit().clear().apply()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupViewPagerWithBottomNav() {
        // Tab 0: Due List, Tab 1: Paid List, Tab 2: Search, Tab 3: Dashboard
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

        // Swiping Left/Right updates Footer Icon
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                when (position) {
                    0 -> binding.bottomNav.selectedItemId = R.id.nav_due
                    1 -> binding.bottomNav.selectedItemId = R.id.nav_paid
                    2 -> binding.bottomNav.selectedItemId = R.id.nav_search
                    3 -> binding.bottomNav.selectedItemId = R.id.nav_dashboard
                }
            }
        })

        // Tapping Footer Icon scrolls ViewPager2
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_due -> {
                    if (binding.viewPager.currentItem != 0) binding.viewPager.setCurrentItem(0, true)
                    true
                }
                R.id.nav_paid -> {
                    if (binding.viewPager.currentItem != 1) binding.viewPager.setCurrentItem(1, true)
                    true
                }
                R.id.nav_search -> {
                    if (binding.viewPager.currentItem != 2) binding.viewPager.setCurrentItem(2, true)
                    true
                }
                R.id.nav_dashboard -> {
                    if (binding.viewPager.currentItem != 3) binding.viewPager.setCurrentItem(3, true)
                    true
                }
                else -> false
            }
        }
    }
}
FILE_EOF

# 2. Add foreign key indices in Entities.kt to eliminate Room warnings
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
    val writtenMarks: Int,
    val practicalMarks: Int,
    val grade: String,
    val submittedByTeacher: String = "Admin",
    val approvalStatus: String = "APPROVED"
) {
    val obtainedMarks: Int get() = writtenMarks + practicalMarks
}
FILE_EOF

# 3. Stage, commit, and push
git add app/src/main/java/com/vidyamandir/schoolmanager/MainActivity.kt
git add app/src/main/java/com/vidyamandir/schoolmanager/data/Entities.kt
git commit -m "fix: resolve unresolved view references in MainActivity and index foreign keys"
git push origin main

echo "Done! Pushed cleanly to GitHub main."
