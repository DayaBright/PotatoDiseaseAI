package com.tesis.potatodiseaseai.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun HelpScreen(innerPadding: PaddingValues) {
    Text(
        text = "Pantalla de Ayuda",
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    )
}