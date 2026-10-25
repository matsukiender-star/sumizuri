package com.sumizuri.sync.core

// Argon2id as RFC 9106 writes it, in plain Kotlin, for the systems that have no library for it. It makes the same bytes as the Bouncy Castle code that Android and the desktop use, so a password gives the same proof and key on every device.
internal object Argon2id {
    private const val BlockLongs = 128
    private const val SyncPoints = 4
    private const val Version = 0x13
    private const val TypeId = 2

    // The tag of a password: the key to stretch, its salt, and the cost. The output is 32 bytes.
    fun hash(
        password: ByteArray,
        salt: ByteArray,
        memoryKib: Int,
        iterations: Int,
        parallelism: Int,
        length: Int = 32,
    ): ByteArray {
        require(
            parallelism >= 1 && iterations >= 1 && memoryKib >= 8 * parallelism,
        ) { "The cost of the hash is not valid." }
        val initial = initialHash(password, salt, memoryKib, iterations, parallelism, length)
        // The memory is rounded down to a whole number of blocks for every lane and slice.
        val blocks = 4 * parallelism * (memoryKib / (4 * parallelism))
        val lane = blocks / parallelism
        val segment = lane / SyncPoints
        val memory = LongArray(blocks * BlockLongs)
        for (i in 0 until parallelism) {
            for (column in 0..1) {
                val seed = initial + le32(column) + le32(i)
                val first = variableHash(seed, 1024)
                loadBlock(memory, (i * lane + column) * BlockLongs, first)
            }
        }
        val scratch = Scratch()
        for (pass in 0 until iterations) {
            for (slice in 0 until SyncPoints) {
                for (i in 0 until parallelism) {
                    fillSegment(memory, scratch, pass, i, slice, lane, segment, blocks, parallelism, iterations)
                }
            }
        }
        val last = LongArray(BlockLongs)
        for (i in 0 until parallelism) {
            val at = (i * lane + lane - 1) * BlockLongs
            for (k in 0 until BlockLongs) last[k] = last[k] xor memory[at + k]
        }
        return variableHash(blockBytes(last), length)
    }

    private class Scratch {
        val reference = LongArray(BlockLongs)
        val addressInput = LongArray(BlockLongs)
        val addressBlock = LongArray(BlockLongs)
        val temp = LongArray(BlockLongs)
        val out = LongArray(BlockLongs)
    }

    private fun initialHash(
        password: ByteArray,
        salt: ByteArray,
        memoryKib: Int,
        iterations: Int,
        parallelism: Int,
        length: Int,
    ): ByteArray {
        val input =
            le32(parallelism) + le32(length) + le32(memoryKib) + le32(iterations) + le32(Version) + le32(TypeId) +
                le32(password.size) + password + le32(salt.size) + salt + le32(0) + le32(0)
        return Blake2b.hash(input, 64)
    }

    // The hash of any length the RFC calls H', made from Blake2b that gives at most 64 bytes.
    private fun variableHash(input: ByteArray, length: Int): ByteArray {
        val prefixed = le32(length) + input
        if (length <= 64) return Blake2b.hash(prefixed, length)
        val rounds = (length + 31) / 32 - 2
        val out = ByteArray(length)
        var v = Blake2b.hash(prefixed, 64)
        v.copyInto(out, 0, 0, 32)
        for (i in 1 until rounds) {
            v = Blake2b.hash(v, 64)
            v.copyInto(out, i * 32, 0, 32)
        }
        val tail = Blake2b.hash(v, length - 32 * rounds)
        tail.copyInto(out, rounds * 32)
        return out
    }

    private fun fillSegment(
        memory: LongArray,
        s: Scratch,
        pass: Int,
        lane: Int,
        slice: Int,
        laneLength: Int,
        segmentLength: Int,
        blocks: Int,
        lanes: Int,
        iterations: Int,
    ) {
        val independent = pass == 0 && slice < SyncPoints / 2
        var counter = 0L
        var start = 0
        if (pass == 0 && slice == 0) start = 2
        if (independent) {
            s.addressInput.fill(0)
            s.addressInput[0] = pass.toLong()
            s.addressInput[1] = lane.toLong()
            s.addressInput[2] = slice.toLong()
            s.addressInput[3] = blocks.toLong()
            s.addressInput[4] = iterations.toLong()
            s.addressInput[5] = TypeId.toLong()
            if (start != 0) {
                counter++
                s.addressInput[6] = counter
                nextAddresses(s)
            }
        }
        var current = lane * laneLength + slice * segmentLength + start
        for (index in start until segmentLength) {
            val previous = if (current % laneLength == 0) current + laneLength - 1 else current - 1
            val random: Long = if (independent) {
                if (index % BlockLongs == 0) {
                    counter++
                    s.addressInput[6] = counter
                    nextAddresses(s)
                }
                s.addressBlock[index % BlockLongs]
            } else {
                memory[previous * BlockLongs]
            }
            val column = random and 0xFFFFFFFFL
            val referenceLane = if (pass == 0 && slice == 0) lane else ((random ushr 32) % lanes).toInt()
            val area = referenceArea(pass, slice, index, segmentLength, laneLength, sameLane = referenceLane == lane)
            val relative = (column * column) ushr 32
            val position = area - 1 - ((area * relative) ushr 32)
            val startPosition = if (pass != 0 && slice != SyncPoints - 1) (slice + 1) * segmentLength else 0
            val referenceColumn = ((startPosition + position) % laneLength).toInt()
            val reference = (referenceLane * laneLength + referenceColumn) * BlockLongs
            compress(memory, previous * BlockLongs, reference, current * BlockLongs, pass > 0, s)
            current++
        }
    }

    // How many blocks the reference may be chosen from.
    private fun referenceArea(
        pass: Int,
        slice: Int,
        index: Int,
        segmentLength: Int,
        laneLength: Int,
        sameLane: Boolean,
    ): Long {
        val area: Int = if (pass == 0) {
            if (slice == 0) {
                index - 1
            } else if (sameLane) {
                slice * segmentLength + index - 1
            } else {
                slice * segmentLength + (if (index == 0) -1 else 0)
            }
        } else {
            if (sameLane) {
                laneLength - segmentLength + index - 1
            } else {
                laneLength - segmentLength + (if (index == 0) -1 else 0)
            }
        }
        return area.toLong()
    }

    // The block of addresses for the next 128 blocks, made by running the compression twice over the counter block.
    private fun nextAddresses(s: Scratch) {
        s.temp.fill(0)
        compressInto(s.temp, s.addressInput, s)
        compressInto(s.addressBlock, s.temp, s)
    }

    // out = G(zero, input), where the first input is all zeros.
    private fun compressInto(out: LongArray, input: LongArray, s: Scratch) {
        val zero = LongArray(BlockLongs)
        val r = s.out
        for (k in 0 until BlockLongs) r[k] = zero[k] xor input[k]
        val work = r.copyOf()
        permute(work)
        for (k in 0 until BlockLongs) out[k] = work[k] xor r[k]
    }

    // The new block is the compression of the previous block and the reference. From the second pass on it is xored over the old block.
    private fun compress(
        memory: LongArray,
        previous: Int,
        reference: Int,
        target: Int,
        overwrite: Boolean,
        s: Scratch,
    ) {
        val r = s.reference
        for (k in 0 until BlockLongs) r[k] = memory[previous + k] xor memory[reference + k]
        val work = s.temp
        r.copyInto(work)
        permute(work)
        if (overwrite) {
            for (k in 0 until BlockLongs) memory[target + k] = memory[target + k] xor r[k] xor work[k]
        } else {
            for (k in 0 until BlockLongs) memory[target + k] = r[k] xor work[k]
        }
    }

    // The permutation of the compression: the rows of the block, then its columns, each as 16 words through the round of Blake2b.
    private fun permute(v: LongArray) {
        for (i in 0 until 8) {
            val b = 16 * i
            round(
                v, b, b + 1, b + 2, b + 3, b + 4, b + 5, b + 6, b + 7, b + 8, b + 9, b + 10, b + 11, b + 12, b + 13,
                b + 14, b + 15,
            )
        }
        for (i in 0 until 8) {
            val b = 2 * i
            round(
                v, b, b + 1, b + 16, b + 17, b + 32, b + 33, b + 48, b + 49, b + 64, b + 65, b + 80, b + 81, b + 96,
                b + 97, b + 112, b + 113,
            )
        }
    }

    private fun round(
        v: LongArray,
        a0: Int,
        a1: Int,
        a2: Int,
        a3: Int,
        a4: Int,
        a5: Int,
        a6: Int,
        a7: Int,
        a8: Int,
        a9: Int,
        a10: Int,
        a11: Int,
        a12: Int,
        a13: Int,
        a14: Int,
        a15: Int,
    ) {
        mix(v, a0, a4, a8, a12)
        mix(v, a1, a5, a9, a13)
        mix(v, a2, a6, a10, a14)
        mix(v, a3, a7, a11, a15)
        mix(v, a0, a5, a10, a15)
        mix(v, a1, a6, a11, a12)
        mix(v, a2, a7, a8, a13)
        mix(v, a3, a4, a9, a14)
    }

    // The mixing step of Blake2b with the product of the low halves added, which is what Argon2 changes in it.
    private fun mix(v: LongArray, a: Int, b: Int, c: Int, d: Int) {
        var va = v[a]
        var vb = v[b]
        var vc = v[c]
        var vd = v[d]
        va = va + vb + 2 * (va and 0xFFFFFFFFL) * (vb and 0xFFFFFFFFL)
        vd = rotr(vd xor va, 32)
        vc = vc + vd + 2 * (vc and 0xFFFFFFFFL) * (vd and 0xFFFFFFFFL)
        vb = rotr(vb xor vc, 24)
        va = va + vb + 2 * (va and 0xFFFFFFFFL) * (vb and 0xFFFFFFFFL)
        vd = rotr(vd xor va, 16)
        vc = vc + vd + 2 * (vc and 0xFFFFFFFFL) * (vd and 0xFFFFFFFFL)
        vb = rotr(vb xor vc, 63)
        v[a] = va
        v[b] = vb
        v[c] = vc
        v[d] = vd
    }

    private fun rotr(x: Long, n: Int): Long = (x ushr n) or (x shl (64 - n))

    private fun le32(value: Int): ByteArray = byteArrayOf(
        value.toByte(),
        (value ushr 8).toByte(),
        (value ushr 16).toByte(),
        (
            value ushr
                24
            ).toByte(),
    )

    private fun loadBlock(memory: LongArray, at: Int, bytes: ByteArray) {
        for (k in 0 until BlockLongs) {
            var word = 0L
            for (j in 7 downTo 0) word = (word shl 8) or (bytes[k * 8 + j].toLong() and 0xFF)
            memory[at + k] = word
        }
    }

    private fun blockBytes(block: LongArray): ByteArray {
        val out = ByteArray(1024)
        for (k in 0 until BlockLongs) {
            for (j in 0 until 8) out[k * 8 + j] = (block[k] ushr (8 * j)).toByte()
        }
        return out
    }
}

// Blake2b as RFC 7693 writes it, with no key, for an output of 1 to 64 bytes.
internal object Blake2b {
    private val Iv = longArrayOf(
        0x6a09e667f3bcc908L,
        -0x4498517a7b3558c5L,
        0x3c6ef372fe94f82bL,
        -0x5ab00ac5a0e2c90fL,
        0x510e527fade682d1L,
        -0x64fa9773d4c193e1L,
        0x1f83d9abfb41bd6bL,
        0x5be0cd19137e2179L,
    )

    private val Sigma = arrayOf(
        intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15),
        intArrayOf(14, 10, 4, 8, 9, 15, 13, 6, 1, 12, 0, 2, 11, 7, 5, 3),
        intArrayOf(11, 8, 12, 0, 5, 2, 15, 13, 10, 14, 3, 6, 7, 1, 9, 4),
        intArrayOf(7, 9, 3, 1, 13, 12, 11, 14, 2, 6, 5, 10, 4, 0, 15, 8),
        intArrayOf(9, 0, 5, 7, 2, 4, 10, 15, 14, 1, 11, 12, 6, 8, 3, 13),
        intArrayOf(2, 12, 6, 10, 0, 11, 8, 3, 4, 13, 7, 5, 15, 14, 1, 9),
        intArrayOf(12, 5, 1, 15, 14, 13, 4, 10, 0, 7, 6, 3, 9, 2, 8, 11),
        intArrayOf(13, 11, 7, 14, 12, 1, 3, 9, 5, 0, 15, 4, 8, 6, 2, 10),
        intArrayOf(6, 15, 14, 9, 11, 3, 0, 8, 12, 2, 13, 7, 1, 4, 10, 5),
        intArrayOf(10, 2, 8, 4, 7, 6, 1, 5, 15, 11, 9, 14, 3, 12, 13, 0),
    )

    fun hash(input: ByteArray, length: Int): ByteArray {
        require(length in 1..64)
        val h = Iv.copyOf()
        h[0] = h[0] xor (0x01010000L or length.toLong())
        val m = LongArray(16)
        var offset = 0
        // Every block but the last is full, and an empty input is one block of zeros.
        while (input.size - offset > 128) {
            readBlock(input, offset, 128, m)
            offset += 128
            compress(h, m, offset.toLong(), last = false)
        }
        val rest = input.size - offset
        readBlock(input, offset, rest, m)
        compress(h, m, input.size.toLong(), last = true)
        val out = ByteArray(length)
        for (i in 0 until length) out[i] = (h[i / 8] ushr (8 * (i % 8))).toByte()
        return out
    }

    private fun readBlock(input: ByteArray, offset: Int, count: Int, m: LongArray) {
        m.fill(0)
        for (i in 0 until count) m[i / 8] = m[i / 8] or ((input[offset + i].toLong() and 0xFF) shl (8 * (i % 8)))
    }

    private fun compress(h: LongArray, m: LongArray, counter: Long, last: Boolean) {
        val v = LongArray(16)
        h.copyInto(v, 0, 0, 8)
        Iv.copyInto(v, 8, 0, 8)
        v[12] = v[12] xor counter
        if (last) v[14] = v[14].inv()
        for (round in 0 until 12) {
            val s = Sigma[round % 10]
            g(v, 0, 4, 8, 12, m[s[0]], m[s[1]])
            g(v, 1, 5, 9, 13, m[s[2]], m[s[3]])
            g(v, 2, 6, 10, 14, m[s[4]], m[s[5]])
            g(v, 3, 7, 11, 15, m[s[6]], m[s[7]])
            g(v, 0, 5, 10, 15, m[s[8]], m[s[9]])
            g(v, 1, 6, 11, 12, m[s[10]], m[s[11]])
            g(v, 2, 7, 8, 13, m[s[12]], m[s[13]])
            g(v, 3, 4, 9, 14, m[s[14]], m[s[15]])
        }
        for (i in 0 until 8) h[i] = h[i] xor v[i] xor v[i + 8]
    }

    private fun g(v: LongArray, a: Int, b: Int, c: Int, d: Int, x: Long, y: Long) {
        v[a] = v[a] + v[b] + x
        v[d] = rotr(v[d] xor v[a], 32)
        v[c] = v[c] + v[d]
        v[b] = rotr(v[b] xor v[c], 24)
        v[a] = v[a] + v[b] + y
        v[d] = rotr(v[d] xor v[a], 16)
        v[c] = v[c] + v[d]
        v[b] = rotr(v[b] xor v[c], 63)
    }

    private fun rotr(x: Long, n: Int): Long = (x ushr n) or (x shl (64 - n))
}
