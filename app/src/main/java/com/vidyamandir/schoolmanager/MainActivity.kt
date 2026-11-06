package com.vidyamandir.schoolmanager

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.databinding.ActivityMainBinding
import com.vidyamandir.schoolmanager.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this, lifecycleScope)

        setupToolbar()
        observeDashboardStats()
        setupCardClicks()
    }

    override fun onResume() {
        super.onResume()
        refreshBalances()
    }

    private fun setupToolbar() {
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_logout -> {
                    val prefs = getSharedPreferences("vv_admin_auth", Context.MODE_PRIVATE)
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

    private fun observeDashboardStats() {
        lifecycleScope.launch {
            db.schoolDao().getStudentCount().collect { count ->
                binding.tvStudentCount.text = count.toString()
            }
        }

        lifecycleScope.launch {
            db.schoolDao().getClassCount().collect { count ->
                binding.tvClassCount.text = count.toString()
            }
        }

        lifecycleScope.launch {
            db.schoolDao().getTeacherCount().collect { count ->
                binding.tvTeacherCount.text = count.toString()
            }
        }

        lifecycleScope.launch {
            db.schoolDao().getPendingMarksheetCount().collect { count ->
                binding.tvPendingMarksheetCount.text = "$count Pending"
            }
        }

        lifecycleScope.launch {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            db.schoolDao().getTodayPayments(cal.timeInMillis).collect { todayPayments ->
                val todaySum = todayPayments.sumOf { it.totalAmount }
                binding.tvDailyCollection.text = "₹${String.format("%.2f", todaySum)}"
            }
        }

        lifecycleScope.launch {
            db.schoolDao().getAllPayments().collect { allPayments ->
                val paidSum = allPayments.sumOf { it.totalAmount }
                binding.tvPaidBalanceAmount.text = "₹${String.format("%.2f", paidSum)}"
            }
        }
    }

    private fun refreshBalances() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val students = db.schoolDao().getAllActiveStudents().first()
                val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
                var totalDues = 0.0

                for (student in students) {
                    val payments = db.schoolDao().getStudentPayments(student.id, 2026).first()
                    val paidMonths = payments.flatMap { p ->
                        p.paidMonths.split(",").filter { it.isNotBlank() }.map { it.trim().toInt() }
                    }.toSet()

                    val startMonth = student.admissionMonth.coerceAtMost(12)
                    var dueMonthsCount = 0
                    for (m in startMonth..currentMonth) {
                        if (!paidMonths.contains(m)) dueMonthsCount++
                    }
                    totalDues += (dueMonthsCount * student.monthlyFee)
                }

                withContext(Dispatchers.Main) {
                    binding.tvDueBalanceAmount.text = "₹${String.format("%.2f", totalDues)}"
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupCardClicks() {
        binding.cardStudents.setOnClickListener {
            startActivity(Intent(this, StudentManagementActivity::class.java))
        }

        binding.cardClasses.setOnClickListener {
            startActivity(Intent(this, ClassManagementActivity::class.java))
        }

        binding.cardTeachers.setOnClickListener {
            startActivity(Intent(this, TeacherManagementActivity::class.java))
        }

        binding.cardDueBalance.setOnClickListener {
            val intent = Intent(this, StudentDuesListActivity::class.java)
            intent.putExtra("VIEW_MODE", "DUE")
            startActivity(intent)
        }

        binding.cardPaidBalance.setOnClickListener {
            val intent = Intent(this, StudentDuesListActivity::class.java)
            intent.putExtra("VIEW_MODE", "PAID")
            startActivity(intent)
        }

        binding.cardTrackPayment.setOnClickListener {
            startActivity(Intent(this, TrackPaymentActivity::class.java))
        }

        binding.cardApprovedMarksheet.setOnClickListener {
            startActivity(Intent(this, MarksheetApprovalActivity::class.java))
        }

        binding.cardMessage.setOnClickListener {
            startActivity(Intent(this, WhatsAppMessageActivity::class.java))
        }
        binding.btnOpenMessage.setOnClickListener {
            startActivity(Intent(this, WhatsAppMessageActivity::class.java))
        }
    }
}
