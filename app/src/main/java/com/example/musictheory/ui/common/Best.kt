package com.example.musictheory.ui.common
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.musictheory.data.SettingsRepo
@Composable fun rememberBest(key: String): Int { val c = LocalContext.current
    return remember(key) { SettingsRepo.bestFlow(c, key) }.collectAsState(initial = 0).value }
