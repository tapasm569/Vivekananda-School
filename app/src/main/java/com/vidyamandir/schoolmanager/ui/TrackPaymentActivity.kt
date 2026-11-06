package com.vidyamandir.schoolmanager.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.databinding.ActivityStudentManagementBinding
import com.vidyamandir.schoolmanager.databinding.ItemMonthFeeBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TrackPaymentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentManagementBinding
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.title = "Today's Tracked Payments"
        binding.fabAddStudent.hide()
        binding.tilClassFilter.visibility = android.view.View.GONE
        binding.toolbar.setNavigationOnClickListener { finish() }

        db = AppDatabase.getDatabase(this, lifecycleScope)
        binding.rvStudents.layoutManager = LinearLayoutManager(this)

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        lifecycleScope.launch {
            db.schoolDao().getTodayPayments(cal.timeInMillis).collect { list ->
                binding.rvStudents.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                        val b = ItemMonthFeeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                        return object : RecyclerView.ViewHolder(b.root) {}
                    }

                    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
                        val p = list[pos]
                        val b = ItemMonthFeeBinding.bind(holder.itemView)
                        val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(p.paymentDate))
                        b.tvMonthName.text = "${p.studentName} (${p.receiptNo})"
                        b.tvFeeBreakdown.text = "Base: ₹${p.baseAmount} | Fine: ₹${p.lateFeeAmount} | $timeStr"
                        b.tvStatusBadge.text = "₹${p.totalAmount}"
                        b.cbSelectMonth.isEnabled = false
                        b.cbSelectMonth.isChecked = true
                    }

                    override fun getItemCount() = list.size
                }
            }
        }
    }
}
