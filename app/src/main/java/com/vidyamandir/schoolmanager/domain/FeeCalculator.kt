package com.vidyamandir.schoolmanager.domain

import com.vidyamandir.schoolmanager.data.Student
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

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
        "January", "February", "March", "April",
        "May", "June", "July", "August",
        "September", "October", "November", "December"
    )

    fun calculateDueMonths(
        student: Student,
        paidMonths: Set<Int>,
        graceDays: Int,
        penaltyAmount: Double,
        currentCalendar: Calendar = Calendar.getInstance()
    ): List<MonthDueStatus> {

        val currentMonth = currentCalendar.get(Calendar.MONTH) + 1
        val currentYear = currentCalendar.get(Calendar.YEAR)
        val today = dateOnly(
            currentYear,
            currentMonth - 1,
            currentCalendar.get(Calendar.DAY_OF_MONTH)
        )

        val safeGraceDays = graceDays.coerceAtLeast(0)
        val safePenalty = penaltyAmount.coerceAtLeast(0.0)

        val statusList = mutableListOf<MonthDueStatus>()

        for (m in 1..12) {
            val name = MONTH_NAMES[m - 1]

            if (paidMonths.contains(m)) {
                statusList.add(
                    MonthDueStatus(
                        m, name, student.monthlyFee,
                        0.0, false, true
                    )
                )
                continue
            }

            val isEligibleDue =
                m >= student.admissionMonth && m <= currentMonth

            var lateFee = 0.0

            if (isEligibleDue && m < currentMonth) {
                val monthEnd = Calendar.getInstance().apply {
                    set(Calendar.YEAR, currentYear)
                    set(Calendar.MONTH, m - 1)
                    set(Calendar.DAY_OF_MONTH, 1)
                }

                val lastDay = monthEnd.getActualMaximum(
                    Calendar.DAY_OF_MONTH
                )

                val endDate = dateOnly(
                    currentYear,
                    m - 1,
                    lastDay
                )

                val overdueDays = TimeUnit.MILLISECONDS.toDays(
                    today.timeInMillis - endDate.timeInMillis
                ).toInt()

                if (overdueDays > safeGraceDays) {
                    lateFee = safePenalty
                }
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

    private fun dateOnly(
        year: Int,
        month: Int,
        day: Int
    ): Calendar {
        return Calendar.getInstance(
            TimeZone.getTimeZone("UTC")
        ).apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
        }
    }
}
