package com.sumizuri.app.ui.kit.cover.focus.face

// One round of a boosted cascade. Each weak test looks at one feature and picks one of two leaf values by the code of that feature. The round passes when the sum of the leaves reaches the threshold.
class LbpStage(val threshold: Float, val features: IntArray, val subsets: IntArray, val leaves: FloatArray)

// A cascade of local binary pattern tests, in the format OpenCV reads, that tells whether a window of a picture holds a face. The rects are four numbers for each feature, the left, the top, the width and the height of one of its nine cells.
class LbpCascade(val size: Int, val rects: IntArray, val stages: List<LbpStage>)

private val Numbers = Regex("-?[0-9]+(?:\\.[0-9]+)?(?:[eE][-+]?[0-9]+)?")

// Reads the XML text of a cascade of stumps. The file is regular, so a few searches do and no XML library is needed.
fun parseLbpCascade(xml: String): LbpCascade {
    val size = Regex("<width>(\\d+)</width>")
        .find(xml)
        ?.groupValues
        ?.get(1)
        ?.toInt() ?: error("The cascade has no size")
    val stagesText = xml.substringAfter("<stages>").substringBefore("<features>")
    val stages = stagesText.split("<stageThreshold>").drop(1).map { chunk ->
        val threshold = chunk.substringBefore("</stageThreshold>").trim().toFloat()
        val body = chunk.substringAfter("</stageThreshold>")
        val nodes = Regex("<internalNodes>(.*?)</internalNodes>", RegexOption.DOT_MATCHES_ALL)
            .findAll(body)
            .map {
                ints(it.groupValues[1])
            }.toList()
        val leaves = Regex("<leafValues>(.*?)</leafValues>", RegexOption.DOT_MATCHES_ALL)
            .findAll(body)
            .map {
                floats(it.groupValues[1])
            }.toList()
        // A stump holds the two child marks, the feature and eight words of the set of codes that take the first leaf.
        LbpStage(
            threshold = threshold,
            features = IntArray(nodes.size) { nodes[it][2] },
            subsets = IntArray(nodes.size * 8) { nodes[it / 8][3 + it % 8] },
            leaves = FloatArray(leaves.size * 2) { leaves[it / 2][it % 2] },
        )
    }
    val rects = Regex(
        "<rect>(.*?)</rect>",
        RegexOption.DOT_MATCHES_ALL,
    ).findAll(xml.substringAfter("<features>"))
        .flatMap {
            ints(it.groupValues[1])
        }.toList()
    return LbpCascade(size, rects.toIntArray(), stages)
}

private fun ints(text: String): List<Int> = Numbers.findAll(text).map { it.value.toInt() }.toList()

private fun floats(text: String): List<Float> = Numbers.findAll(text).map { it.value.toFloat() }.toList()
