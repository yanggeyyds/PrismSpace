package com.yzddmr6.prismspace.prism.compose.space

/** Single source of truth for whether the dual space is usable RIGHT NOW
 *  (CE-unlock-aware — not just profile-owner/running). */
enum class SpaceUsability { NotProvisioned, Suspended, LockedNeedsUnlock, Usable }

/** Pure, unit-tested. Precedence: NotProvisioned > Suspended(not running) >
 *  LockedNeedsUnlock(running but CE-locked) > Usable. */
fun spaceUsability(provisioned: Boolean, running: Boolean, unlocked: Boolean): SpaceUsability =
    when {
        !provisioned -> SpaceUsability.NotProvisioned
        !running     -> SpaceUsability.Suspended
        !unlocked    -> SpaceUsability.LockedNeedsUnlock
        else         -> SpaceUsability.Usable
    }
