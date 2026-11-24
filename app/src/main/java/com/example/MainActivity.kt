package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.NetworkModule
import com.example.ui.TgzLifestealApp
import com.example.ui.theme.TgzLifestealTheme
import com.example.ui.viewmodel.TgzViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TgzLifestealTheme {
                val viewModel: TgzViewModel = viewModel(
                    factory = NetworkModule.viewModelFactory
                )
                TgzLifestealApp(viewModel = viewModel)
            }
        }
    }
}
