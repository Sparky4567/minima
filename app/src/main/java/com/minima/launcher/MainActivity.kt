package com.minima.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.minima.launcher.data.LauncherViewModel
import com.minima.launcher.ui.HomeScreen
import com.minima.launcher.ui.theme.MinimaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MinimaTheme {
                val vm: LauncherViewModel =
                    viewModel(factory = LauncherViewModel.factory(this))
                HomeScreen(vm = vm, activity = this)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh app list when returning (installs/uninstalls).
        // ViewModel is retained; simplest hook is here via recreation-safe call.
    }
}
