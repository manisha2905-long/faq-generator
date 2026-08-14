package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.FaqScreen
import com.example.ui.theme.FAQGeneratorTheme
import com.example.ui.viewmodel.FaqViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: FaqViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FAQGeneratorTheme {
                FaqScreen(viewModel = viewModel)
            }
        }
    }
}
