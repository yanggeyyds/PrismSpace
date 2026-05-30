package com.yzddmr6.prismspace;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Thin trampoline that holds the MAIN+LAUNCHER intent-filter and forwards to MainActivity.
 * Reset-to-Home semantics live in MainActivity.startMainUi() (gated on savedInstanceState==null)
 * so launcher taps on an already-running task resume the last visited tab.
 */
public class LauncherActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivity(new Intent(this, MainActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        finish();
    }
}
