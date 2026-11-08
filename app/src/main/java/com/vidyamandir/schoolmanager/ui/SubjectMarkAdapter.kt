package com.vidyamandir.schoolmanager.ui

import android.text.Editable
import android.text.TextWatcher
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputEditText
import androidx.recyclerview.widget.RecyclerView
import com.vidyamandir.schoolmanager.databinding.ItemSubjectMarkInputBinding

data class ComponentScoreItem(
    val name: String,
    val maxMarks: Int,
    var obtainedMarks: Int = 0,
    val position: Int = 0
)

data class SubjectScoreItem(
    val subjectName: String,
    var fullMarks: Int = 100,
    var writtenMarks: Int = 0,
    var oralMarks: Int = 0,
    var practicalMarks: Int = 0,
    var hasWritten: Boolean = true,
    var maxWritten: Int = 70,
    var hasOral: Boolean = false,
    var maxOral: Int = 10,
    var hasPractical: Boolean = false,
    var maxPractical: Int = 20,
    val customComponents: MutableList<ComponentScoreItem> = mutableListOf()
) {
    val obtainedMarks: Int
        get() = writtenMarks + oralMarks + practicalMarks +
            customComponents.sumOf { it.obtainedMarks }

    val grade: String
        get() {
            val pct = if (fullMarks > 0)
                obtainedMarks.toDouble() / fullMarks * 100
            else 0.0

            return when {
                pct >= 90 -> "AA"
                pct >= 80 -> "A+"
                pct >= 70 -> "A"
                pct >= 60 -> "B+"
                pct >= 50 -> "B"
                pct >= 35 -> "C"
                pct >= 25 -> "D"
                else -> "F"
            }
        }
}

class SubjectMarkAdapter(
    private val items: MutableList<SubjectScoreItem>
) : RecyclerView.Adapter<SubjectMarkAdapter.SubjectViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubjectViewHolder {
        val binding = ItemSubjectMarkInputBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SubjectViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SubjectViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun replaceItems(newItems: List<SubjectScoreItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class SubjectViewHolder(
        private val binding: ItemSubjectMarkInputBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var writtenWatcher: TextWatcher? = null
        private var oralWatcher: TextWatcher? = null
        private var practicalWatcher: TextWatcher? = null

        fun bind(item: SubjectScoreItem) {
            writtenWatcher?.let { binding.etWrittenMarks.removeTextChangedListener(it) }
            oralWatcher?.let { binding.etOralMarks.removeTextChangedListener(it) }
            practicalWatcher?.let { binding.etPracticalMarks.removeTextChangedListener(it) }

            binding.tvSubjectName.text = item.subjectName
            binding.etFullMarks.setText(item.fullMarks.toString())
            binding.etFullMarks.isEnabled = false

            binding.etWrittenMarks.setText(item.writtenMarks.toString())
            binding.etOralMarks.setText(item.oralMarks.toString())
            binding.etPracticalMarks.setText(item.practicalMarks.toString())

            setFieldVisible(binding.etWrittenMarks, item.hasWritten)
            setFieldVisible(binding.etOralMarks, item.hasOral)
            setFieldVisible(binding.etPracticalMarks, item.hasPractical)

            writtenWatcher = watcher { value ->
                item.writtenMarks = value.coerceIn(0, item.maxWritten)
                updateBadge(item)
            }
            oralWatcher = watcher { value ->
                item.oralMarks = value.coerceIn(0, item.maxOral)
                updateBadge(item)
            }
            practicalWatcher = watcher { value ->
                item.practicalMarks = value.coerceIn(0, item.maxPractical)
                updateBadge(item)
            }

            binding.etWrittenMarks.addTextChangedListener(writtenWatcher)
            binding.etOralMarks.addTextChangedListener(oralWatcher)
            binding.etPracticalMarks.addTextChangedListener(practicalWatcher)

            binding.llCustomMarks.removeAllViews()

            item.customComponents.forEach { component ->
                val field = TextInputLayout(binding.root.context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    hint = "${component.name} (/${component.maxMarks})"
                }

                val input = TextInputEditText(binding.root.context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    inputType = InputType.TYPE_CLASS_NUMBER
                    setText(component.obtainedMarks.toString())
                }

                input.addTextChangedListener(watcher { value ->
                    component.obtainedMarks = value.coerceIn(0, component.maxMarks)
                    updateBadge(item)
                })

                field.addView(input)
                binding.llCustomMarks.addView(field)
            }

            updateBadge(item)
        }

        private fun setFieldVisible(input: View, visible: Boolean) {
            input.visibility = if (visible) View.VISIBLE else View.GONE
            (input.parent?.parent as? View)?.visibility =
                if (visible) View.VISIBLE else View.GONE
        }

        private fun watcher(onChanged: (Int) -> Unit): TextWatcher {
            return object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    onChanged(s?.toString()?.toIntOrNull() ?: 0)
                }
                override fun afterTextChanged(s: Editable?) {}
            }
        }

        private fun updateBadge(item: SubjectScoreItem) {
            binding.tvSubjectGradeBadge.text =
                "Grade: ${item.grade} (${item.obtainedMarks}/${item.fullMarks})"
        }
    }
}
