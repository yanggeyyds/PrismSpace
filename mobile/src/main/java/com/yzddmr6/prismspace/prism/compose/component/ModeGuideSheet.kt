package com.yzddmr6.prismspace.prism.compose.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.yzddmr6.prismspace.mobile.R
import com.yzddmr6.prismspace.prism.compose.theme.PrismSpacing
import com.yzddmr6.prismspace.prism.compose.vm.PrismMode

// ---------------------------------------------------------------------------
// ModeGuideSheet: bottom sheet for run-mode selection.
// Shows three options: 普通模式 / Shizuku-ADB / Root
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeGuideSheet(
    currentMode: PrismMode,
    onDismiss: () -> Unit,
    onSetNormal: () -> Unit,
    onCheckShizuku: () -> Boolean,
    onRequestRoot: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        ModeListPanel(
            currentMode = currentMode,
            onDismiss = onDismiss,
            onSetNormal = {
                onSetNormal()
                onDismiss()
            },
            onRequestRoot = {
                onRequestRoot()
                onDismiss()
            },
            onCheckShizuku = onCheckShizuku,
        )
    }
}

/** Trailing checkmark composable for the currently-active mode row (null otherwise). */
@Composable
private fun activeCheck(active: Boolean): (@Composable () -> Unit)? =
    if (!active) null else ({ Icon(PrismIcons.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) })

@Composable
private fun ModeListPanel(
    currentMode: PrismMode,
    onDismiss: () -> Unit,
    onSetNormal: () -> Unit,
    onRequestRoot: () -> Unit,
    onCheckShizuku: () -> Boolean,
) {
    // All three mode rows now share one contract: tap the row = activate that mode.
    //  • 普通模式  → always succeeds → switch + close.
    //  • Root     → request su; on grant switch (feedback via snackbar) + close.
    //  • Shizuku  → run the readiness check; if authorized switch + close (identical to 普通模式),
    //               otherwise PROGRESSIVELY reveal the setup guide instead of failing silently.
    // The guide is hidden by default (no always-on clutter) and appears only when a tap finds
    // Shizuku not ready — so the three rows look and behave consistently.
    var shizukuGuideVisible by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(horizontal = PrismSpacing.Lg).padding(bottom = PrismSpacing.Xl)) {
        Text(
            text = stringResource(R.string.lz_set_run_mode),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = PrismSpacing.Lg),
        )

        // 普通模式.
        GroupCard {
            ActionRow(
                title = stringResource(R.string.lz_set_mode_normal_title),
                summary = stringResource(R.string.lz_set_mode_normal_summary),
                leadingIcon = PrismIcons.Key,
                trailing = activeCheck(currentMode == PrismMode.Normal),
                onClick = onSetNormal,
            )
        }

        Spacer(Modifier.height(PrismSpacing.Sm))

        // Shizuku — tap attempts the switch (like the other rows). Ready → close; not ready → reveal guide.
        GroupCard {
            ActionRow(
                title = stringResource(R.string.lz_mode_shizuku),
                summary = stringResource(R.string.lz_set_mode_shizuku_summary),
                leadingIcon = PrismIcons.Adb,
                trailing = activeCheck(currentMode == PrismMode.Shizuku),
                onClick = {
                    if (onCheckShizuku()) onDismiss() else shizukuGuideVisible = true
                },
            )
        }

        // Inline Shizuku setup guide — revealed only after a tap finds Shizuku not ready.
        AnimatedVisibility(
            visible = shizukuGuideVisible,
            enter = fadeIn() + expandVertically(),
        ) {
            ShizukuGuideInline(onCheckShizuku = onCheckShizuku, onDismiss = onDismiss)
        }

        Spacer(Modifier.height(PrismSpacing.Sm))

        // Root.
        GroupCard {
            ActionRow(
                title = "Root",
                summary = stringResource(R.string.lz_set_mode_root_summary),
                leadingIcon = PrismIcons.Shield,
                trailing = activeCheck(currentMode == PrismMode.Root),
                onClick = onRequestRoot,
            )
        }
    }
}

@Composable
private fun ShizukuGuideInline(
    onCheckShizuku: () -> Boolean,
    onDismiss: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = PrismSpacing.Xs, vertical = PrismSpacing.Sm)) {
        Text(
            text = stringResource(R.string.lz_set_shizuku_intro),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = PrismSpacing.Sm),
        )
        Text(
            text = stringResource(R.string.lz_set_shizuku_step1),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.lz_set_shizuku_step2),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.lz_set_shizuku_step3),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = PrismSpacing.Md),
        )
        // Check Shizuku connection.
        // Uses same readiness check as PrismAppClones: Shizuku.getVersion()>=11 && checkSelfPermission()==GRANTED
        Button(
            onClick = {
                if (onCheckShizuku()) onDismiss()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.lz_set_shizuku_check_button))
        }
    }
}
