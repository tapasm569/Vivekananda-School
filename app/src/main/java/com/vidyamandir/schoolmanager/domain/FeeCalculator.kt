package com.vidyamandir.schoolmanager.domain

import com.vidyamandir.schoolmanager.data.LateFeeRule
import com.vidyamandir.schoolmanager.data.Student
import java.util.Calendar

data class MonthDueStatus(
    val monthIndex: Int,
    val monthName: String,
    val baseFee: Double,
    val lateFee: Double,
    val isDue: Boolean,
    val isPaid: Boolean
)

object FeeCalculator {
    private val MONTH_NAMES = arrayOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    fun calculateDueMonths(
        student: Student,
        paidMonths: Set<Int>,
        activeRules: List<LateFeeRule>,
        currentCalendar: Calendar = Calendar.getInstance()
    ): List<MonthDueStatus> {
        val currentMonth = currentCalendar.get(Calendar.MONTH) + 1
        val currentDay = currentCalendar.get(Calendar.DAY_OF_MONTH)
        val statusList = mutableListOf<MonthDueStatus>()

        for (m in 1..12) {
            val name = MONTH_NAMES[m - 1]
            if (paidMonths.contains(m)) {
                statusList.add(MonthDueStatus(m, name, student.monthlyFee, 0.0, isDue = false, isPaid = true))
                continue
            }

            val isEligibleDue = m >= student.admissionMonth && m <= currentMonth
            val lateFee = if (isEligibleDue) {
                val overdueDays = if (m == currentMonth) (currentDay - 1) else 30
                computeLateFine(overdueDays, activeRules)
            } else {
                0.0
            }

            statusList.add(
                MonthDueStatus(
                    monthIndex = m,
                    monthName = name,
                    baseFee = student.monthlyFee,
                    lateFee = lateFee,
                    isDue = isEligibleDue,
                    isPaid = false
                )
            )
        }
        return statusList
    }

    private fun computeLateFine(overdueDays: Int, rules: List<LateFeeRule>): Double {
        if (overdueDays <= 10) return 0.0
        val matchedRule = rules.firstOrNull { overdueDays in it.dayFrom..it.dayTo }
        return matchedRule?.fineAmount ?: 0.0
    }
}
