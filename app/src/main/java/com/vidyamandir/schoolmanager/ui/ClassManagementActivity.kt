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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClassManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityClassManagementBinding
    private lateinit var db: AppDatabase
    private var editingClassId: Long? = null
    private var classList: List<SchoolClass> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClassManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this, lifecycleScope)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.rvClasses.layoutManager = LinearLayoutManager(this)

        binding.cbDivideSection.setOnCheckedChangeListener { _, checked ->
            binding.tilSections.visibility = if (checked) View.VISIBLE else View.GONE
        }

        binding.btnAddClass.setOnClickListener { saveClass() }
        observeClasses()
    }

    private fun saveClass() {
        val name = binding.etClassName.text?.toString()?.trim().orEmpty()
        val fee = binding.etMonthlyFee.text?.toString()?.trim()?.toDoubleOrNull()
        val hasSections = binding.cbDivideSection.isChecked
        val sections = if (hasSections) {
            binding.etSections.text?.toString()?.trim().orEmpty()
        } else ""

        val subjects = binding.etClassSubjects.text?.toString()
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.distinctBy { it.lowercase() }
            ?.joinToString(",") ?: ""

        if (name.isBlank() || fee == null || !fee.isFinite() || fee < 0) {
            Toast.makeText(this, "Enter a valid class name and monthly fee", Toast.LENGTH_SHORT).show()
            return
        }

        if (hasSections && sections.isBlank()) {
            Toast.makeText(this, "Please enter section names", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val existing = withContext(Dispatchers.IO) {
                    db.schoolDao().getClassByName(name)
                }

                if (existing != null && existing.id != editingClassId) {
                    Toast.makeText(this@ClassManagementActivity,
                        "This class name already exists", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val id = editingClassId ?: 0L
                val schoolClass = SchoolClass(
                    id = id,
                    className = name,
                    monthlyFee = fee,
                    hasSections = hasSections,
                    sections = sections,
                    subjects = subjects
                )

                withContext(Dispatchers.IO) {
                    if (editingClassId == null) {
                        db.schoolDao().insertClass(schoolClass)
                    } else {
                        db.schoolDao().updateClass(schoolClass)
                    }
                }

                val message = if (editingClassId == null) {
                    "Class registered successfully"
                } else {
                    "Class updated successfully"
                }

                clearForm()
                Toast.makeText(this@ClassManagementActivity,
                    message, Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Toast.makeText(this@ClassManagementActivity,
                    "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun editClass(cls: SchoolClass) {
        editingClassId = cls.id
        binding.etClassName.setText(cls.className)
        binding.etMonthlyFee.setText(cls.monthlyFee.toString())
        binding.cbDivideSection.isChecked = cls.hasSections
        binding.etSections.setText(cls.sections)
        binding.etClassSubjects.setText(cls.subjects)
        binding.btnAddClass.text = "Update Class"
        binding.etClassName.requestFocus()
    }

    private fun clearForm() {
        editingClassId = null
        binding.etClassName.text?.clear()
        binding.etMonthlyFee.text?.clear()
        binding.etSections.text?.clear()
        binding.etClassSubjects.text?.clear()
        binding.cbDivideSection.isChecked = false
        binding.btnAddClass.text = "Save & Register Class"
    }

    private fun observeClasses() {
        lifecycleScope.launch {
            db.schoolDao().getAllClasses().collect { list ->
                classList = list
                binding.rvClasses.adapter = ClassAdapter()
            }
        }
    }

    private inner class ClassAdapter :
        RecyclerView.Adapter<ClassAdapter.Holder>() {

        inner class Holder(val b: ItemMonthFeeBinding) :
            RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val b = ItemMonthFeeBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return Holder(b)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val cls = classList[position]
            val b = holder.b

            b.tvMonthName.text = cls.className

            if (cls.hasSections && cls.sections.isNotBlank()) {
                val units = cls.getDisplayUnits().joinToString(", ") {
                    "${cls.className} (${it.second})"
                }
                b.tvFeeBreakdown.text = "Fee: ₹${cls.monthlyFee}/m • Units: $units"
                b.tvStatusBadge.text = "SECTIONS: ${cls.sections}"
            } else {
                b.tvFeeBreakdown.text = "Fee: ₹${cls.monthlyFee}/m • Single Unit"
                b.tvStatusBadge.text = "CLASS"
            }

            b.cbSelectMonth.visibility = View.GONE
            b.btnEditClass.visibility = View.VISIBLE
            b.btnEditClass.setOnClickListener { editClass(cls) }
        }

        override fun getItemCount() = classList.size
    }
}
