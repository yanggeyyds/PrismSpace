package com.yzddmr6.prismspace.prism.compose.vm

import com.yzddmr6.prismspace.engine.LaunchResult
import com.yzddmr6.prismspace.mobile.R

/** UI feedback for a LaunchResult. Pure (no Android). Single source of launch copy.
 *  @property message user-facing text; empty string signals "no toast" (the Ok case).
 *  @property isError true for all non-Ok results. */
data class LaunchFeedback(val message: String, val isError: Boolean)

fun launchFeedback(result: LaunchResult, appLabel: String, res: StringResolver = zhFallback): LaunchFeedback = when (result) {
    LaunchResult.Ok -> LaunchFeedback("", isError = false)
    LaunchResult.SpaceNotReady ->
        LaunchFeedback(res(R.string.lz_vm_launch_space_not_ready, emptyArray()), isError = true)
    LaunchResult.AppMissing ->
        LaunchFeedback(res(R.string.lz_vm_launch_app_missing, arrayOf(appLabel)), isError = true)
    LaunchResult.Denied ->
        LaunchFeedback(res(R.string.lz_vm_launch_denied, emptyArray()), isError = true)
    is LaunchResult.Unknown ->
        LaunchFeedback(res(R.string.lz_vm_launch_unknown, arrayOf(appLabel, result.reason?.takeIf { it.isNotBlank() } ?: res(R.string.lz_vm_unknown_error, emptyArray()))), isError = true)
}
