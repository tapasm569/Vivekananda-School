package com.vidyamandir.schoolmanager.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
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

        private fun setPillBadge(view: TextView, text: String, textColor: String, bgColor: String) {
            view.text = text
            view.setTextColor(Color.parseColor(textColor))
            val radius = 20 * view.resources.displayMetrics.density
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = radius
                setColor(Color.parseColor(bgColor))
            }
            view.background = drawable
        }

        fun bind(item: MonthDueStatus) {
            binding.tvMonthName.text = "${item.monthName} ${item.academicYear}"

            if (item.isPaid) {
                binding.cbSelectMonth.isChecked = false
                binding.cbSelectMonth.isEnabled = false
                binding.tvFeeBreakdown.text = "Settled (₹${String.format("%.0f", item.paidAmount)})"
                setPillBadge(binding.tvStatusBadge, "PAID", "#16A34A", "#DCFCE7")
                binding.root.setOnClickListener(null)
                binding.cbSelectMonth.setOnClickListener(null)
                return
            }

            binding.cbSelectMonth.isEnabled = true
            val isChecked = selectedPeriodKeys.contains(item.periodKey)
            binding.cbSelectMonth.isChecked = isChecked

            // Visual feedback on card border
            if (isChecked) {
                binding.cardMonth.strokeColor = Color.parseColor("#0F3D6E")
                binding.cardMonth.strokeWidth = (2 * binding.root.resources.displayMetrics.density).toInt()
            } else {
                binding.cardMonth.strokeColor = Color.parseColor("#E2E8F0")
                binding.cardMonth.strokeWidth = (1 * binding.root.resources.displayMetrics.density).toInt()
            }

            if (item.paidAmount > 0.0) {
                setPillBadge(binding.tvStatusBadge, "PARTIAL: ₹${String.format("%.0f", item.baseFee)} Due", "#EA580C", "#FFEDD5")
                binding.tvFeeBreakdown.text = "Paid: ₹${String.format("%.0f", item.paidAmount)} / ₹${String.format("%.0f", item.fullMonthlyFee)} | Fine: ₹${item.lateFee}"
            } else if (item.isDue) {
                setPillBadge(binding.tvStatusBadge, "DUE", "#DC2626", "#FEE2E2")
                binding.tvFeeBreakdown.text = "Fee: ₹${item.baseFee} | Fine: ₹${item.lateFee}"
            } else {
                setPillBadge(binding.tvStatusBadge, "ADVANCE", "#D97706", "#FEF3C7")
                binding.tvFeeBreakdown.text = "Fee: ₹${item.baseFee}"
            }

            val toggleAction = {
                if (selectedPeriodKeys.contains(item.periodKey)) {
                    selectedPeriodKeys.remove(item.periodKey)
                } else {
                    selectedPeriodKeys.add(item.periodKey)
                }

                binding.cbSelectMonth.isChecked = selectedPeriodKeys.contains(item.periodKey)
                notifyItemChanged(adapterPosition)
                val selectedItems = items.filter { selectedPeriodKeys.contains(it.periodKey) }
                onSelectionChanged(selectedItems)
            }

            binding.cbSelectMonth.setOnClickListener { toggleAction() }
            binding.root.setOnClickListener { toggleAction() }
        }
    }
}
