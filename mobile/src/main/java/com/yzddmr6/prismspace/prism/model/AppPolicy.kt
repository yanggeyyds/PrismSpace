package com.yzddmr6.prismspace.prism.model

data class AppPolicy(
    val fileAccess: FileAccessMode = FileAccessMode.Isolated,
    val background: BackgroundPolicy = BackgroundPolicy.SystemDefault,
    val network: NetworkPolicy = NetworkPolicy.SystemDefault,
    val notifications: NotificationPolicy = NotificationPolicy.SystemDefault,
    val crossSpaceOpen: CrossSpaceOpenPolicy = CrossSpaceOpenPolicy.AskEveryTime,
    val restrictedPermissions: Set<PermissionRestriction> = emptySet(),
) {
    val permissionsRestricted: Int get() = restrictedPermissions.size
    val fileAccessSummary: String get() = fileAccess.label
    val backgroundSummary: String get() = background.label
    val networkSummary: String get() = network.label
    val crossSpaceOpenSummary: String get() = crossSpaceOpen.label
}

enum class FileAccessMode(val label: String) {
    Isolated("隔离"),
    ImportExportOnly("仅导入导出"),
    SharedMedia("共享媒体"),
    SharedFolder("共享文件夹"),
}

enum class BackgroundPolicy(val label: String) {
    SystemDefault("系统默认"),
    FreezeWhenIdle("空闲冻结"),
    Suspended("暂停运行"),
    BlockBackground("限制后台"),
}

enum class NetworkPolicy(val label: String) {
    SystemDefault("系统默认"),
    BlockBackground("限制后台联网"),
    BlockAll("禁止联网"),
}

enum class NotificationPolicy(val label: String) {
    SystemDefault("系统默认"),
    HideInCloneSpace("隐藏双开通知"),
    Allow("允许通知"),
}

enum class CrossSpaceOpenPolicy(val label: String) {
    AskEveryTime("每次询问"),
    PreferMain("优先主空间"),
    PreferClone("优先双开空间"),
}

enum class PermissionRestriction {
    Contacts,
    Location,
    Camera,
    Microphone,
    Photos,
    Files,
}

enum class PolicyAction {
    Freeze,
    Suspend,
    ImportExportFiles,
    RestrictBackground,
    RestrictNetwork,
    HideNotifications,
    ConfigureSharedMedia,
}

object AppPolicyPlanner {

    fun availability(action: PolicyAction, state: CapabilityState): CapabilityAvailability = when (action) {
        PolicyAction.Freeze,
        PolicyAction.Suspend -> if (state.profileOwner is CapabilityAvailability.Available)
            CapabilityAvailability.Available
        else CapabilityAvailability.NeedsSetup("需要工作资料管理")

        PolicyAction.ImportExportFiles -> if (state.normal is CapabilityAvailability.Available)
            CapabilityAvailability.Available
        else CapabilityAvailability.NeedsSetup("需要普通模式")

        PolicyAction.RestrictBackground,
        PolicyAction.RestrictNetwork,
        PolicyAction.HideNotifications,
        PolicyAction.ConfigureSharedMedia -> enhancedAvailability(state)
    }

    private fun enhancedAvailability(state: CapabilityState): CapabilityAvailability = when {
        state.shizuku is CapabilityAvailability.Available -> CapabilityAvailability.Available
        state.adb is CapabilityAvailability.Available -> CapabilityAvailability.Available
        state.root is CapabilityAvailability.Available -> CapabilityAvailability.Available
        state.root is CapabilityAvailability.AvailableButDisabled -> CapabilityAvailability.NeedsSetup("需要 Shizuku/ADB；Root 仅作为兜底")
        else -> CapabilityAvailability.NeedsSetup("需要 Shizuku/ADB")
    }
}
