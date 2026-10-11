package com.example.musictheory.ui
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.musictheory.data.PathsData
@Composable
fun OnboardingOverlay(onPick: (String) -> Unit, onSkip: () -> Unit) {
    Dialog(onDismissRequest = onSkip, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(70.dp)); Text("🎵", fontSize = 56.sp)
                Text("Welcome to Harmonia", fontSize = 28.sp, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
                Text("Music theory across cultures — learn by reading, listening and playing.",
                    fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                Text("What draws you in first?", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 34.dp))
                listOf("🏛  Western classical" to "western", "🪷  Ragas & maqam" to "raga",
                    "🥁  World rhythm" to "rhythm", "🌍  All of it!" to "western").forEach { (label, key) ->
                    Button(onClick = { onPick(PathsData.suggestion[key] ?: "western_foundations") },
                        shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                        Text(label, fontWeight = FontWeight.Bold) } }
                TextButton(onClick = onSkip, modifier = Modifier.padding(top = 14.dp)) {
                    Text("Skip — just explore") } } } }
}
