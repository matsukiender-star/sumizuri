package com.sumizuri.app.data.text

// How far the clock of this device is from UTC at a moment, in milliseconds, so a moment can be put on a day of the person's own.
expect fun localOffsetMillis(at: Long): Long

// The month and the year of a moment in full, as in October 2026, in the language of the device.
expect fun monthYearText(at: Long): String

// The same shortened, as in Oct 2026.
expect fun monthYearShort(at: Long): String

// The name of a day of the week, counting Monday as 1, in the language of the device.
expect fun weekdayName(weekday: Int): String

// A date in full, as in Oct 3, 2026, in the language of the device.
expect fun dateText(at: Long): String

// The month of a moment counted in months since the year 0, so two moments of one month share a number.
expect fun monthIndexOf(at: Long): Int
