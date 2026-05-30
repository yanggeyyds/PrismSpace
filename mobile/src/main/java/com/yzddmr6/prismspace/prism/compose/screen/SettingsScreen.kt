package com.yzddmr6.prismspace.prism.compose.screen

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.yzddmr6.prismspace.prism.compose.component.AboutSheet
import com.yzddmr6.prismspace.prism.compose.component.ActionRow
import com.yzddmr6.prismspace.prism.compose.component.DeleteFinalSheet
import com.yzddmr6.prismspace.prism.compose.component.DeleteWarningSheet
import com.yzddmr6.prismspace.prism.compose.component.GroupCard
import com.yzddmr6.prismspace.prism.compose.component.ModeGuideSheet
import com.yzddmr6.prismspace.prism.compose.component.NavRow
import com.yzddmr6.prismspace.prism.compose.component.PrismIcons
import com.yzddmr6.prismspace.prism.compose.component.RepairConfirmSheet
import com.yzddmr6.prismspace.prism.compose.component.SwitchRow
import com.yzddmr6.prismspace.prism.compose.theme.PrismSpacing
import com.yzddmr6.prismspace.mobile.R
import com.yzddmr6.prismspace.prism.compose.vm.PrismMode
import com.yzddmr6.prismspace.prism.compose.vm.prismModeLabelRes
import com.yzddmr6.prismspace.prism.compose.vm.SettingsViewModel
import com.yzddmr6.prismspace.util.Activities
import com.yzddmr6.prismspace.util.PrismLocale

// ---------------------------------------------------------------------------
// SettingsScreen: run mode, space controls, diagnostics, update, language and about.
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(nav: NavHostController) {
    val vm: SettingsViewModel = viewModel()
    val uiState by vm.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) { vm.refreshCapabilities() }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.refreshCapabilities()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Sheet visibility state
    var showModeSheet by remember { mutableStateOf(false) }
    var showAboutSheet by remember { mutableStateOf(false) }
    var showRepairConfirm by remember { mutableStateOf(false) }
    var showDeleteWarning by remember { mutableStateOf(false) }
    var showDeleteFinal by remember { mutableStateOf(false) }
    var showLangDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.lz_set_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PrismSpacing.Lg, vertical = PrismSpacing.Sm),
            verticalArrangement = Arrangement.spacedBy(PrismSpacing.Md),
        ) {

            // modeLabel derives from selectedMode, not transient capability detection.
            GroupCard(title = null) {
                // Single label source avoids Home/Settings drift.
                val modeLabel = stringResource(prismModeLabelRes(uiState?.selectedMode ?: PrismMode.Normal))
                NavRow(
                    title = stringResource(R.string.lz_set_run_mode),
                    summary = modeLabel,
                    leadingIcon = PrismIcons.Key,
                    onClick = { showModeSheet = true },
                )
            }

            // Space suspend and repair actions.
            GroupCard(title = null) {
                val suspended = uiState?.spaceSuspended ?: false
                SwitchRow(
                    title = stringResource(R.string.lz_set_suspend_title),
                    summary = stringResource(R.string.lz_set_suspend_summary),
                    leadingIcon = PrismIcons.Snow,
                    checked = suspended,
                    onCheckedChange = { vm.suspendSpace(it) },
                )
                ActionRow(
                    title = uiState?.spaceActionTitle?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.lz_set_repair_title),
                    summary = uiState?.spaceActionSummary?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.lz_set_repair_summary),
                    leadingIcon = PrismIcons.Wrench,
                    onClick = {
                        if (uiState?.spaceActionNeedsConfirmation == true) showRepairConfirm = true
                        else vm.repairSpace(context)
                    },
                )
                if (uiState?.profileOwnerReady == true) {
                    ActionRow(
                        title = stringResource(R.string.lz_set_delete_space_title),
                        summary = stringResource(R.string.lz_set_delete_space_summary),
                        leadingIcon = PrismIcons.Trash,
                        danger = true,
                        onClick = { showDeleteWarning = true },
                    )
                }
            }

            // Diagnostic log export.
            GroupCard(title = null) {
                ActionRow(
                    title = stringResource(R.string.lz_set_export_logs_title),
                    summary = stringResource(R.string.lz_set_export_logs_summary),
                    leadingIcon = Icons.Outlined.IosShare,
                    onClick = { vm.exportLogs(context) },
                )
            }

            // GitHub Releases update check.
            GroupCard(title = null) {
                ActionRow(
                    title = stringResource(R.string.lz_set_check_update_title),
                    summary = stringResource(R.string.lz_set_check_update_summary),
                    leadingIcon = PrismIcons.Refresh,
                    onClick = { vm.checkForUpdate() },
                )
            }

            // Language override.
            GroupCard(title = null) {
                val curLang = PrismLocale.getStored(context)
                val langLabel = when (curLang) {
                    PrismLocale.ZH -> stringResource(R.string.prism_language_zh)
                    PrismLocale.EN -> stringResource(R.string.prism_language_en)
                    else -> stringResource(R.string.prism_language_system)
                }
                NavRow(
                    title = stringResource(R.string.prism_settings_language),
                    summary = langLabel,
                    leadingIcon = Icons.Outlined.Language,
                    onClick = { showLangDialog = true },
                )
            }

            // About PrismSpace.
            GroupCard(title = null) {
                NavRow(
                    title = stringResource(R.string.lz_set_about_title),
                    summary = stringResource(R.string.lz_set_about_summary),
                    leadingIcon = PrismIcons.Info,
                    onClick = { showAboutSheet = true },
                )
            }
        }
    }

    // ── Mode guide sheet ────────────────────────────────────────────────────
    if (showModeSheet) {
        ModeGuideSheet(
            currentMode = uiState?.selectedMode ?: PrismMode.Normal,
            onDismiss = { showModeSheet = false },
            onSetNormal = { vm.setNormalMode() },
            onCheckShizuku = { vm.checkShizuku() },
            onRequestRoot = { vm.requestRoot() },
        )
    }

    // ── About sheet ─────────────────────────────────────────────────────────
    if (showAboutSheet) {
        val versionText = remember(context) {
            try {
                val pi = context.packageManager.getPackageInfo(context.packageName, 0)
                val vc = if (Build.VERSION.SDK_INT >= 28) pi.longVersionCode else pi.versionCode.toLong()
                "v${pi.versionName ?: "?"} ($vc)"
            } catch (_: Exception) { "v?" }
        }
        AboutSheet(
            versionText = versionText,
            packageName = context.packageName,
            onDismiss = { showAboutSheet = false },
        )
    }

    if (showRepairConfirm) {
        RepairConfirmSheet(
            onConfirm = { showRepairConfirm = false; vm.repairSpace(context) },
            onDismiss = { showRepairConfirm = false },
        )
    }
    if (showDeleteWarning) {
        DeleteWarningSheet(
            onContinue = {
                showDeleteWarning = false
                showDeleteFinal = true
            },
            onDismiss = { showDeleteWarning = false },
        )
    }
    if (showDeleteFinal) {
        DeleteFinalSheet(
            onConfirm = {
                showDeleteFinal = false
                vm.deleteDualSpace(Activities.findActivityFrom(context))
            },
            onDismiss = { showDeleteFinal = false },
        )
    }

    // ── Language picker ───────────────────────────────────────────────────────
    if (showLangDialog) {
        val current = PrismLocale.getStored(context)
        val options = listOf(
            PrismLocale.SYSTEM to stringResource(R.string.prism_language_system),
            PrismLocale.ZH to stringResource(R.string.prism_language_zh),
            PrismLocale.EN to stringResource(R.string.prism_language_en),
        )
        AlertDialog(
            onDismissRequest = { showLangDialog = false },
            title = { Text(stringResource(R.string.prism_settings_language)) },
            text = {
                Column {
                    options.forEach { (tag, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showLangDialog = false
                                    if (tag != current) {
                                        PrismLocale.setStored(context, tag)
                                        (context as? android.app.Activity)?.recreate()
                                    }
                                }
                                .padding(vertical = PrismSpacing.Md),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = tag == current, onClick = null)
                            Spacer(Modifier.width(PrismSpacing.Md))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showLangDialog = false }) { Text(stringResource(R.string.lz_set_cancel)) } },
        )
    }

    // ── update-available dialog ───────────────────────────────────────────────
    uiState?.updateInfo?.let { info ->
        AlertDialog(
            onDismissRequest = { vm.dismissUpdate() },
            title = { Text(stringResource(R.string.lz_set_update_title, info.version)) },
            text = { Text(info.notes) },
            confirmButton = {
                TextButton(onClick = {
                    runCatching {
                        context.startActivity(
                            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(info.url))
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                    vm.dismissUpdate()
                }) { Text(stringResource(R.string.lz_set_update_download)) }
            },
            dismissButton = { TextButton(onClick = { vm.dismissUpdate() }) { Text(stringResource(R.string.lz_set_update_later)) } },
        )
    }

}
