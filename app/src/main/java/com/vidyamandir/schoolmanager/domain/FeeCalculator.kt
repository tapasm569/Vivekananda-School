package com.vidyamandir.schoolmanager.domain

import com.vidyamandir.schoolmanager.data.Student
import com.vidyamandir.schoolmanager.data.FeePayment
import com.vidyamandir.schoolmanager.data.LateFeeRule
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

data class MonthDueStatus(
    val academicYear: Int,
    val monthIndex: Int,
    val monthName: String,
    val baseFee: Double,
    val lateFee: Double,
    val isDue: Boolean,
    val isPaid: Boolean
) {
    val periodKey: String
        get() = "%04d-%02d".format(academicYear, monthIndex)
}

object FeeCalculator {

    fun defaultPenaltyRules(): List<LateFeeRule> = listOf(
        LateFeeRule(dayFrom = 1, dayTo = 4, fineAmount = 0.0),
        LateFeeRule(dayFrom = 5, dayTo = 9, fineAmount = 20.0),
        LateFeeRule(dayFrom = 10, dayTo = 19, fineAmount = 40.0),
        LateFeeRule(dayFrom = 20, dayTo = 29, fineAmount = 60.0),
        LateFeeRule(dayFrom = 30, dayTo = 9999, fineAmount = 100.0)
    )

    fun effectiveRules(rules: List<LateFeeRule>): List<LateFeeRule> {
        val oldDefaults = listOf(
            Triple(1, 10, 0.0),
            Triple(11, 20, 50.0),
            Triple(21, 30, 100.0)
        )
        val isOldDefault = rules.size == 3 &&
            rules.sortedBy { it.dayFrom }.map {
                Triple(it.dayFrom, it.dayTo, it.fineAmount)
            } == oldDefaults

        return if (rules.isEmpty() || isOldDefault) {
            defaultPenaltyRules()
        } else {
            rules
        }
    }


    private val MONTH_NAMES = arrayOf(
        "January", "February", "March", "April",
        "May", "June", "July", "August",
        "September", "October", "November", "December"
    )

    fun parsePaidPeriodKeys(payments: List<FeePayment>): Set<String> {
        val result = mutableSetOf<String>()

        payments.forEach { payment ->
            payment.paidMonths.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .forEach { token ->
                    if (token.matches(Regex("\\d{4}-\\d{2}"))) {
                        val parts = token.split("-")
                        val year = parts[0].toIntOrNull()
                        val month = parts[1].toIntOrNull()
                        if (year != null && month != null && month in 1..12) {
                            result.add("%04d-%02d".format(year, month))
                        }
                    } else {
                        val month = token.toIntOrNull()
                        if (month != null && month in 1..12) {
                            result.add(
                                "%04d-%02d".format(payment.academicYear, month)
                            )
                        }
                    }
                }
        }
        return result
    }

    fun calculateDueMonths(
        student: Student,
        paidPeriodKeys: Set<String>,
        rules: List<LateFeeRule>,
        currentCalendar: Calendar = Calendar.getInstance(),
        startYear: Int = 2026
    ): List<MonthDueStatus> {

        val currentYear = currentCalendar.get(Calendar.YEAR)
        val currentMonth = currentCalendar.get(Calendar.MONTH) + 1

        val today = dateOnly(
            currentYear,
            currentMonth - 1,
            currentCalendar.get(Calendar.DAY_OF_MONTH)
        )

        val statuses = mutableListOf<MonthDueStatus>()

        if (currentYear < startYear) return emptyList()

        for (year in startYear..currentYear) {
            val firstMonth = if (year == startYear) {
                student.admissionMonth.coerceIn(1, 12)
            } else {
                1
            }

            val lastMonth = if (year == currentYear) 12 else 12

            for (month in firstMonth..lastMonth) {
                val key = "%04d-%02d".format(year, month)
                val paid = paidPeriodKeys.contains(key)

                val isDue = year < currentYear || month <= currentMonth

                var lateFee = 0.0

                if (!paid) {
                    val nextMonth = Calendar.getInstance(
                        TimeZone.getTimeZone("UTC")
                    ).apply {
                        clear()
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month - 1)
                        set(Calendar.DAY_OF_MONTH, 1)
                        add(Calendar.MONTH, 1)
                    }

                    val dueDate = dateOnly(
                        nextMonth.get(Calendar.YEAR),
                        nextMonth.get(Calendar.MONTH),
                        1
                    )

                    val overdueDays = if (today.timeInMillis >= dueDate.timeInMillis) {
                        TimeUnit.MILLISECONDS.toDays(
                            today.timeInMillis - dueDate.timeInMillis
                        ).toInt() + 1
                    } else {
                        0
                    }

                    lateFee = rules
                        .filter { overdueDays >= it.dayFrom && overdueDays <= it.dayTo }
                        .maxByOrNull { it.dayFrom }
                        ?.fineAmount
                        ?: 0.0
                }

                statuses.add(
                    MonthDueStatus(
                        academicYear = year,
                        monthIndex = month,
                        monthName = MONTH_NAMES[month - 1],
                        baseFee = student.monthlyFee,
                        lateFee = lateFee,
                        isDue = isDue,
                        isPaid = paid
                    )
                )
            }
        }

        return statuses
    }

    private fun dateOnly(year: Int, month: Int, day: Int): Calendar {
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
        }
    }
}
