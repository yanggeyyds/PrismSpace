package com.yzddmr6.prismspace.engine

/** Outcome of a launch attempt.
 *  Lives in `shared` because PrismManager returns it and shared cannot depend on mobile. */
sealed class LaunchResult {
    object Ok : LaunchResult()
    object SpaceNotReady : LaunchResult()
    object AppMissing : LaunchResult()
    object Denied : LaunchResult()
    data class Unknown(val reason: String?) : LaunchResult()
}
