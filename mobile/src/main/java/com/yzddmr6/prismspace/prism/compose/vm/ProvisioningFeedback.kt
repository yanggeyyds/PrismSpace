package com.yzddmr6.prismspace.prism.compose.vm

import com.yzddmr6.prismspace.mobile.R
import com.yzddmr6.prismspace.prism.compose.space.CreateSpaceResult
import com.yzddmr6.prismspace.prism.compose.space.DeleteSpaceResult

/** Single source of all provisioning user-facing text. Pure (no Android). */
fun provisioningFeedback(result: CreateSpaceResult, res: StringResolver = zhFallback): DestroyFeedback = when (result) {
    is CreateSpaceResult.Success ->
        DestroyFeedback(res(R.string.lz_vm_create_success, emptyArray()), isError = false, routeToSystemRemoval = false)
    CreateSpaceResult.RootUnavailable ->
        DestroyFeedback(res(R.string.lz_vm_create_root_unavailable, emptyArray()), isError = true, routeToSystemRemoval = false)
    is CreateSpaceResult.CapReached ->
        DestroyFeedback(res(R.string.lz_vm_create_cap_reached, arrayOf(result.max)), isError = true, routeToSystemRemoval = false)
    CreateSpaceResult.ManagedProfileLimitReached ->
        DestroyFeedback(res(R.string.lz_vm_create_managed_profile_limit, emptyArray()), isError = true, routeToSystemRemoval = false)
    is CreateSpaceResult.Failed ->
        DestroyFeedback(res(R.string.lz_vm_create_failed, arrayOf(result.reason?.takeIf { it.isNotBlank() } ?: res(R.string.lz_vm_unknown_error, emptyArray()))), isError = true, routeToSystemRemoval = false)
}

fun provisioningFeedback(result: DeleteSpaceResult, res: StringResolver = zhFallback): DestroyFeedback = when (result) {
    DeleteSpaceResult.Success ->
        DestroyFeedback(res(R.string.lz_vm_delete_success, emptyArray()), isError = false, routeToSystemRemoval = false)
    DeleteSpaceResult.RootUnavailable ->
        DestroyFeedback(res(R.string.lz_vm_delete_root_unavailable, emptyArray()), isError = true, routeToSystemRemoval = true)
    is DeleteSpaceResult.FellBackToSelfDestroy -> destroyProfileFeedback(result.inner, res)
    is DeleteSpaceResult.Failed ->
        DestroyFeedback(res(R.string.lz_vm_delete_failed, arrayOf(result.reason?.takeIf { it.isNotBlank() } ?: res(R.string.lz_vm_unknown_error, emptyArray()))), isError = true, routeToSystemRemoval = false)
}
