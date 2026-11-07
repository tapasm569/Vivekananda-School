package com.vidyamandir.schoolmanager.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.data.Student
import com.vidyamandir.schoolmanager.databinding.FragmentDueListBinding
import com.vidyamandir.schoolmanager.databinding.ItemStudentCardBinding
import com.vidyamandir.schoolmanager.util.ImageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.util.Calendar

data class DueStudentCardItem(
    val student: Student,
    val totalDue: Double,
    val details: String
)

class DueListFragment : Fragment() {

    private var _binding: FragmentDueListBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDueListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = AppDatabase.getDatabase(requireContext(), viewLifecycleOwner.lifecycleScope)
        binding.rvDueList.layoutManager = LinearLayoutManager(requireContext())
        loadDues()
    }

    override fun onResume() {
        super.onResume()
        loadDues()
    }

    private fun loadDues() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val students = db.schoolDao().getAllActiveStudents().first()
            val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
            val dueList = mutableListOf<DueStudentCardItem>()
            var grandTotalDue = 0.0

            for (student in students) {
                val payments = db.schoolDao().getStudentPayments(student.id, 2026).first()
                val paidMonths = payments.flatMap { p ->
                    p.paidMonths.split(",").filter { it.isNotBlank() }.map { it.trim().toInt() }
                }.toSet()

                val dueCount = (student.admissionMonth..currentMonth).count { !paidMonths.contains(it) }
                if (dueCount > 0) {
                    val amount = dueCount * student.monthlyFee
                    grandTotalDue += amount
                    dueList.add(DueStudentCardItem(student, amount, "Due: $dueCount Month(s) unpaid"))
                }
            }

            withContext(Dispatchers.Main) {
                if (_binding != null) {
                    binding.tvTotalDueHeader.text = "₹${String.format("%.2f", grandTotalDue)}"
                    binding.tvDueStudentCount.text = "${dueList.size} Students"
                    binding.rvDueList.adapter = DueAdapter(dueList)
                }
            }
        }
    }

    private fun sendReminder(student: Student, dueAmt: Double) {
        try {
            val phone = if (student.parentPhone.startsWith("+91")) student.parentPhone else "+91${student.parentPhone}"
            val msg = "Namaskar, Vivekananda Vidyamandir Fee Reminder: Outstanding monthly fee of ₹$dueAmt is due for ${student.fullName} (${student.displayClass}). Please clear the dues soon."
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$phone&text=${URLEncoder.encode(msg, "UTF-8")}"))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "WhatsApp not found or invalid phone number", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class DueAdapter(private val list: List<DueStudentCardItem>) : RecyclerView.Adapter<DueAdapter.ViewHolder>() {

        class ViewHolder(val b: ItemStudentCardBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val b = ItemStudentCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(b)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val entry = list[position]
            val s = entry.student
            holder.b.tvStudentName.text = s.fullName
            holder.b.tvStudentMeta.text = "${s.displayClass} • Roll: ${s.rollNo}"
            holder.b.tvParentsName.text = "${entry.details} • 📞 ${s.parentPhone}"

            holder.b.tvMonthlyFeeBadge.text = "Due: ₹${entry.totalDue}"
            holder.b.tvMonthlyFeeBadge.setTextColor(android.graphics.Color.parseColor("#DC2626"))
            holder.b.tvMonthlyFeeBadge.setBackgroundColor(android.graphics.Color.parseColor("#FEE2E2"))

            ImageHelper.loadProfilePhoto(s.photoUri, holder.b.ivStudentAvatar, android.R.drawable.sym_def_app_icon)

            holder.b.root.setOnClickListener { StudentCardActions.openProfile(holder.itemView.context, s) }
            holder.b.btnStudentWhatsApp.setOnClickListener { StudentCardActions.whatsapp(holder.itemView.context, s) }
            holder.b.btnStudentCall.setOnClickListener { StudentCardActions.call(holder.itemView.context, s) }
            holder.b.btnCollectDue.visibility = View.VISIBLE
            holder.b.btnCollectDue.setOnClickListener { StudentCardActions.collect(holder.itemView.context, s) }
        }

        override fun getItemCount() = list.size
    }
}
