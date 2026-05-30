package com.yzddmr6.prismspace.prism.model

import java.nio.charset.StandardCharsets

data class PerAppFileShareSpec(
    val packageName: String,
    val safePackageSegment: String,
    val relativePath: String,
    val markerDisplayName: String,
    val markerPayload: String,
) : java.io.Serializable {
    val markerBytes: ByteArray get() = markerPayload.toByteArray(StandardCharsets.UTF_8)
}

object PerAppFileSharePolicy {
    private const val ROOT = "Download/PrismSpace"

    fun specFor(packageName: String): PerAppFileShareSpec {
        val safeSegment = packageName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val relativePath = "$ROOT/"
        return PerAppFileShareSpec(
            packageName = packageName,
            safePackageSegment = safeSegment,
            relativePath = relativePath,
            markerDisplayName = "prismspace-share-policy-$safeSegment.json",
            markerPayload = buildMarkerPayload(packageName, relativePath),
        )
    }

    private fun buildMarkerPayload(packageName: String, relativePath: String): String =
        "{\"packageName\":\"${packageName.jsonEscaped()}\",\"relativePath\":\"${relativePath.jsonEscaped()}\",\"mode\":\"ImportExportOnly\"}"

    private fun String.jsonEscaped(): String = replace("\\", "\\\\").replace("\"", "\\\"")
}
