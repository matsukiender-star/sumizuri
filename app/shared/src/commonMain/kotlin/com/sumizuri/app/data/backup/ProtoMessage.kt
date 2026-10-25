package com.sumizuri.app.data.backup

// A protobuf message read without its schema, which is enough to pick out the fields by their numbers. A field is a number, how it was written, and its value: a whole number, or the bytes of a text or an inner message.
class ProtoMessage private constructor(private val fields: List<Field>) {
    private class Field(val number: Int, val wireType: Int, val value: Any)

    fun text(number: Int): String? = fields
        .firstOrNull {
            it.number == number && it.value is ByteArray
        }?.let { (it.value as ByteArray).decodeToString() }

    fun number(number: Int): Long? = fields.firstOrNull { it.number == number && it.value is Long }?.value as Long?

    // True when the field is there at all, for a yes or no that protobuf leaves out when it is no.
    fun has(number: Int): Boolean = number(number) != null

    fun messages(number: Int): List<ProtoMessage> = fields
        .filter {
            it.number == number && it.value is ByteArray
        }.map { decode(it.value as ByteArray) }

    // A repeated whole number, whether it was written one to a field or packed in one run.
    fun numbers(number: Int): List<Long> = buildList {
        for (field in fields) {
            if (field.number != number) continue
            when (val value = field.value) {
                is Long -> add(value)

                is ByteArray -> {
                    var at = 0
                    while (at < value.size) {
                        val (next, size) = readVarint(value, at)
                        add(next)
                        at += size
                    }
                }
            }
        }
    }

    fun float(number: Int): Float? = fields
        .firstOrNull {
            it.number == number && it.wireType == FixedFour
        }?.let { Float.fromBits((it.value as Long).toInt()) }

    companion object {
        private const val Varint = 0
        private const val Fixed64 = 1
        private const val Delimited = 2
        private const val FixedFour = 5

        fun decode(bytes: ByteArray): ProtoMessage {
            val fields = ArrayList<Field>()
            var at = 0
            while (at < bytes.size) {
                val (tag, tagSize) = readVarint(bytes, at)
                at += tagSize
                val number = (tag shr 3).toInt()
                when (val wire = (tag and 7).toInt()) {
                    Varint -> {
                        val (value, size) = readVarint(bytes, at)
                        fields += Field(number, wire, value)
                        at += size
                    }

                    Delimited -> {
                        val (length, size) = readVarint(bytes, at)
                        at += size
                        fields += Field(number, wire, bytes.copyOfRange(at, at + length.toInt()))
                        at += length.toInt()
                    }

                    FixedFour -> {
                        var value = 0L
                        for (index in 0 until 4) {
                            value =
                                value or ((bytes[at + index].toLong() and 0xff) shl (8 * index))
                        }
                        fields += Field(number, wire, value)
                        at += 4
                    }

                    Fixed64 -> at += 8

                    else -> error("Unexpected protobuf wire type $wire")
                }
            }
            return ProtoMessage(fields)
        }

        // Gives the number and how many bytes it took.
        private fun readVarint(bytes: ByteArray, from: Int): Pair<Long, Int> {
            var result = 0L
            var shift = 0
            var at = from
            while (true) {
                val byte = bytes[at++].toInt()
                result = result or ((byte and 0x7f).toLong() shl shift)
                if (byte and 0x80 == 0) break
                shift += 7
            }
            return result to (at - from)
        }
    }
}
