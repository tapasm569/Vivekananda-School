package com.vidyamandir.schoolmanager.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.data.Student
import com.vidyamandir.schoolmanager.databinding.FragmentSearchBinding
import com.vidyamandir.schoolmanager.databinding.ItemStudentCardBinding
import com.vidyamandir.schoolmanager.util.ImageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase
    private var allStudents: List<Student> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        db = AppDatabase.getDatabase(requireContext(), viewLifecycleOwner.lifecycleScope)
        binding.rvSearchResults.layoutManager = LinearLayoutManager(requireContext())
        loadStudents()

        binding.etSearchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filter(s?.toString()?.trim().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadStudents() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            allStudents = db.schoolDao().getAllActiveStudents().first()
            withContext(Dispatchers.Main) {
                if (_binding != null) filter(binding.etSearchInput.text?.toString()?.trim().orEmpty())
            }
        }
    }

    private fun filter(query: String) {
        val filtered = allStudents.filter { s ->
            query.isEmpty() ||
                s.fullName.contains(query, true) ||
                s.className.contains(query, true) ||
                s.section.contains(query, true) ||
                s.rollNo.toString() == query ||
                s.parentPhone.contains(query)
        }

        binding.rvSearchResults.adapter = SearchAdapter(filtered)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class SearchAdapter(private val list: List<Student>) :
        RecyclerView.Adapter<SearchAdapter.ViewHolder>() {

        class ViewHolder(val b: ItemStudentCardBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val b = ItemStudentCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(b)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val s = list[position]
            val context = holder.itemView.context

            holder.b.tvStudentName.text = s.fullName
            holder.b.tvStudentMeta.text = "${s.displayClass} • Roll: ${s.rollNo}"
            holder.b.tvParentsName.text = "Father: ${s.fatherName} • 📞 ${s.parentPhone}"
            holder.b.tvMonthlyFeeBadge.text = "₹${s.monthlyFee}/m"

            ImageHelper.loadProfilePhoto(s.photoUri, holder.b.ivStudentAvatar, android.R.drawable.sym_def_app_icon)

            holder.b.root.setOnClickListener {
                StudentCardActions.openProfile(context, s)
            }
            holder.b.btnStudentWhatsApp.setOnClickListener {
                StudentCardActions.whatsapp(context, s)
            }
            holder.b.btnStudentCall.setOnClickListener {
                StudentCardActions.call(context, s)
            }
            holder.b.btnCollectDue.visibility = View.VISIBLE
            holder.b.btnCollectDue.setOnClickListener {
                StudentCardActions.collect(context, s)
            }
            holder.b.btnStudentReceipts.setOnClickListener {
                context.startActivity(Intent(context, StudentReceiptHistoryActivity::class.java)
                    .putExtra("STUDENT_ID", s.id))
            }
        }

        override fun getItemCount() = list.size
    }
}
