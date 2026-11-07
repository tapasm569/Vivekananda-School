package com.vidyamandir.schoolmanager.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.data.Student
import com.vidyamandir.schoolmanager.databinding.FragmentPaidListBinding
import com.vidyamandir.schoolmanager.databinding.ItemStudentCardBinding
import com.vidyamandir.schoolmanager.util.ImageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class PaidListFragment : Fragment() {

    private var _binding: FragmentPaidListBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPaidListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = AppDatabase.getDatabase(requireContext(), viewLifecycleOwner.lifecycleScope)
        binding.rvPaidList.layoutManager = LinearLayoutManager(requireContext())
        loadPaidStudents()
    }

    override fun onResume() {
        super.onResume()
        loadPaidStudents()
    }

    private fun loadPaidStudents() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val students = db.schoolDao().getAllActiveStudents().first()
            val allPayments = db.schoolDao().getAllPayments().first()
            val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
            val paidList = mutableListOf<Student>()

            for (student in students) {
                val studentPayments = db.schoolDao().getStudentPayments(student.id, 2026).first()
                val paidMonths = studentPayments.flatMap { p ->
                    p.paidMonths.split(",").filter { it.isNotBlank() }.map { it.trim().toInt() }
                }.toSet()

                val dueCount = (student.admissionMonth..currentMonth).count { !paidMonths.contains(it) }
                if (dueCount == 0) {
                    paidList.add(student)
                }
            }

            val grandTotalPaid = allPayments.sumOf { it.totalAmount }

            withContext(Dispatchers.Main) {
                if (_binding != null) {
                    binding.tvTotalPaidHeader.text = "₹${String.format("%.2f", grandTotalPaid)}"
                    binding.tvPaidStudentCount.text = "${paidList.size} Cleared"
                    binding.rvPaidList.adapter = PaidAdapter(paidList)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class PaidAdapter(private val list: List<Student>) :
        RecyclerView.Adapter<PaidAdapter.ViewHolder>() {

        class ViewHolder(val b: ItemStudentCardBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val b = ItemStudentCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(b)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val s = list[position]
            holder.b.tvStudentName.text = s.fullName
            holder.b.tvStudentMeta.text = "${s.displayClass} • Roll: ${s.rollNo}"
            holder.b.tvParentsName.text = "Father: ${s.fatherName} • 📞 ${s.parentPhone}"

            holder.b.tvMonthlyFeeBadge.text = "PAID"
            holder.b.tvMonthlyFeeBadge.setTextColor(android.graphics.Color.parseColor("#16A34A"))
            holder.b.tvMonthlyFeeBadge.setBackgroundColor(android.graphics.Color.parseColor("#DCFCE7"))

            ImageHelper.loadProfilePhoto(s.photoUri, holder.b.ivStudentAvatar, android.R.drawable.sym_def_app_icon)
            holder.b.root.setOnClickListener { StudentCardActions.openProfile(holder.itemView.context, s) }
            holder.b.btnStudentWhatsApp.setOnClickListener { StudentCardActions.whatsapp(holder.itemView.context, s) }
            holder.b.btnStudentCall.setOnClickListener { StudentCardActions.call(holder.itemView.context, s) }
            holder.b.btnCollectDue.visibility = View.GONE
        }

        override fun getItemCount() = list.size
    }
}
