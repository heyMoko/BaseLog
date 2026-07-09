package com.mokostudio.baselog.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mokostudio.baselog.navigation.BaseLogNavHost

@Composable
fun BaseLogApp(modifier: Modifier = Modifier) {
    BaseLogNavHost(modifier = modifier)
}
