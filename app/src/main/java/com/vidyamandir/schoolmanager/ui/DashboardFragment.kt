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
