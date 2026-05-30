package com.yzddmr6.prismspace.setup

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.yzddmr6.prismspace.util.PrismLocale
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import com.yzddmr6.prismspace.setup.compose.PrismSetupScreen
import com.yzddmr6.prismspace.setup.compose.SetupController
import com.yzddmr6.prismspace.setup.compose.SetupStateViewModel

/**
 * PrismSpace setup activity.
 *
 * Compose-only ComponentActivity. UI state lives in [SetupStateViewModel] so the
 * error dialog and the `incompleteSetupAcked` flag survive rotation /
 * dark-mode toggle. The ActivityResultLauncher is owned by the Activity
 * (must register before STARTED) and dispatches into the retained VM.
 *
 * Business logic — DPM prerequisite checks, managed-provisioning intent build,
 * result handling — stays in [SetupViewModel], which is separate from
 * lifecycle UI state in [SetupStateViewModel].
 */
class SetupActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) = super.attachBaseContext(PrismLocale.wrap(newBase))

    private val stateVm: SetupStateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val launcher = registerForActivityResult(StartActivityForResult()) { result ->
            SetupController.handleProvisionResult(this, stateVm, result.resultCode)
        }
        val controller = SetupController(this, stateVm, launcher)
        setContent { PrismSetupScreen(controller) }
    }
}
