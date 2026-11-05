package in.vidyamandir.schoolmanager.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import in.vidyamandir.schoolmanager.databinding.ItemMonthFeeBinding
import in.vidyamandir.schoolmanager.domain.MonthDueStatus

class MonthAdapter(
    private val onSelectionChanged: (selectedMonths: List<MonthDueStatus>) -> Unit
) : RecyclerView.Adapter<MonthAdapter.MonthViewHolder>() {

    private var items: List<MonthDueStatus> = emptyList()
    private val selectedMonthIndices = mutableSetOf<Int>()

    fun submitList(newList: List<MonthDueStatus>) {
        items = newList
        selectedMonthIndices.clear()
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

    inner class MonthViewHolder(private val binding: ItemMonthFeeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MonthDueStatus) {
            binding.tvMonthName.text = item.monthName

            if (item.isPaid) {
                binding.cbSelectMonth.isChecked = false
                binding.cbSelectMonth.isEnabled = false
                binding.tvFeeBreakdown.text = "Monthly fee already settled"
                binding.tvStatusBadge.text = "PAID"
                binding.tvStatusBadge.setTextColor(Color.parseColor("#16A34A"))
                binding.tvStatusBadge.setBackgroundColor(Color.parseColor("#DCFCE7"))
                binding.cardMonth.strokeColor = Color.parseColor("#E2E8F0")
                binding.root.setOnClickListener(null)
            } else {
                binding.cbSelectMonth.isEnabled = true
                binding.cbSelectMonth.isChecked = selectedMonthIndices.contains(item.monthIndex)

                if (item.isDue) {
                    binding.tvStatusBadge.text = "DUE"
                    binding.tvStatusBadge.setTextColor(Color.parseColor("#DC2626"))
                    binding.tvStatusBadge.setBackgroundColor(Color.parseColor("#FEE2E2"))
                    binding.tvFeeBreakdown.text = "Fee: ₹${item.baseFee} | Fine: ₹${item.lateFee}"
                } else {
                    binding.tvStatusBadge.text = "ADVANCE"
                    binding.tvStatusBadge.setTextColor(Color.parseColor("#D97706"))
                    binding.tvStatusBadge.setBackgroundColor(Color.parseColor("#FEF3C7"))
                    binding.tvFeeBreakdown.text = "Fee: ₹${item.baseFee} | Fine: ₹0"
                }

                val toggleAction = {
                    if (selectedMonthIndices.contains(item.monthIndex)) {
                        selectedMonthIndices.remove(item.monthIndex)
                    } else {
                        selectedMonthIndices.add(item.monthIndex)
                    }
                    notifyItemChanged(adapterPosition)
                    val selectedItems = items.filter { selectedMonthIndices.contains(it.monthIndex) }
                    onSelectionChanged(selectedItems)
                }

                binding.cbSelectMonth.setOnClickListener { toggleAction() }
                binding.root.setOnClickListener { toggleAction() }
            }
        }
    }
}
