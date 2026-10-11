package com.example.musictheory.ui.common
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musictheory.util.tap
@Composable
fun WorldKeyboard(freqs: List<Double>, labels: List<String>, onKey: (Int, Double) -> Unit,
                  modifier: Modifier = Modifier) {
    val view = LocalView.current
    Row(modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.Bottom) {
        freqs.forEachIndexed { i, f ->
            Button(onClick = { view.tap(); onKey(i, f) }, shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                modifier = Modifier.padding(horizontal = 2.dp).width(48.dp).height(116.dp)) {
                Text(labels.getOrElse(i) { "" }, fontSize = 12.sp, maxLines = 1) } } }
}
fun solfege(semi: Double, sargam: Boolean): String {
    val names = if (sargam) arrayOf("Sa","Re","Ga","Ma","Pa","Dha","Ni")
        else arrayOf("Do","Re","Mi","Fa","Sol","La","Ti")
    val m = ((semi.toInt() % 12) + 12) % 12
    return when (m) { 0 -> names[0]; 2 -> names[1]; 4 -> names[2]; 5 -> names[3]
        7 -> names[4]; 9 -> names[5]; 11 -> names[6]; else -> names[((m + 1) / 2) % 7] + "·" }
}
