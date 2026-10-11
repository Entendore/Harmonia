package com.example.musictheory.ui.common
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musictheory.audio.TonePlayer
import com.example.musictheory.ui.theme.LocalAppDarkTheme
enum class OptState { IDLE, CORRECT, WRONG, MUTED }
@Composable fun correctColor() = if (LocalAppDarkTheme.current) Color(0xFF81C784) else Color(0xFF2E7D32)
@Composable fun wrongColor() = if (LocalAppDarkTheme.current) Color(0xFFE57373) else Color(0xFFC62828)
@Composable
fun GameScaffold(title: String, onBack: () -> Unit, scrollable: Boolean = true,
                 content: @Composable ColumnScope.() -> Unit) {
    DisposableEffect(Unit) { onDispose { TonePlayer.stopAll() } }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        val body = if (scrollable) Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                   else Modifier.fillMaxSize()
        Column(body.padding(horizontal = 16.dp)) { content() } }
}
@Composable fun ScoreRow(left: String, right: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(left, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(right, fontSize = 14.sp, fontWeight = FontWeight.Bold) } }
@Composable fun InfoCard(text: String) {
    Card(shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Text(text, Modifier.padding(18.dp), fontSize = 17.sp, fontWeight = FontWeight.Bold) } }
@Composable
fun OptionButton(text: String, state: OptState, enabled: Boolean = true,
                 modifier: Modifier = Modifier.fillMaxWidth(), onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val container = when (state) { OptState.IDLE -> cs.primaryContainer
        OptState.CORRECT -> correctColor(); OptState.WRONG -> wrongColor(); OptState.MUTED -> cs.surfaceVariant }
    val content = when (state) { OptState.CORRECT, OptState.WRONG -> Color.White
        OptState.MUTED -> cs.onSurfaceVariant.copy(alpha = 0.85f); OptState.IDLE -> cs.onPrimaryContainer }
    Button(onClick = onClick, enabled = enabled, shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content,
            disabledContainerColor = container, disabledContentColor = content), modifier = modifier) {
        Text(text, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 6.dp)) } }
@Composable fun FeedbackText(text: String, correct: Boolean?) { if (text.isEmpty()) return
    val color = when (correct) { true -> correctColor(); false -> wrongColor(); null -> MaterialTheme.colorScheme.primary }
    Text(text, color = color, fontSize = 15.sp, lineHeight = 21.sp, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) }
@Composable fun ResultPanel(headline: String, detail: String, onReplay: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🏁", fontSize = 52.sp)
        Text(headline, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center)
        Text(detail, textAlign = TextAlign.Center, fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp, bottom = 20.dp))
        Button(onClick = onReplay, shape = RoundedCornerShape(14.dp)) { Text("Play again") }
        Spacer(Modifier.height(24.dp)) } }
@Composable fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit,
                          modifier: Modifier = Modifier.fillMaxWidth()) {
    Row(modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f)); Switch(checked = checked, onCheckedChange = null) } }
