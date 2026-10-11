package com.example.musictheory.ui.common
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
private val NAMES = arrayOf("C","C♯","D","D♯","E","F","F♯","G","G♯","A","A♯","B")
fun keyLabel(i: Int): String = NAMES[i % 12] + (4 + i / 12)
@Composable
fun KeyPad12(count: Int = 12, selected: Set<Int>, locked: Set<Int> = emptySet(),
             onToggle: (Int) -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Row(modifier.horizontalScroll(rememberScrollState())) {
        (0 until count).forEach { i -> val sel = i in selected; val lock = i in locked
            Button(onClick = { if (!lock) onToggle(i) }, enabled = !lock, shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when { lock -> cs.secondary; sel -> cs.primary; else -> Color.White },
                    contentColor = if (sel || lock) Color.White else Color.Black,
                    disabledContainerColor = cs.secondary, disabledContentColor = Color.White),
                modifier = Modifier.padding(horizontal = 2.dp)
                    .width(if (count > 12) 44.dp else 48.dp).height(110.dp)) {
                Text(keyLabel(i), fontSize = 11.sp, maxLines = 1) } } }
}
