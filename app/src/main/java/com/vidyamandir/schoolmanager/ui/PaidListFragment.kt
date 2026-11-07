package com.vidyamandir.schoolmanager.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AlertDialog
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.data.Student
import com.vidyamandir.schoolmanager.databinding.FragmentPaidListBinding
import com.vidyamandir.schoolmanager.databinding.ItemStudentCardBinding
import com.vidyamandir.schoolmanager.util.ImageHelper
import com.vidyamandir.schoolmanager.util.PaymentReceiptPdf
import com.vidyamandir.schoolmanager.util.ThermalReceiptPrinter
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
                    binding.rvPaidList.adapter = PaidAdapter(paidList) { student -> openStudentReceipts(student) }
                }
            }
        }
    }


    private fun openStudentReceipts(student: Student) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val payments = db.schoolDao().getStudentPayments(student.id, 2026).first()
                .sortedByDescending { it.paymentDate }
            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext
                if (payments.isEmpty()) {
                    Toast.makeText(requireContext(), "No payment receipts found for ${student.fullName}.", Toast.LENGTH_SHORT).show()
                    return@withContext
                }
                val labels = payments.map { payment ->
                    val date = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
                        .format(java.util.Date(payment.paymentDate))
                    "${payment.receiptNo}  •  ₹${String.format("%.2f", payment.totalAmount)}  •  $date"
                }.toTypedArray()
                AlertDialog.Builder(requireContext())
                    .setTitle("Payment Receipts — ${student.fullName}")
                    .setItems(labels) { _, index -> showReceiptOptions(student, payments[index]) }
                    .setNegativeButton("Close", null)
                    .show()
            }
        }
    }

    private fun showReceiptOptions(student: Student, payment: com.vidyamandir.schoolmanager.data.FeePayment) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val file = runCatching { PaymentReceiptPdf.create(requireContext(), student, payment) }.getOrNull()
            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext
                if (file == null) {
                    Toast.makeText(requireContext(), "Could not prepare this receipt PDF.", Toast.LENGTH_LONG).show()
                    return@withContext
                }
                AlertDialog.Builder(requireContext())
                    .setTitle(payment.receiptNo)
                    .setItems(arrayOf("View PDF", "Print PDF", "Thermal Printer", "Share PDF")) { _, choice ->
                        runCatching {
                            when (choice) {
                                0 -> PaymentReceiptPdf.view(requireContext(), file)
                                1 -> PaymentReceiptPdf.print(requireContext(), file)
                                2 -> ThermalReceiptPrinter.choosePrinterAndPrint(requireActivity(), PaymentReceiptPdf.thermalText(student, payment))
                                3 -> PaymentReceiptPdf.share(requireContext(), file)
                            }
                        }.onFailure {
                            Toast.makeText(requireContext(), "Unable to open receipt: ${it.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class PaidAdapter(
        private val list: List<Student>,
        private val onReceiptsClick: (Student) -> Unit
    ) : RecyclerView.Adapter<PaidAdapter.ViewHolder>() {

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
            holder.b.btnCollectDue.visibility = View.VISIBLE
            holder.b.btnCollectDue.text = "Receipt"
            holder.b.btnCollectDue.setBackgroundColor(android.graphics.Color.parseColor("#0F766E"))
            holder.b.btnCollectDue.setOnClickListener { onReceiptsClick(s) }
        }

        override fun getItemCount() = list.size
    }
}
