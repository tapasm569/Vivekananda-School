package in.vidyamandir.schoolmanager.ui

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import in.vidyamandir.schoolmanager.databinding.ItemSubjectMarkInputBinding

data class SubjectScoreItem(
    val subjectName: String,
    var fullMarks: Int = 100,
    var writtenMarks: Int = 0,
    var practicalMarks: Int = 0
) {
    val obtainedMarks: Int get() = writtenMarks + practicalMarks

    val grade: String
        get() {
            val pct = if (fullMarks > 0) (obtainedMarks.toDouble() / fullMarks * 100) else 0.0
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
    private val items: List<SubjectScoreItem>
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

    inner class SubjectViewHolder(private val binding: ItemSubjectMarkInputBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SubjectScoreItem) {
            binding.tvSubjectName.text = item.subjectName
            binding.etFullMarks.setText(item.fullMarks.toString())
            binding.etWrittenMarks.setText(item.writtenMarks.toString())
            binding.etPracticalMarks.setText(item.practicalMarks.toString())
            updateBadge(item)

            binding.etFullMarks.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    item.fullMarks = s.toString().toIntOrNull() ?: 100
                    updateBadge(item)
                }
                override fun afterTextChanged(s: Editable?) {}
            })

            binding.etWrittenMarks.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    item.writtenMarks = s.toString().toIntOrNull() ?: 0
                    updateBadge(item)
                }
                override fun afterTextChanged(s: Editable?) {}
            })

            binding.etPracticalMarks.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    item.practicalMarks = s.toString().toIntOrNull() ?: 0
                    updateBadge(item)
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }

        private fun updateBadge(item: SubjectScoreItem) {
            binding.tvSubjectGradeBadge.text = "Grade: ${item.grade} (${item.obtainedMarks}/${item.fullMarks})"
        }
    }
}
