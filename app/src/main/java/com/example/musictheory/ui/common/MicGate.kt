package com.example.musictheory.ui.common
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.musictheory.util.safeStart
@Composable
fun MicGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context,
        Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    var asked by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it; asked = true }
    if (granted) { content(); return }
    Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(80.dp)); Text("🎤", fontSize = 52.sp)
        Text("Your voice is the instrument", fontSize = 22.sp, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 14.dp))
        Text("Harmonia listens to grade your pitch. Audio is analysed live, in memory only — never recorded, stored, or sent anywhere. No mic = no problem; every other feature stays fully playable.",
            fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
        Button(onClick = { launcher.launch(Manifest.permission.RECORD_AUDIO) },
            shape = RoundedCornerShape(14.dp), modifier = Modifier.padding(top = 22.dp)) {
            Text("Allow microphone", fontWeight = FontWeight.Bold) }
        if (asked && !granted) {
            Text("Permission denied — enable it in system settings.", fontSize = 13.sp,
                color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 10.dp))
            OutlinedButton(onClick = { context.safeStart(
                Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", context.packageName, null)), "Couldn't open settings") },
                modifier = Modifier.padding(top = 8.dp)) { Text("Open settings") } } }
}
