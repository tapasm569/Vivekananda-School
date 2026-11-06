package com.vidyamandir.schoolmanager.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.data.Teacher
import com.vidyamandir.schoolmanager.databinding.ActivityTeacherManagementBinding
import com.vidyamandir.schoolmanager.databinding.ItemTeacherCardBinding
import com.vidyamandir.schoolmanager.util.ImageHelper
import kotlinx.coroutines.launch

class TeacherManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTeacherManagementBinding
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTeacherManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        db = AppDatabase.getDatabase(this, lifecycleScope)

        binding.rvTeachers.layoutManager = LinearLayoutManager(this)

        binding.fabAddTeacher.setOnClickListener {
            startActivity(Intent(this, AddTeacherActivity::class.java))
        }

        lifecycleScope.launch {
            db.schoolDao().getAllTeachers().collect { list ->
                binding.rvTeachers.adapter = TeacherCardAdapter(list)
            }
        }
    }

    class TeacherCardAdapter(private val list: List<Teacher>) :
        RecyclerView.Adapter<TeacherCardAdapter.ViewHolder>() {

        class ViewHolder(val binding: ItemTeacherCardBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val b = ItemTeacherCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(b)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val t = list[position]
            holder.binding.tvTeacherName.text = t.name
            holder.binding.tvTeacherPhone.text = "📞 ${t.phone}"
            holder.binding.tvTeacherPassword.text = "Login Pass: ${t.password}"

            ImageHelper.loadProfilePhoto(
                t.photoUri,
                holder.binding.ivTeacherAvatar,
                android.R.drawable.sym_def_app_icon
            )
        }

        override fun getItemCount() = list.size
    }
}
