package com.sumizuri.app.ui.kit.cover.focus.face

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// The smallest face looked for, as a share of the shorter side of the picture. Smaller ones are mostly speckles of hair and cloth.
private const val SmallestFace = 0.12

// How much the window grows from one round of the scan to the next.
private const val ScaleStep = 1.1

// The cells of a scaled feature are rounded, so one can reach a few pixels past its window.
private const val Slack = 3

// Looks for faces in a grey picture with the cascade, at many sizes, and joins the windows that sit on the same face. Only a face that enough windows agree on is kept, the best of them first.
fun LbpCascade.detect(gray: IntArray, width: Int, height: Int, minNeighbors: Int = 3): List<FaceBox> {
    val stride = width + 1
    val integral = IntArray(stride * (height + 1))
    for (y in 0 until height) {
        var row = 0
        for (x in 0 until width) {
            row += gray[y * width + x]
            integral[(y + 1) * stride + x + 1] = integral[y * stride + x + 1] + row
        }
    }
    val corners = IntArray(16)
    val hits = ArrayList<FaceBox>()
    val shortest = min(width, height)
    var scale = 1.0
    while (size * scale + Slack <= shortest) {
        val window = (size * scale).roundToInt()
        if (window >= shortest * SmallestFace) {
            val scaled = IntArray(rects.size) {
                (rects[it] * scale).roundToInt().let { value ->
                    if (it % 4 >=
                        2
                    ) {
                        max(1, value)
                    } else {
                        value
                    }
                }
            }
            val step = max(1, window / 12)
            for (top in 0..height - window - Slack step step) {
                for (left in 0..width - window - Slack step step) {
                    if (passes(integral, stride, left, top, scaled, corners)) {
                        hits +=
                            FaceBox(left + window / 2f, top + window / 2f, window.toFloat(), 1)
                    }
                }
            }
        }
        scale *= ScaleStep
    }
    return group(hits, minNeighbors)
}

private fun LbpCascade.passes(
    integral: IntArray,
    stride: Int,
    left: Int,
    top: Int,
    scaled: IntArray,
    corners: IntArray,
): Boolean {
    for (stage in stages) {
        var sum = 0f
        for (k in stage.features.indices) {
            val feature = stage.features[k] * 4
            val code =
                code(
                    integral,
                    stride,
                    left + scaled[feature],
                    top + scaled[feature + 1],
                    scaled[feature + 2],
                    scaled[
                        feature +
                            3,
                    ],
                    corners,
                )
            val inSet = stage.subsets[k * 8 + (code shr 5)] and (1 shl (code and 31)) != 0
            sum += if (inSet) stage.leaves[k * 2] else stage.leaves[k * 2 + 1]
        }
        if (sum < stage.threshold - 1e-5f) return false
    }
    return true
}

// The code of one feature: the nine cells of a three by three block, and a bit for each outer cell that is at least as bright as the middle one.
private fun code(integral: IntArray, stride: Int, x: Int, y: Int, w: Int, h: Int, corners: IntArray): Int {
    for (j in 0..3) for (i in 0..3) corners[j * 4 + i] = integral[(y + j * h) * stride + x + i * w]
    fun cell(i: Int, j: Int) =
        corners[j * 4 + i] - corners[j * 4 + i + 1] - corners[(j + 1) * 4 + i] + corners[(j + 1) * 4 + i + 1]
    val middle = cell(1, 1)
    return (if (cell(0, 0) >= middle) 128 else 0) or
        (if (cell(1, 0) >= middle) 64 else 0) or
        (if (cell(2, 0) >= middle) 32 else 0) or
        (if (cell(2, 1) >= middle) 16 else 0) or
        (if (cell(2, 2) >= middle) 8 else 0) or
        (if (cell(1, 2) >= middle) 4 else 0) or
        (if (cell(0, 2) >= middle) 2 else 0) or
        (if (cell(0, 1) >= middle) 1 else 0)
}

// Windows whose middles are close and whose sizes are alike are one face. The face is their average and the count is how sure the cascade was.
private fun group(hits: List<FaceBox>, minNeighbors: Int): List<FaceBox> {
    val leader = IntArray(hits.size) { it }
    fun find(i: Int): Int {
        var at = i
        while (leader[at] != at) {
            leader[at] = leader[leader[at]]
            at = leader[at]
        }
        return at
    }
    for (i in hits.indices) {
        for (j in i + 1 until hits.size) {
            val a = hits[i]
            val b = hits[j]
            val reach = 0.3f * min(a.size, b.size)
            if (abs(a.x - b.x) <= reach && abs(a.y - b.y) <= reach &&
                max(a.size, b.size) <= 1.5f * min(a.size, b.size)
            ) {
                leader[find(i)] = find(j)
            }
        }
    }
    return hits.indices
        .groupBy(::find)
        .values
        .filter { it.size >= minNeighbors }
        .map { members ->
            val boxes = members.map { hits[it] }
            FaceBox(
                boxes
                    .map {
                        it.x
                    }.average()
                    .toFloat(),
                boxes
                    .map {
                        it.y
                    }.average()
                    .toFloat(),
                boxes.map { it.size }.average().toFloat(),
                boxes.size,
            )
        }.sortedByDescending { it.neighbors * it.size }
}
