package com.yzddmr6.prismspace.prism.model

object PerAppShareDestination {

    @Suppress("UNUSED_PARAMETER")
    fun mediaRelativePath(packageName: String): String =
        "Pictures/PrismSpace/"

    @Suppress("UNUSED_PARAMETER")
    fun downloadRelativePath(packageName: String): String =
        "Download/PrismSpace/"

    fun safePackageSegment(packageName: String): String =
        packageName.map { char ->
            if (char.isLetterOrDigit() || char == '.' || char == '_' || char == '-') char else '_'
        }.joinToString("").ifBlank { "unknown" }
}
