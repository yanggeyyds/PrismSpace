package com.yzddmr6.prismspace.prism.compose.vm

import com.yzddmr6.prismspace.mobile.R
import com.yzddmr6.prismspace.setup.DestroyProfileResult

/**
 * UI-facing decision derived from a [DestroyProfileResult]. Pure (no Android).
 *
 * @property message user-facing text (already localized; the single source of truth).
 * @property isError whether the message represents a failure.
 * @property routeToSystemRemoval when `true`, in-app removal is unavailable and the UI
 *   must direct the user to system Settings for manual profile removal.
 */
data class DestroyFeedback(
    val message: String,
    val isError: Boolean,
    val routeToSystemRemoval: Boolean,
)

/** Single source of all destroy-profile user-facing text/decisions. Pure & unit-tested. */
fun destroyProfileFeedback(result: DestroyProfileResult, res: StringResolver = zhFallback): DestroyFeedback = when (result) {
    DestroyProfileResult.Success ->
        DestroyFeedback(res(R.string.lz_vm_destroy_in_progress, emptyArray()), isError = false, routeToSystemRemoval = false)
    DestroyProfileResult.NotProfileOwner ->
        DestroyFeedback(res(R.string.lz_vm_destroy_not_profile_owner, emptyArray()), isError = true, routeToSystemRemoval = true)
    is DestroyProfileResult.Failed ->
        DestroyFeedback(res(R.string.lz_vm_destroy_failed, arrayOf(result.reason?.takeIf { it.isNotBlank() } ?: res(R.string.lz_vm_unknown_error, emptyArray()))), isError = true, routeToSystemRemoval = false)
}
