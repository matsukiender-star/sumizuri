package com.sumizuri.app.data.text

import java.text.DateFormat
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

actual fun localOffsetMillis(at: Long): Long = TimeZone.getDefault().getOffset(at).toLong()

actual fun monthYearText(at: Long): String = SimpleDateFormat("LLLL yyyy", Locale.getDefault()).format(Date(at))

actual fun monthYearShort(at: Long): String = SimpleDateFormat("LLL yyyy", Locale.getDefault()).format(Date(at))

actual fun weekdayName(weekday: Int): String = DateFormatSymbols.getInstance(Locale.getDefault()).weekdays[
    weekday % 7 +
        1,
]

actual fun dateText(at: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(Date(at))

actual fun monthIndexOf(at: Long): Int = Calendar.getInstance().apply { timeInMillis = at }.let {
    it.get(Calendar.YEAR) *
        12 +
        it.get(Calendar.MONTH)
}
