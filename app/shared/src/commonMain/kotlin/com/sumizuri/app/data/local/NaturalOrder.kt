package com.sumizuri.app.data.local

// Names in the order a person counts them, so Chapter 2 comes before Chapter 10, and letters are compared without minding their case.
val NaturalOrder: Comparator<String> = Comparator { a, b ->
    val left = chunks(a)
    val right = chunks(b)
    for (i in 0 until minOf(left.size, right.size)) {
        val x = left[i]
        val y = right[i]
        val bothNumbers = x.first().isDigit() && y.first().isDigit()
        val order = if (bothNumbers) {
            val trimmedX = x.trimStart('0')
            val trimmedY = y.trimStart('0')
            if (trimmedX.length != trimmedY.length) trimmedX.length - trimmedY.length else trimmedX.compareTo(trimmedY)
        } else {
            x.compareTo(y, ignoreCase = true)
        }
        if (order != 0) return@Comparator order
    }
    left.size - right.size
}

// A name cut into runs of digits and runs of anything else.
private fun chunks(name: String): List<String> {
    val parts = ArrayList<String>()
    val current = StringBuilder()
    for (char in name) {
        if (current.isNotEmpty() && current.last().isDigit() != char.isDigit()) {
            parts += current.toString()
            current.clear()
        }
        current.append(char)
    }
    if (current.isNotEmpty()) parts += current.toString()
    return parts
}
