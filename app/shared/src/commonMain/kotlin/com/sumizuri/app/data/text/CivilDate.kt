package com.sumizuri.app.data.text

// A day of the calendar as the year, the month from 1 and the day of the month from 1.
data class CivilDate(val year: Int, val month: Int, val day: Int) {
    // The days since the first of January 1970.
    val epochDay: Long
        get() {
            val y = if (month <= 2) year - 1 else year
            val era = (if (y >= 0) y else y - 399) / 400
            val yearOfEra = y - era * 400
            val dayOfYear = (153 * (month + (if (month > 2) -3 else 9)) + 2) / 5 + day - 1
            val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
            return era * 146097L + dayOfEra - 719468
        }

    // The first day of the month this many months away.
    fun monthsAway(count: Int): CivilDate {
        val index = year * 12 + (month - 1) + count
        return CivilDate(index.floorDiv(12), index.mod(12) + 1, 1)
    }

    val daysInMonth: Int
        get() = when (month) {
            2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }

    companion object {
        // The day of a number of days since the first of January 1970.
        fun ofEpochDay(days: Long): CivilDate {
            val shifted = days + 719468
            val era = (if (shifted >= 0) shifted else shifted - 146096) / 146097
            val dayOfEra = shifted - era * 146097
            val yearOfEra = (dayOfEra - dayOfEra / 1460 + dayOfEra / 36524 - dayOfEra / 146096) / 365
            val dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
            val shiftedMonth = (5 * dayOfYear + 2) / 153
            val day = (dayOfYear - (153 * shiftedMonth + 2) / 5 + 1).toInt()
            val month = (if (shiftedMonth < 10) shiftedMonth + 3 else shiftedMonth - 9).toInt()
            val year = (yearOfEra + era * 400 + if (month <= 2) 1 else 0).toInt()
            return CivilDate(year, month, day)
        }
    }
}

// The day of the week of a day counted from 1970, with Monday as 1 and Sunday as 7. The first of January 1970 was a Thursday.
fun weekdayOf(epochDay: Long): Int = (epochDay + 3).mod(7L).toInt() + 1
