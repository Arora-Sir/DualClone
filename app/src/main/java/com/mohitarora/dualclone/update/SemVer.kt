package com.mohitarora.dualclone.update

/** Semantic version with an optional prerelease tag. A prerelease sorts below the same release. */
data class SemVer(val major: Int, val minor: Int, val patch: Int, val prerelease: String? = null) : Comparable<SemVer> {

    val isPrerelease: Boolean get() = prerelease != null

    override fun compareTo(other: SemVer): Int {
        compareValuesBy(this, other, SemVer::major, SemVer::minor, SemVer::patch).let { if (it != 0) return it }
        val a = prerelease
        val b = other.prerelease
        return when {
            a == null && b == null -> 0
            a == null -> 1
            b == null -> -1
            else -> comparePrerelease(a, b)
        }
    }

    override fun toString(): String = "$major.$minor.$patch" + (prerelease?.let { "-$it" } ?: "")

    private fun comparePrerelease(a: String, b: String): Int {
        val pa = a.split('.')
        val pb = b.split('.')
        for (i in 0 until minOf(pa.size, pb.size)) {
            val na = pa[i].toIntOrNull()
            val nb = pb[i].toIntOrNull()
            val c = when {
                na != null && nb != null -> na.compareTo(nb)
                na != null -> -1
                nb != null -> 1
                else -> pa[i].compareTo(pb[i])
            }
            if (c != 0) return c
        }
        return pa.size.compareTo(pb.size)
    }

    companion object {
        private val REGEX = Regex("""^v?(\d+)\.(\d+)\.(\d+)(?:-([0-9A-Za-z.-]+))?(?:\+[0-9A-Za-z.-]+)?$""")

        fun parse(text: String): SemVer? {
            val m = REGEX.matchEntire(text.trim()) ?: return null
            val (major, minor, patch) = m.destructured.let { Triple(it.component1().toIntOrNull(), it.component2().toIntOrNull(), it.component3().toIntOrNull()) }
            if (major == null || minor == null || patch == null) return null
            return SemVer(major, minor, patch, m.groupValues[4].ifEmpty { null })
        }
    }
}
