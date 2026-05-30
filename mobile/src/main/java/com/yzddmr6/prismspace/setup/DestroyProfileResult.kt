package com.yzddmr6.prismspace.setup

/** Tristate outcome of a destroy-profile attempt. Owned by the producer package (setup);
 *  consumed by the pure feedback mapper. Java references: `DestroyProfileResult.Success.INSTANCE`,
 *  `DestroyProfileResult.NotProfileOwner.INSTANCE`, `new DestroyProfileResult.Failed(msg)`. */
sealed class DestroyProfileResult {
    /** wipeData(0) returned without throwing — profile removal is now in progress. */
    object Success : DestroyProfileResult()
    /** Not profile owner — cannot wipe; correct recovery is manual system-Settings removal. */
    object NotProfileOwner : DestroyProfileResult()
    /** wipeData(0) threw — nothing was destroyed; safe to retry. */
    data class Failed(val reason: String?) : DestroyProfileResult()
}
