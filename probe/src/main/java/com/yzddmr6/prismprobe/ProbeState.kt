package com.yzddmr6.prismprobe

data class ProbeState(
    val userId: Int,
    val uid: Int,
    val packageName: String,
    val processName: String,
    val sdkInt: Int,
    val isManagedProfile: Boolean,
    val visibleImageCount: Int = 0,
    val visibleDownloadCount: Int = 0,
    val cameraGranted: Boolean = false,
    val microphoneGranted: Boolean = false,
    val locationGranted: Boolean = false,
    val contactsGranted: Boolean = false,
    val notificationGranted: Boolean = false,
) {

    fun identityLine(): String =
        "user=$userId uid=$uid package=$packageName process=$processName sdk=$sdkInt managed=$isManagedProfile"

    fun fileVisibilityLine(): String =
        "images=$visibleImageCount downloads=$visibleDownloadCount"

    fun permissionLine(): String =
        "camera=$cameraGranted microphone=$microphoneGranted location=$locationGranted contacts=$contactsGranted notification=$notificationGranted"
}
