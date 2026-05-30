package com.yzddmr6.prismspace.clone

import android.graphics.drawable.Drawable
import android.os.UserHandle
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.yzddmr6.prismspace.controller.PrismAppClones
import com.yzddmr6.prismspace.mobile.R
import com.yzddmr6.prismspace.controller.PrismAppClones.Companion.AppCloneMode
import com.yzddmr6.prismspace.prism.compose.theme.PrismTheme
import com.yzddmr6.prismspace.util.UserHandles

/** One install-method option in the M3 clone selector. [mode] is a @AppCloneMode int. */
data class CloneModeOption(
    val mode: Int,
    val title: String,
    /** Method description when [available]; the grey-out reason when not. */
    val summary: String,
    val available: Boolean,
    /** Show a "去启用" guide button (Shizuku / Root, which the user can enable in Settings). */
    val showEnableGuide: Boolean,
)

/**
 * Lists all install methods (file-sync / Play / Shizuku / Root) as a mutually-exclusive,
 * non-cancelable single-select. Unavailable methods are greyed out with the reason and,
 * for Shizuku/Root, a "去启用" button that jumps to the run-mode settings. Copy direction is one-way 主→双,
 * so only dual-space targets are listed.
 */
class AppClonesBottomSheet(
    private val targets: Map<UserHandle, String>, private val icons: Map<UserHandle, Drawable>?,
    private val isCloned: Function1<UserHandle, Boolean>, private val clone: Function2<UserHandle, @AppCloneMode Int, Unit>) {

    @Suppress("unused") // For Compose Preview
    constructor(): this(mapOf(UserHandles.of(13) to "PrismSpace"), null, { false }, { _, _ -> })

    @Composable @Preview
    fun compose(options: List<CloneModeOption>, selectedMode: MutableState<Int>, onGuide: () -> Unit) {
        PrismTheme {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = stringResource(R.string.lz_app_choose_install_method), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface)
                    options.forEach { opt -> CloneModeRow(opt, selectedMode, onGuide) }

                    Spacer(Modifier.height(14.dp))
                    if (targets.size == 1) {
                        // Single dual space (the common case): one clear primary CTA instead of a
                        // tap-the-row-to-commit "复制到" list (which read like a settings label, not a button).
                        val (user, _) = targets.entries.first()
                        val cloned = isCloned(user)
                        Button(
                            onClick = {
                                Log.i(TAG, "Clone CTA tapped user=${user.hashCode()} mode=${selectedMode.value} cloned=$cloned")
                                if (! cloned) clone(user, selectedMode.value)
                            },
                            enabled = ! cloned,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.lz_app_clone_to_dual_space)) }
                    } else {
                        // Multiple dual spaces (experimental): pick which one to clone into.
                        Text(text = stringResource(R.string.lz_app_copy_to), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        for ((user, label) in targets) {
                            val cloned = isCloned(user)
                            TargetRow(label, icons?.get(user), cloned) { if (! cloned) clone(user, selectedMode.value) }
                        }
                    }
                }
            }
        }
    }
}

private const val TAG = "Prism.CloneSheet"

@Composable private fun CloneModeRow(opt: CloneModeOption, selectedMode: MutableState<Int>, onGuide: () -> Unit) {
    val titleColor = MaterialTheme.colorScheme.onSurface.let { if (opt.available) it else it.copy(alpha = 0.38f) }
    val summaryColor = MaterialTheme.colorScheme.onSurfaceVariant.let { if (opt.available) it else it.copy(alpha = 0.38f) }
    Row(modifier = Modifier.fillMaxWidth()
        .clickable(enabled = opt.available) { selectedMode.value = opt.mode }
        .padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selectedMode.value == opt.mode, enabled = opt.available,
            onClick = { if (opt.available) selectedMode.value = opt.mode })
        Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
            Text(text = opt.title, style = MaterialTheme.typography.bodyLarge, color = titleColor)
            Text(text = opt.summary, style = MaterialTheme.typography.bodySmall, color = summaryColor)
        }
        if (! opt.available && opt.showEnableGuide) TextButton(onClick = onGuide) { Text(stringResource(R.string.lz_app_enable_guide)) }
    }
}

@Composable private fun TargetRow(label: String, icon: Drawable?, cloned: Boolean, onClick: () -> Unit) {
    val color = MaterialTheme.colorScheme.onSurface.let { if (cloned) it.copy(alpha = 0.38f) else it }
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
        .clickable(enabled = ! cloned, onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        val bmp = icon?.toBitmap()?.asImageBitmap()
        if (bmp != null) Image(bmp, null, modifier = Modifier.size(28.dp))
        Text(text = if (cloned) stringResource(R.string.lz_app_target_already_in_space, label) else label, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}
