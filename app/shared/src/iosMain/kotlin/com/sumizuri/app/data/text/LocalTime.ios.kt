package com.sumizuri.app.data.text

import platform.Foundation.*

private fun dateOf(at: Long): NSDate = NSDate.dateWithTimeIntervalSince1970(at / 1000.0)

private fun formatted(at: Long, pattern: String): String = NSDateFormatter()
    .apply {
        locale = NSLocale.currentLocale
        dateFormat = pattern
    }.stringFromDate(dateOf(at))

actual fun localOffsetMillis(at: Long): Long = NSTimeZone.localTimeZone.secondsFromGMTForDate(dateOf(at)) * 1000L

actual fun monthYearText(at: Long): String = formatted(at, "LLLL yyyy")

actual fun monthYearShort(at: Long): String = formatted(at, "LLL yyyy")

// The names start with Sunday, so Monday as 1 is the second and Sunday as 7 is the first.
actual fun weekdayName(weekday: Int): String =
    NSDateFormatter().apply { locale = NSLocale.currentLocale }.weekdaySymbols[
        weekday %
            7,
    ] as String

actual fun dateText(at: Long): String = NSDateFormatter()
    .apply {
        locale = NSLocale.currentLocale
        dateStyle = NSDateFormatterMediumStyle
        timeStyle = NSDateFormatterNoStyle
    }.stringFromDate(dateOf(at))

actual fun monthIndexOf(at: Long): Int {
    val parts = NSCalendar.currentCalendar.components(NSCalendarUnitYear or NSCalendarUnitMonth, fromDate = dateOf(at))
    return parts.year.toInt() * 12 + (parts.month.toInt() - 1)
}
