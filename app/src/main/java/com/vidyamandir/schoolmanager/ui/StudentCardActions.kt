package com.vidyamandir.schoolmanager.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.vidyamandir.schoolmanager.data.Student
import java.net.URLEncoder

object StudentCardActions {
    fun openProfile(context: Context, student: Student) {
        context.startActivity(Intent(context, StudentProfileActivity::class.java).putExtra("STUDENT_ID", student.id))
    }
    fun whatsapp(context: Context, student: Student) {
        val phone = student.parentPhone.filter { it.isDigit() }.let { if (it.length == 10) "91$it" else it }
        if (phone.isBlank()) { Toast.makeText(context, "Parent phone number unavailable", Toast.LENGTH_SHORT).show(); return }
        val msg = "Namaskar, Vivekananda Vidyamandir regarding ${student.fullName}."
        val uri = Uri.parse("https://wa.me/$phone?text=${URLEncoder.encode(msg, "UTF-8")}")
        try { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) } catch (_: Exception) { Toast.makeText(context, "Unable to open WhatsApp", Toast.LENGTH_SHORT).show() }
    }
    fun call(context: Context, student: Student) {
        val phone = student.parentPhone.trim()
        if (phone.isBlank()) { Toast.makeText(context, "Parent phone number unavailable", Toast.LENGTH_SHORT).show(); return }
        try { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))) } catch (_: Exception) { Toast.makeText(context, "Unable to open dialer", Toast.LENGTH_SHORT).show() }
    }
    fun collect(context: Context, student: Student) {
        context.startActivity(Intent(context, FeeCollectionActivity::class.java).putExtra("STUDENT_ID", student.id))
    }
}
