package com.vidyamandir.schoolmanager.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.data.Student
import com.vidyamandir.schoolmanager.databinding.ActivityStudentManagementBinding
import com.vidyamandir.schoolmanager.databinding.ItemStudentCardBinding
import kotlinx.coroutines.launch
import java.net.URLEncoder

class WhatsAppMessageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentManagementBinding
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.title = "WhatsApp Parent Broadcast"
        binding.fabAddStudent.hide()
        binding.toolbar.setNavigationOnClickListener { finish() }

        db = AppDatabase.getDatabase(this, lifecycleScope)
        binding.rvStudents.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            db.schoolDao().getAllActiveStudents().collect { list ->
                binding.rvStudents.adapter = WhatsAppAdapter(list) { student ->
                    sendWhatsApp(student)
                }
            }
        }
    }

    private fun sendWhatsApp(student: Student) {
        try {
            val phone = if (student.parentPhone.startsWith("+91")) student.parentPhone else "+91${student.parentPhone}"
            val text = "Namaskar, this is Vivekananda Vidyamandir (Dhupguri). Notice regarding student: ${student.fullName} (Class: ${student.className}, Roll: ${student.rollNo})."
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$phone&text=${URLEncoder.encode(text, "UTF-8")}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "WhatsApp not installed or invalid phone number", Toast.LENGTH_SHORT).show()
        }
    }

    class WhatsAppAdapter(
        private val list: List<Student>,
        private val onClick: (Student) -> Unit
    ) : RecyclerView.Adapter<WhatsAppAdapter.WhatsAppViewHolder>() {

        class WhatsAppViewHolder(val binding: ItemStudentCardBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WhatsAppViewHolder {
            val binding = ItemStudentCardBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return WhatsAppViewHolder(binding)
        }

        override fun onBindViewHolder(holder: WhatsAppViewHolder, position: Int) {
            val s = list[position]
            holder.binding.tvStudentName.text = s.fullName
            holder.binding.tvStudentMeta.text = "${s.className} • 📞 ${s.parentPhone}"
            holder.binding.tvParentsName.text = "Tap to send WhatsApp alert"
            holder.binding.tvMonthlyFeeBadge.text = "CHAT"
            holder.binding.root.setOnClickListener { onClick(s) }
        }

        override fun getItemCount() = list.size
    }
}
