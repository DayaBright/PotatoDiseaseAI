package com.tesis.potatodiseaseai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tesis.potatodiseaseai.ui.theme.PotatoDiseaseAITheme
import com.tesis.potatodiseaseai.ui.navigation.MainNavigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PotatoDiseaseAITheme {
                MainNavigation()
            }
        }
    }
}