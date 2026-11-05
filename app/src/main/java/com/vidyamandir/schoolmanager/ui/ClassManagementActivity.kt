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

        binding.btnAddClass.setOnClickListener {
            val name = binding.etClassName.text?.toString()?.trim() ?: ""
            val feeStr = binding.etMonthlyFee.text?.toString()?.trim() ?: ""

            if (name.isEmpty() || feeStr.isEmpty()) {
                Toast.makeText(this, "Please enter Class name and Fee", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch(Dispatchers.IO) {
                db.schoolDao().insertClass(SchoolClass(className = name, monthlyFee = feeStr.toDoubleOrNull() ?: 500.0))
                withContext(Dispatchers.Main) {
                    binding.etClassName.text?.clear()
                    binding.etMonthlyFee.text?.clear()
                    Toast.makeText(this@ClassManagementActivity, "Class Added!", Toast.LENGTH_SHORT).show()
                }
            }
        }

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
                        b.tvFeeBreakdown.text = "Monthly Fee: ₹${cls.monthlyFee} • Sections: ${cls.sections}"
                        b.tvStatusBadge.text = "ACTIVE"
                        b.cbSelectMonth.isEnabled = false
                        b.cbSelectMonth.isChecked = true
                    }
                    override fun getItemCount() = list.size
                }
            }
        }
    }
}
