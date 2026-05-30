@file:Suppress("LongMethod", "MagicNumber")
package com.yzddmr6.prismspace.prism.compose.screen

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import android.widget.Toast
import com.yzddmr6.prismspace.mobile.R
import com.yzddmr6.prismspace.prism.compose.component.ActionRow
import com.yzddmr6.prismspace.prism.compose.component.GroupCard
import com.yzddmr6.prismspace.prism.compose.component.PrismIcons
import com.yzddmr6.prismspace.prism.compose.theme.PrismSpacing
import com.yzddmr6.prismspace.prism.compose.vm.FilesViewModel
import com.yzddmr6.prismspace.prism.service.FileBridgeService
import com.yzddmr6.prismspace.prism.service.TransferRecordActions
import com.yzddmr6.prismspace.prism.service.displayTitle
import com.yzddmr6.prismspace.prism.service.openSystemFileManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(nav: NavHostController) {
    val vm: FilesViewModel = viewModel()
    val context = LocalContext.current
    val activity = context as? Activity
    val history by vm.history.collectAsState()
    var showClearConfirm by remember { mutableStateOf(false) }

    // Refresh whenever the tab is shown (a transfer may have happened in another app meanwhile).
    LaunchedEffect(Unit) { vm.refresh() }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(stringResource(R.string.lz_pf_files_clear_confirm_title)) },
            // Make explicit it only clears the LOG, not the transferred files (users fear data loss).
            text = { Text(stringResource(R.string.lz_pf_files_clear_confirm_body)) },
            confirmButton = {
                TextButton(onClick = { showClearConfirm = false; vm.clearHistory() }) {
                    Text(stringResource(R.string.lz_pf_files_clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text(stringResource(R.string.lz_set_cancel)) }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.lz_pf_files_title)) },
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
            verticalArrangement = Arrangement.spacedBy(PrismSpacing.None),
        ) {
            // ── How to transfer files between spaces (the one unified mechanism: system share) ──
            Text(
                text = stringResource(R.string.lz_pf_files_transfer_heading),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = PrismSpacing.Xs, bottom = 10.dp),
            )
            GroupCard(title = null) {
                GuideStep(1, stringResource(R.string.lz_pf_files_step1))
                GuideStep(2, stringResource(R.string.lz_pf_files_step2))
                GuideStep(3, stringResource(R.string.lz_pf_files_step3))
                GuideStep(4, stringResource(R.string.lz_pf_files_step4))
            }
            Spacer(Modifier.height(PrismSpacing.Sm))
            Text(
                text = stringResource(R.string.lz_pf_files_tab_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(20.dp))

            // ── Persisted transfer history (survives app restart) ──
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = PrismSpacing.Sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.lz_pf_files_history_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (history.isNotEmpty()) {
                    TextButton(onClick = { showClearConfirm = true }) { Text(stringResource(R.string.lz_pf_files_clear)) }
                }
            }

            if (history.isNotEmpty()) {
                GroupCard(title = null) {
                    history.forEach { item ->
                        // Tap → open in the file manager for every record type. APK/app transfers add a
                        // separate Install action so the file-manager path stays generic.
                        val installableInProfile = item.packageName != null && activity != null
                        val actions = TransferRecordActions.forRecord(item, installableInProfile)
                        ActionRow(
                            title = item.displayTitle(),
                            summary = listOf(item.location.takeIf { it.isNotBlank() }, formatTime(item.timeMillis).takeIf { it.isNotBlank() })
                                .filterNotNull().joinToString(" · "),
                            leadingIcon = if (item.isImage) PrismIcons.Img else PrismIcons.File,
                            trailing = {
                                if (actions.canInstall && activity != null) {
                                    TextButton(onClick = {
                                        val openResult = FileBridgeService().openProfileInstallEntry(activity)
                                        if (!openResult.success) {
                                            Toast.makeText(context, openResult.message, Toast.LENGTH_LONG).show()
                                        }
                                    }) {
                                        Text(stringResource(R.string.lz_pf_install))
                                    }
                                } else {
                                    Icon(
                                        imageVector = PrismIcons.FileOpen,
                                        contentDescription = stringResource(R.string.lz_pf_open_action),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            },
                            onClick = {
                                if (item.packageName != null && activity != null) {
                                    FileBridgeService().openProfileDownloadsFolder(activity)
                                } else {
                                    openSystemFileManager(context)
                                }
                            },
                        )
                    }
                }
            } else {
                GroupCard(title = null) {
                    Text(
                        text = stringResource(R.string.lz_pf_files_history_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = PrismSpacing.Lg, vertical = 18.dp),
                    )
                }
            }

            Spacer(Modifier.height(PrismSpacing.Sm))
        }
    }
}

@Composable
private fun GuideStep(n: Int, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PrismSpacing.Lg, vertical = PrismSpacing.Sm),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "$n",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = PrismSpacing.Md),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun formatTime(millis: Long): String =
    if (millis <= 0L) "" else SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(millis))
