package com.tesis.potatodiseaseai.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tesis.potatodiseaseai.R

@Composable
fun HelpScreen(innerPadding: PaddingValues) {
    Text(
        text = stringResource(R.string.help_title),
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    )
}