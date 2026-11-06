package com.vidyamandir.schoolmanager.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.data.SchoolClass
import com.vidyamandir.schoolmanager.databinding.ActivityClassManagementBinding
import com.vidyamandir.schoolmanager.databinding.ItemMonthFeeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClassManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityClassManagementBinding
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClassManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this, lifecycleScope)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.rvClasses.layoutManager = LinearLayoutManager(this)

        binding.cbDivideSection.setOnCheckedChangeListener { _, isChecked ->
            binding.tilSections.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        binding.btnAddClass = setSaveClickListener()
        observeClasses()
    }

    private fun setSaveClickListener(): View.OnClickListener {
        val listener = View.OnClickListener {
            val name = binding.etClassName.text?.toString()?.trim() ?: ""
            val feeStr = binding.etMonthlyFee.text?.toString()?.trim() ?: ""
            val hasSections = binding.cbDivideSection.isChecked
            val sections = if (hasSections) {
                binding.etSections.text?.toString()?.trim() ?: "A, B"
            } else {
                ""
            }

            if (name.isEmpty() || feeStr.isEmpty()) {
                Toast.makeText(this, "Please enter Class Name and Monthly Fee", Toast.LENGTH_SHORT).show()
                return@OnClickListener
            }

            lifecycleScope.launch(Dispatchers.IO) {
                val newClass = SchoolClass(
                    className = name,
                    monthlyFee = feeStr.toDoubleOrNull() ?: 500.0,
                    hasSections = hasSections,
                    sections = sections
                )
                db.schoolDao().insertClass(newClass)

                withContext(Dispatchers.Main) {
                    binding.etClassName.text?.clear()
                    binding.etMonthlyFee.text?.clear()
                    binding.cbDivideSection.isChecked = false
                    Toast.makeText(this@ClassManagementActivity, "Class registered successfully!", Toast.LENGTH_SHORT).show()
                }
            }
        }
        binding.btnAddClass.setOnClickListener(listener)
        return listener
    }

    private fun observeClasses() {
        lifecycleScope.launch {
            db.schoolDao().getAllClasses().collect { list ->
                binding.rvClasses.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                        val v = ItemMonthFeeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                        return object : RecyclerView.ViewHolder(v.root) {}
                    }

                    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
                        val cls = list[pos]
                        val b = ItemMonthFeeBinding.bind(holder.itemView)
                        b.tvMonthName.text = cls.className

                        if (cls.hasSections && cls.sections.isNotBlank()) {
                            val units = cls.getDisplayUnits().joinToString(", ") { "${cls.className} (${it.second})" }
                            b.tvFeeBreakdown.text = "Fee: ₹${cls.monthlyFee}/m • Units: $units"
                            b.tvStatusBadge.text = "SECTIONS: ${cls.sections}"
                        } else {
                            b.tvFeeBreakdown.text = "Fee: ₹${cls.monthlyFee}/m • Single Unit (No Sections)"
                            b.tvStatusBadge.text = "SINGLE CLASS"
                        }

                        b.cbSelectMonth.visibility = View.GONE
                    }

                    override fun getItemCount() = list.size
                }
            }
        }
    }
}
