package com.vidyamandir.schoolmanager.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.data.Student
import com.vidyamandir.schoolmanager.databinding.ActivityWhatsappBroadcastBinding
import com.vidyamandir.schoolmanager.databinding.ItemWhatsappStudentBinding
import com.vidyamandir.schoolmanager.util.ImageHelper
import kotlinx.coroutines.launch

class WhatsAppMessageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWhatsappBroadcastBinding
    private lateinit var db: AppDatabase
    private lateinit var studentAdapter: WhatsAppAdapter

    private var allStudents: List<Student> = emptyList()
    private var filteredStudents: List<Student> = emptyList()

    private val selectedStudentIds = mutableSetOf<Long>()

    private var selectedClass = "All Classes"

    private var broadcastQueue: List<Student> = emptyList()
    private var broadcastIndex = 0
    private var broadcastMessage = ""

    companion object {
        private const val REQUEST_WHATSAPP = 501
        private const val REQUEST_INDIVIDUAL_WHATSAPP = 502
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityWhatsappBroadcastBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this, lifecycleScope)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        studentAdapter = WhatsAppAdapter(
            list = emptyList(),
            selectedIds = selectedStudentIds,
            onSelectionChanged = {
                updateSelectionCount()
            },
            onWhatsApp = { student ->
                openWhatsApp(student, defaultMessage(student))
            },
            onCall = { student ->
                callParent(student)
            }
        )

        binding.rvStudents.layoutManager = LinearLayoutManager(this)
        binding.rvStudents.adapter = studentAdapter

        binding.actvClassFilter.setOnItemClickListener { parent, _, position, _ ->
            selectedClass = parent.getItemAtPosition(position).toString()
            applyFilters()
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?, start: Int, count: Int, after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?, start: Int, before: Int, count: Int
            ) {
                applyFilters()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.cbSelectAll.setOnCheckedChangeListener { _, checked ->
            filteredStudents.forEach { student ->
                if (checked) {
                    selectedStudentIds.add(student.id)
                } else {
                    selectedStudentIds.remove(student.id)
                }
            }
            refreshCards()
        }

        binding.btnSendBroadcast.setOnClickListener {
            startBroadcast()
        }

        loadStudents()
    }

    private fun loadStudents() {
        lifecycleScope.launch {
            try {
                db.schoolDao().getAllActiveStudents().collect { students ->
                    allStudents = students

                    val classes = listOf("All Classes") +
                        students.map { it.displayClass }
                            .distinct()
                            .sorted()

                    binding.actvClassFilter.setAdapter(
                        ArrayAdapter(
                            this@WhatsAppMessageActivity,
                            android.R.layout.simple_dropdown_item_1line,
                            classes
                        )
                    )

                    binding.actvClassFilter.setText("All Classes", false)
                    applyFilters()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@WhatsAppMessageActivity,
                    "Unable to load students: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun applyFilters() {
        val query = binding.etSearch.text?.toString()?.trim().orEmpty()

        filteredStudents = allStudents.filter { student ->
            val matchesClass =
                selectedClass == "All Classes" ||
                    student.displayClass.equals(selectedClass, true)

            val matchesSearch =
                query.isBlank() ||
                    student.fullName.contains(query, true) ||
                    student.admissionNo.contains(query, true) ||
                    student.parentPhone.contains(query, true)

            matchesClass && matchesSearch
        }

        refreshCards()
    }

    private fun refreshCards() {
        studentAdapter.updateList(filteredStudents)
        updateSelectionCount()
    }

    private fun updateSelectionCount() {
        binding.tvSelectedCount.text =
            "${selectedStudentIds.size} selected"

        val allVisibleSelected = filteredStudents.isNotEmpty() &&
            filteredStudents.all {
                selectedStudentIds.contains(it.id)
            }

        binding.cbSelectAll.setOnCheckedChangeListener(null)
        binding.cbSelectAll.isChecked = allVisibleSelected

        binding.cbSelectAll.setOnCheckedChangeListener { _, checked ->
            filteredStudents.forEach { student ->
                if (checked) {
                    selectedStudentIds.add(student.id)
                } else {
                    selectedStudentIds.remove(student.id)
                }
            }
            refreshCards()
        }
    }

    private fun defaultMessage(student: Student): String {
        return "Namaskar, this is Vivekananda Vidyamandir (Dhupguri). " +
            "Regarding ${student.fullName}, Class: ${student.displayClass}, " +
            "Roll: ${student.rollNo}. Thank you."
    }

    private fun getWhatsAppPhone(student: Student): String? {
        val digits = student.parentPhone.filter { it.isDigit() }

        return when {
            digits.length == 10 -> "91$digits"
            digits.length == 12 && digits.startsWith("91") -> digits
            else -> null
        }
    }

    private fun openWhatsApp(
        student: Student,
        message: String,
        requestCode: Int = REQUEST_INDIVIDUAL_WHATSAPP
    ) {
        val phone = getWhatsAppPhone(student)

        if (phone == null) {
            Toast.makeText(
                this,
                "Invalid parent phone number: ${student.parentPhone}",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        try {
            val uri = Uri.parse("https://api.whatsapp.com/send")
                .buildUpon()
                .appendQueryParameter("phone", phone)
                .appendQueryParameter("text", message)
                .build()

            startActivityForResult(
                Intent(Intent.ACTION_VIEW, uri),
                requestCode
            )
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Unable to open WhatsApp: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun callParent(student: Student) {
        val phone = student.parentPhone.filter {
            it.isDigit() || it == '+'
        }

        if (phone.isBlank()) {
            Toast.makeText(this, "Parent phone number is missing", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            startActivity(
                Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
            )
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to open phone dialer", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startBroadcast() {
        val selected = allStudents.filter {
            selectedStudentIds.contains(it.id)
        }

        if (selected.isEmpty()) {
            Toast.makeText(this, "Please select at least one parent", Toast.LENGTH_SHORT).show()
            return
        }

        val message = binding.etMessage.text?.toString()?.trim().orEmpty()

        if (message.isBlank()) {
            binding.tilMessage.error = "Please write your message first"
            return
        }

        binding.tilMessage.error = null

        broadcastQueue = selected
        broadcastIndex = 0
        broadcastMessage = message

        AlertDialog.Builder(this)
            .setTitle("Start WhatsApp Broadcast?")
            .setMessage(
                "${selected.size} parents selected.\n\n" +
                    "WhatsApp will open separately for each parent. " +
                    "You must press Send in WhatsApp yourself."
            )
            .setPositiveButton("Continue") { _, _ ->
                openNextBroadcast()
            }
            .setNegativeButton("Cancel") { _, _ ->
                clearBroadcastQueue()
            }
            .show()
    }

    private fun openNextBroadcast() {
        if (broadcastIndex >= broadcastQueue.size) {
            Toast.makeText(
                this,
                "Broadcast list completed",
                Toast.LENGTH_LONG
            ).show()
            clearBroadcastQueue()
            return
        }

        val student = broadcastQueue[broadcastIndex]
        val personalizedMessage =
            "$broadcastMessage\n\nStudent: ${student.fullName} | " +
                "Class: ${student.displayClass} | Roll: ${student.rollNo}"

        openWhatsApp(
            student,
            personalizedMessage,
            REQUEST_WHATSAPP
        )
    }

    private fun clearBroadcastQueue() {
        broadcastQueue = emptyList()
        broadcastIndex = 0
        broadcastMessage = ""
    }

    @Deprecated("Deprecated in Android API; retained for compatibility")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (
            requestCode == REQUEST_WHATSAPP &&
            broadcastIndex in broadcastQueue.indices
        ) {
            val current = broadcastQueue[broadcastIndex]

            AlertDialog.Builder(this)
                .setTitle("Next Parent")
                .setMessage(
                    "Returned from WhatsApp for ${current.fullName}.\n\n" +
                        "If you sent the message, continue to the next parent."
                )
                .setPositiveButton("Next Parent") { _, _ ->
                    broadcastIndex++
                    openNextBroadcast()
                }
                .setNegativeButton("Finish") { _, _ ->
                    clearBroadcastQueue()
                }
                .show()
        }
    }

    class WhatsAppAdapter(
        private var list: List<Student>,
        private val selectedIds: MutableSet<Long>,
        private val onSelectionChanged: () -> Unit,
        private val onWhatsApp: (Student) -> Unit,
        private val onCall: (Student) -> Unit
    ) : RecyclerView.Adapter<WhatsAppAdapter.ViewHolder>() {

        class ViewHolder(
            val binding: ItemWhatsappStudentBinding
        ) : RecyclerView.ViewHolder(binding.root)

        fun updateList(newList: List<Student>) {
            list = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ViewHolder {
            val binding = ItemWhatsappStudentBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val student = list[position]
            val item = holder.binding

            item.tvStudentName.text = student.fullName
            item.tvStudentMeta.text =
                "${student.displayClass} • Roll: ${student.rollNo}"

            item.tvParentsName.text =
                "Parent: ${student.fatherName} • ${student.parentPhone}"

            ImageHelper.loadProfilePhoto(
                student.photoUri,
                item.ivStudentAvatar,
                android.R.drawable.sym_def_app_icon
            )

            item.cbSelectStudent.setOnCheckedChangeListener(null)
            item.cbSelectStudent.isChecked =
                selectedIds.contains(student.id)

            item.cbSelectStudent.setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    selectedIds.add(student.id)
                } else {
                    selectedIds.remove(student.id)
                }
                onSelectionChanged()
            }

            item.cardStudent.setOnClickListener {
                item.cbSelectStudent.isChecked =
                    !item.cbSelectStudent.isChecked
            }

            item.btnWhatsApp.setOnClickListener {
                onWhatsApp(student)
            }

            item.btnCall.setOnClickListener {
                onCall(student)
            }
        }

        override fun getItemCount(): Int = list.size
    }
}
