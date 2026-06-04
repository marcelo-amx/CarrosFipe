package com.example.lib

object FuzzyMatcher {

    // Normalize: lowercase, trim, remove special chars
    private fun normalize(s: String) =
        s.lowercase().replace(Regex("[^a-z0-9\\s]"), "").trim()

    // Token Sort Ratio — same words, different order ("ZX 300" / "300 ZX")
    fun tokenSortRatio(a: String, b: String): Double {
        val sortedA = normalize(a).split("\\s+".toRegex()).sorted().joinToString(" ")
        val sortedB = normalize(b).split("\\s+".toRegex()).sorted().joinToString(" ")
        return levenshteinSimilarity(sortedA, sortedB)
    }

    // Token Set Ratio — one is subset of the other ("Vertis" vs long string)
    fun tokenSetRatio(a: String, b: String): Double {
        val setA = normalize(a).split("\\s+".toRegex()).toSet()
        val setB = normalize(b).split("\\s+".toRegex()).toSet()
        val intersection = setA intersect setB
        val onlyA = intersection union (setA subtract setB)
        val onlyB = intersection union (setB subtract setA)

        val s1 = levenshteinSimilarity(
            intersection.sorted().joinToString(" "),
            onlyA.sorted().joinToString(" ")
        )
        val s2 = levenshteinSimilarity(
            intersection.sorted().joinToString(" "),
            onlyB.sorted().joinToString(" ")
        )
        val s3 = levenshteinSimilarity(
            onlyA.sorted().joinToString(" "),
            onlyB.sorted().joinToString(" ")
        )
        return maxOf(s1, s2, s3)
    }

    // Jaccard — fast token overlap score
    fun jaccardSimilarity(a: String, b: String): Double {
        val setA = normalize(a).split("\\s+".toRegex()).toSet()
        val setB = normalize(b).split("\\s+".toRegex()).toSet()
        if (setA.isEmpty() && setB.isEmpty()) return 1.0
        val intersection = (setA intersect setB).size
        val union = (setA union setB).size
        return intersection.toDouble() / union
    }

    // Combined score — picks the best strategy automatically
    fun score(a: String, b: String): Double {
        return maxOf(
            tokenSortRatio(a, b),
            tokenSetRatio(a, b),
            jaccardSimilarity(a, b)
        )
    }

    fun String.isAlikeTo(otherString: String) =
        score(this, otherString)

    // --- Core: Levenshtein similarity (0.0 to 1.0) ---
    private fun levenshteinSimilarity(a: String, b: String): Double {
        if (a == b) return 1.0
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val dist = levenshteinDistance(a, b)
        return 1.0 - dist.toDouble() / maxOf(a.length, b.length)
    }

    private fun levenshteinDistance(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length)
            for (j in 1..b.length)
                dp[i][j] = if (a[i - 1] == b[j - 1]) dp[i - 1][j - 1]
                            else 1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
        return dp[a.length][b.length]
    }
}