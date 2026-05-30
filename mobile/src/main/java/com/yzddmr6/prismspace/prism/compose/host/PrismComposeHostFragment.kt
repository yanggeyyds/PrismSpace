package com.yzddmr6.prismspace.prism.compose.host

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.compose.rememberNavController
import com.yzddmr6.prismspace.prism.compose.nav.PrismNavHost
import com.yzddmr6.prismspace.prism.compose.theme.PrismTheme

class PrismComposeHostFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            PrismTheme {
                PrismNavHost(rememberNavController())
            }
        }
    }
}
