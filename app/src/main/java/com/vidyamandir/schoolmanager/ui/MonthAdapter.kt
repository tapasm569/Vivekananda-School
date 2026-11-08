package com.vidyamandir.schoolmanager.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vidyamandir.schoolmanager.databinding.ItemMonthFeeBinding
import com.vidyamandir.schoolmanager.domain.MonthDueStatus

class MonthAdapter(
    private val onSelectionChanged: (selectedMonths: List<MonthDueStatus>) -> Unit
) : RecyclerView.Adapter<MonthAdapter.MonthViewHolder>() {

    private var items: List<MonthDueStatus> = emptyList()
    private val selectedPeriodKeys = mutableSetOf<String>()

    fun submitList(newList: List<MonthDueStatus>) {
        items = newList
        selectedPeriodKeys.clear()
        notifyDataSetChanged()
        onSelectionChanged(emptyList())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonthViewHolder {
        val binding = ItemMonthFeeBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MonthViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MonthViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class MonthViewHolder(
        private val binding: ItemMonthFeeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MonthDueStatus) {
            binding.tvMonthName.text = "${item.monthName} ${item.academicYear}"

            if (item.isPaid) {
                binding.cbSelectMonth.isChecked = false
                binding.cbSelectMonth.isEnabled = false
                binding.tvFeeBreakdown.text = "Monthly fee settled (₹${String.format("%.0f", item.paidAmount)})"
                binding.tvStatusBadge.text = "PAID"
                binding.tvStatusBadge.setTextColor(Color.parseColor("#16A34A"))
                binding.tvStatusBadge.setBackgroundColor(Color.parseColor("#DCFCE7"))
                binding.root.setOnClickListener(null)
                binding.cbSelectMonth.setOnClickListener(null)
                return
            }

            binding.cbSelectMonth.isEnabled = true
            binding.cbSelectMonth.isChecked = selectedPeriodKeys.contains(item.periodKey)

            if (item.paidAmount > 0.0) {
                binding.tvStatusBadge.text = "PARTIAL: ₹${String.format("%.0f", item.baseFee)} Due"
                binding.tvStatusBadge.setTextColor(Color.parseColor("#EA580C"))
                binding.tvStatusBadge.setBackgroundColor(Color.parseColor("#FFEDD5"))
                binding.tvFeeBreakdown.text = "Paid: ₹${String.format("%.0f", item.paidAmount)} / ₹${String.format("%.0f", item.fullMonthlyFee)} | Fine: ₹${item.lateFee}"
            } else if (item.isDue) {
                binding.tvStatusBadge.text = "DUE"
                binding.tvStatusBadge.setTextColor(Color.parseColor("#DC2626"))
                binding.tvStatusBadge.setBackgroundColor(Color.parseColor("#FEE2E2"))
                binding.tvFeeBreakdown.text = "Fee: ₹${item.baseFee} | Fine: ₹${item.lateFee}"
            } else {
                binding.tvStatusBadge.text = "ADVANCE"
                binding.tvStatusBadge.setTextColor(Color.parseColor("#D97706"))
                binding.tvStatusBadge.setBackgroundColor(Color.parseColor("#FEF3C7"))
                binding.tvFeeBreakdown.text = "Fee: ₹${item.baseFee}"
            }

            val toggleAction = {
                if (selectedPeriodKeys.contains(item.periodKey)) {
                    selectedPeriodKeys.remove(item.periodKey)
                } else {
                    selectedPeriodKeys.add(item.periodKey)
                }

                binding.cbSelectMonth.isChecked = selectedPeriodKeys.contains(item.periodKey)
                val selectedItems = items.filter { selectedPeriodKeys.contains(it.periodKey) }
                onSelectionChanged(selectedItems)
            }

            binding.cbSelectMonth.setOnClickListener { toggleAction() }
            binding.root.setOnClickListener { toggleAction() }
        }
    }
}
