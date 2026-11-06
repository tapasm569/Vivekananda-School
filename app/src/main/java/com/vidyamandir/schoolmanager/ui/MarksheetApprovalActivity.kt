package com.vidyamandir.schoolmanager.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.data.ExamResult
import com.vidyamandir.schoolmanager.databinding.ActivityStudentManagementBinding
import com.vidyamandir.schoolmanager.databinding.ItemSubjectMarkInputBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MarksheetApprovalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentManagementBinding
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.title = "Approve Marksheets"
        binding.fabAddStudent.hide()
        binding.tilClassFilter.visibility = android.view.View.GONE
        binding.toolbar.setNavigationOnClickListener { finish() }

        db = AppDatabase.getDatabase(this, lifecycleScope)
        binding.rvStudents.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            db.schoolDao().getPendingMarksheets().collect { pendingList ->
                binding.rvStudents.adapter = ApprovalAdapter(pendingList) { id, status ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        db.schoolDao().updateMarksheetStatus(id, status)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MarksheetApprovalActivity, "Marksheet $status!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    class ApprovalAdapter(
        private val list: List<ExamResult>,
        private val onAction: (Long, String) -> Unit
    ) : RecyclerView.Adapter<ApprovalAdapter.ApprovalViewHolder>() {

        class ApprovalViewHolder(val binding: ItemSubjectMarkInputBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApprovalViewHolder {
            val binding = ItemSubjectMarkInputBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ApprovalViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ApprovalViewHolder, position: Int) {
            val item = list[position]
            holder.binding.tvSubjectName.text = "${item.subjectName} (${item.examName})"
            holder.binding.tvSubjectGradeBadge.text = "Tap to Approve (${item.obtainedMarks}/${item.fullMarks})"
            holder.binding.etFullMarks.setText(item.fullMarks.toString())
            holder.binding.etWrittenMarks.setText(item.writtenMarks.toString())
            holder.binding.etPracticalMarks.setText(item.practicalMarks.toString())

            holder.binding.root.setOnClickListener {
                onAction(item.id, "APPROVED")
            }
        }

        override fun getItemCount() = list.size
    }
}
