package com.yzddmr6.prismspace.prism.model

import com.yzddmr6.prismspace.mobile.R
import com.yzddmr6.prismspace.prism.compose.vm.StringResolver
import com.yzddmr6.prismspace.prism.compose.vm.zhFallback

data class PrismSettingsModeState(
    val normal: PrismModeCard,
    val shizukuAdb: PrismModeCard,
    val root: PrismModeCard,
) {
    companion object {
        fun from(
            shizuku: PrismShizukuAdbStatus,
            root: PrismRootStatus,
            res: StringResolver = zhFallback,
        ): PrismSettingsModeState = PrismSettingsModeState(
            normal = PrismModeCard(
                title = res(R.string.lz_vm_mode_normal_title, emptyArray()),
                status = res(R.string.lz_vm_mode_normal_status, emptyArray()),
                summary = res(R.string.lz_vm_mode_normal_summary, emptyArray()),
            ),
            shizukuAdb = PrismModeCard(
                title = res(R.string.lz_vm_mode_shizuku_title, emptyArray()),
                status = res(shizuku.labelRes, emptyArray()),
                summary = res(
                    when (shizuku) {
                        PrismShizukuAdbStatus.Ready -> R.string.lz_vm_mode_shizuku_summary_ready
                        PrismShizukuAdbStatus.WaitingAuthorization -> R.string.lz_vm_mode_shizuku_summary_waiting
                        PrismShizukuAdbStatus.NotRunning -> R.string.lz_vm_mode_shizuku_summary_not_running
                        PrismShizukuAdbStatus.NotInstalled -> R.string.lz_vm_mode_shizuku_summary_not_installed
                    },
                    emptyArray(),
                ),
            ),
            root = PrismModeCard(
                title = res(R.string.lz_vm_mode_root_title, emptyArray()),
                status = res(root.labelRes, emptyArray()),
                summary = res(R.string.lz_vm_mode_root_summary, emptyArray()),
            ),
        )
    }
}

data class PrismModeCard(
    val title: String,
    val status: String,
    val summary: String,
)

enum class PrismShizukuAdbStatus(val labelRes: Int) {
    NotInstalled(R.string.lz_vm_shizuku_status_not_installed),
    NotRunning(R.string.lz_vm_shizuku_status_not_running),
    WaitingAuthorization(R.string.lz_vm_shizuku_status_waiting),
    Ready(R.string.lz_vm_shizuku_status_ready),
}

enum class PrismRootStatus(val labelRes: Int) {
    NotDetected(R.string.lz_vm_root_status_not_detected),
    AvailableButDisabled(R.string.lz_vm_root_status_available_disabled),
    Enabled(R.string.lz_vm_root_status_enabled),
}
