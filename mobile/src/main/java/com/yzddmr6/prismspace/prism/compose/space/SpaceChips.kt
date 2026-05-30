package com.yzddmr6.prismspace.prism.compose.space

import com.yzddmr6.prismspace.mobile.R
import com.yzddmr6.prismspace.prism.compose.vm.SpaceSegment
import com.yzddmr6.prismspace.prism.compose.vm.StringResolver

/** A chip in the 空间-page switcher row. id `"__create__"` is the trailing ＋新建 chip. */
data class SpaceChip(
    val id: String,
    val label: String,
    val selected: Boolean,
    val isCreate: Boolean,
)

const val CREATE_CHIP_ID = "__create__"

/** First dual space id in [spaces] order (assumed userId-sorted by SpaceRepository.spaces()), or null when none. */
fun pickDefaultDualSpaceId(spaces: List<PrismSpace>): String? =
    spaces.firstOrNull { it.kind == PrismSpaceKind.Dual }?.id

/** Mutual-exclusion gate: a dual chip may be highlighted ONLY when the Dual
 *  segment is active; on Main no dual chip is selected. Preserves last-selected
 *  dual (selectedDualSpaceId untouched; restored when segment returns to Dual). */
fun selectedDualChipId(
    segment: SpaceSegment,
    selectedDualSpaceId: String?,
    spaces: List<PrismSpace>,
): String? = if (segment == SpaceSegment.Dual)
    (selectedDualSpaceId ?: pickDefaultDualSpaceId(spaces)) else null

/** Chip row = main + each dual (in order) + trailing ＋新建. Exactly one of
 *  selectedMain / selectedDualId drives the highlight; the create chip is never selected. */
fun spaceChips(
    spaces: List<PrismSpace>,
    selectedMain: Boolean,
    selectedDualId: String?,
    showCreate: Boolean = false,
): List<SpaceChip> =
    buildList {
        spaces.forEach { s ->
            val sel = if (s.kind == PrismSpaceKind.Main) selectedMain else (s.id == selectedDualId)
            add(SpaceChip(s.id, s.displayName, selected = sel, isCreate = false))
        }
        if (showCreate) add(SpaceChip(CREATE_CHIP_ID, "＋新建空间", selected = false, isCreate = true))
    }

/** Honest experimental-block copy. ROM-dependent wording (NOT "永远无法") —
 *  multi-profile works only on custom ROMs that lift the per-parent quota. */
data class ExperimentalBlockInfo(val title: String, val body: String, val dismiss: String)

fun experimentalBlockInfo(
    spaceName: String,
    userId: Int,
    dualCount: Int,
    res: StringResolver,
): ExperimentalBlockInfo =
    ExperimentalBlockInfo(
        title = res(R.string.lz_space_exp_title, emptyArray()),
        body = res(R.string.lz_space_exp_body, arrayOf(spaceName, userId, dualCount)),
        dismiss = res(R.string.lz_space_exp_dismiss, emptyArray()),
    )
