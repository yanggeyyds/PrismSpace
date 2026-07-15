package com.yzddmr6.prismspace.prism.compose.vm

import android.content.Context
import com.yzddmr6.prismspace.mobile.R
import com.yzddmr6.prismspace.util.PrismLocale

/** Unified feedback-channel currency. */
data class ActionFeedback(val message: String, val isError: Boolean)

/**
 * Resolver from a string-resource id (+ optional format args) to a localized string.
 *
 * Production callers pass a lambda backed by `PrismLocale.wrap(context)::getString`, so the
 * copy follows the user's chosen language. Pure unit tests pass [zhFallback] (or omit the
 * argument, which defaults to it) to keep asserting the verbatim Chinese strings.
 */
typealias StringResolver = (Int, Array<out Any>) -> String

/**
 * Default resolver used by pure unit tests and any caller without a Context.
 * Returns Simplified-Chinese copy formatted with the given args.
 */
val zhFallback: StringResolver = { id, args -> String.format(zhTemplate(id), *args) }

/**
 * Build a locale-aware [StringResolver] backed by the user's chosen language. Resolves each id
 * through `PrismLocale.wrap(context)` so the copy follows the in-app language override.
 */
fun prismResolver(context: Context): StringResolver =
    { id, args -> PrismLocale.wrap(context).getString(id, *args) }

private fun zhTemplate(id: Int): String = when (id) {
    R.string.lz_vm_chip_frozen -> "已冻结"
    R.string.lz_vm_chip_running -> "运行中"
    R.string.lz_vm_chip_cloned -> "已双开"
    R.string.lz_vm_chip_not_cloned -> "未双开"
    R.string.lz_vm_batch_progress -> "正在处理 %1\$d/%2\$d…"
    R.string.lz_vm_creating_space -> "正在创建双开空间…"
    R.string.lz_vm_deleting_space -> "正在删除双开空间…"
    R.string.lz_vm_default_space_name -> "双开空间"
    R.string.lz_vm_files_imported -> "已导入 %1\$d 个文件"
    R.string.lz_vm_files_import_all_failed -> "导入失败：%1\$d 个文件未能导入"
    R.string.lz_vm_files_import_partial -> "导入完成：成功 %1\$d，失败 %2\$d"
    R.string.lz_vm_files_import_detailed -> "导入完成：成功 %1\$d，超限跳过 %2\$d，失败 %3\$d"
    R.string.lz_vm_batch_freeze_ok -> "已冻结 %1\$d 个应用"
    R.string.lz_vm_batch_freeze_partial -> "冻结完成: 成功 %1\$d, 失败 %2\$d"
    R.string.lz_vm_batch_uninstall_ok -> "正在卸载 %1\$d 个应用"
    R.string.lz_vm_batch_uninstall_partial -> "卸载发起: 成功 %1\$d, 失败 %2\$d"
    R.string.lz_vm_batch_clone_ok -> "正在克隆 %1\$d 个应用到双开空间"
    R.string.lz_vm_batch_clone_partial -> "克隆发起: 成功 %1\$d, 失败 %2\$d"
    R.string.lz_vm_create_success -> "已创建新的双开空间"
    R.string.lz_vm_create_root_unavailable -> "需要 Root 权限才能创建空间，请先在设置中启用 Root"
    R.string.lz_vm_create_cap_reached -> "已达本设备空间上限（最多 %1\$d 个用户），无法再创建"
    R.string.lz_vm_create_managed_profile_limit -> "本设备系统仅允许一个双开空间（已达系统工作资料上限），无法再创建"
    R.string.lz_vm_create_failed -> "创建空间失败：%1\$s，可重试"
    R.string.lz_vm_delete_success -> "已删除该双开空间"
    R.string.lz_vm_delete_root_unavailable -> "普通模式无法直接删除双开空间，请在系统设置中移除工作资料；启用 Root 后可一键删除。"
    R.string.lz_vm_delete_failed -> "删除空间失败：%1\$s，空间未被破坏，可重试"
    R.string.lz_vm_unknown_error -> "未知错误"
    R.string.lz_vm_destroy_in_progress -> "正在删除双开空间…"
    R.string.lz_vm_destroy_not_profile_owner -> "无管理权限，需在系统设置中手动移除双开空间"
    R.string.lz_vm_destroy_failed -> "删除失败：%1\$s，空间未被破坏，可重试"
    R.string.lz_vm_launch_space_not_ready -> "双开空间尚未就绪，请在设置中检查空间状态或权限模式"
    R.string.lz_vm_launch_app_missing -> "%1\$s 在该空间没有可启动入口"
    R.string.lz_vm_launch_denied -> "无权限访问该空间，请检查权限模式"
    R.string.lz_vm_launch_unknown -> "未能启动 %1\$s：%2\$s"
    R.string.lz_vm_mode_normal_title -> "普通模式"
    R.string.lz_vm_mode_normal_status -> "可用"
    R.string.lz_vm_mode_normal_summary -> "双开空间、应用打开、冻结、暂停和基础文件导入导出不需要 Root。"
    R.string.lz_vm_mode_shizuku_title -> "Shizuku 模式"
    R.string.lz_vm_mode_shizuku_summary_ready -> "可用于增强复制、后台限制和更多系统查询。"
    R.string.lz_vm_mode_shizuku_summary_waiting -> "Shizuku 已连接，仍需要授权 PrismSpace。"
    R.string.lz_vm_mode_shizuku_summary_not_running -> "启动 Shizuku 或无线调试后可启用增强能力。"
    R.string.lz_vm_mode_shizuku_summary_not_installed -> "未检测到 Shizuku；普通双开功能仍可使用。"
    R.string.lz_vm_mode_dhizuku_title -> "Dhizuku 模式"
    R.string.lz_vm_mode_dhizuku_summary_ready -> "已共享设备所有者权限，可使用增强克隆。"
    R.string.lz_vm_mode_dhizuku_summary_waiting -> "Dhizuku 已激活，仍需要授权 PrismSpace。"
    R.string.lz_vm_mode_dhizuku_summary_not_activated -> "将 Dhizuku 激活为设备所有者后可启用增强能力。"
    R.string.lz_vm_mode_dhizuku_summary_not_installed -> "未检测到 Dhizuku；普通双开功能仍可使用。"
    R.string.lz_vm_mode_root_title -> "Root 模式"
    R.string.lz_vm_mode_root_summary -> "Root 仅用于高级诊断和兜底维护，不是普通双开功能的依赖。"
    R.string.lz_vm_shizuku_status_not_installed -> "未安装"
    R.string.lz_vm_shizuku_status_not_running -> "未运行"
    R.string.lz_vm_shizuku_status_waiting -> "等待授权"
    R.string.lz_vm_shizuku_status_ready -> "可用"
    R.string.lz_vm_dhizuku_status_not_installed -> "未安装"
    R.string.lz_vm_dhizuku_status_not_activated -> "未激活"
    R.string.lz_vm_dhizuku_status_waiting -> "等待授权"
    R.string.lz_vm_dhizuku_status_ready -> "可用"
    R.string.lz_vm_root_status_not_detected -> "未检测"
    R.string.lz_vm_root_status_available_disabled -> "可用但关闭"
    R.string.lz_vm_root_status_enabled -> "已启用"
    // SettingsViewModel mapSettingsUiModel (pure, tested) mode card
    R.string.lz_setvm_mode_title -> "当前模式"
    R.string.lz_setvm_mode_body_not_created -> "双开空间未创建，请先完成初始设置。"
    R.string.lz_setvm_mode_body_shizuku -> "Shizuku 模式可用，增强能力已启用。"
    R.string.lz_setvm_mode_body_dhizuku -> "Dhizuku 模式可用，设备所有者权限已启用。"
    R.string.lz_setvm_mode_body_normal -> "普通模式运行中，核心双开功能可用。"
    else -> ""
}

/** Pure: Files import success/failure counts → user feedback. */
fun filesImportFeedback(success: Int, failed: Int, res: StringResolver = zhFallback): ActionFeedback = when {
    success == 0 && failed == 0 -> ActionFeedback("", false)
    failed == 0 -> ActionFeedback(res(R.string.lz_vm_files_imported, arrayOf(success)), false)
    success == 0 -> ActionFeedback(res(R.string.lz_vm_files_import_all_failed, arrayOf(failed)), true)
    else -> ActionFeedback(res(R.string.lz_vm_files_import_partial, arrayOf(success, failed)), true)
}

/**
 * Pure: detailed Files import outcome. For the common no-oversize path it
 * delegates to [filesImportFeedback]; the oversize case adds specific copy.
 */
fun filesImportFeedbackDetailed(ok: Int, oversize: Int, otherFail: Int, res: StringResolver = zhFallback): ActionFeedback =
    if (oversize == 0) filesImportFeedback(ok, otherFail, res)
    else ActionFeedback(res(R.string.lz_vm_files_import_detailed, arrayOf(ok, oversize, otherFail)), isError = true)

/**
 * Pure: batch-action feedback shared by SpaceViewModel and tests.
 * failed == failures.size; isError == failures.isNotEmpty().
 */
fun batchActionFeedback(action: BatchAction, succeeded: Int, failed: Int, res: StringResolver = zhFallback): ActionFeedback {
    val msg = when (action) {
        BatchAction.Freeze ->
            if (failed == 0) res(R.string.lz_vm_batch_freeze_ok, arrayOf(succeeded))
            else res(R.string.lz_vm_batch_freeze_partial, arrayOf(succeeded, failed))
        BatchAction.Uninstall ->
            if (failed == 0) res(R.string.lz_vm_batch_uninstall_ok, arrayOf(succeeded))
            else res(R.string.lz_vm_batch_uninstall_partial, arrayOf(succeeded, failed))
        BatchAction.CopyToDual ->
            if (failed == 0) res(R.string.lz_vm_batch_clone_ok, arrayOf(succeeded))
            else res(R.string.lz_vm_batch_clone_partial, arrayOf(succeeded, failed))
    }
    return ActionFeedback(msg, isError = failed > 0)
}
