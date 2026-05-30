package com.yzddmr6.prismspace.prism.compose.space

sealed class PmCreateOutcome {
    data class Created(val userId: Int) : PmCreateOutcome()
    object LimitReached : PmCreateOutcome()
    /** OS per-user managed-profile cap exceeded (standard Android allows 1). */
    object ManagedProfileLimit : PmCreateOutcome()
    data class Failed(val reason: String?) : PmCreateOutcome()
}
sealed class PmRemoveOutcome {
    object Removed : PmRemoveOutcome()
    data class Failed(val reason: String?) : PmRemoveOutcome()
}

/** Root iff at least one non-blank output line. Assumes the shell command (Shell.SU.run("id"))
*  produces output only when root was granted (libsuperuser returns null/empty on denial). */
fun isRootOutput(lines: List<String>?): Boolean = lines?.any { it.isNotBlank() } == true

private val CREATE_ID = Regex("""created user id (\d+)""", RegexOption.IGNORE_CASE)

fun parsePmCreateOutput(lines: List<String>?): PmCreateOutcome {
    if (lines.isNullOrEmpty()) return PmCreateOutcome.Failed(null)
    val joined = lines.joinToString("\n")
    CREATE_ID.find(joined)?.let { return PmCreateOutcome.Created(it.groupValues[1].toInt()) }
    val low = joined.lowercase()
    if (low.contains("cannot add more profiles")) return PmCreateOutcome.ManagedProfileLimit
    if (low.contains("maximum number of users") || low.contains("limit reached"))
        return PmCreateOutcome.LimitReached
    return PmCreateOutcome.Failed(lines.firstOrNull { it.isNotBlank() && it.trim() != "END" })
}

fun parsePmRemoveOutput(lines: List<String>?): PmRemoveOutcome {
    if (lines.isNullOrEmpty()) return PmRemoveOutcome.Failed(null)
    val joined = lines.joinToString("\n")
    if (joined.contains("Success:", ignoreCase = true)) return PmRemoveOutcome.Removed
    return PmRemoveOutcome.Failed(lines.firstOrNull { it.isNotBlank() } ?: joined)
}

/** current = 1 (main) + currentManaged; Unknown when maxUsers null. */
fun computeCap(maxUsers: Int?, currentManaged: Int): SpaceCapProbe =
    if (maxUsers == null) SpaceCapProbe.Unknown
    else SpaceCapProbe.Known(max = maxUsers, current = 1 + currentManaged)
