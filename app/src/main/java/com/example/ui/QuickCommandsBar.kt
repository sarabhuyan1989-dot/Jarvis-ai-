package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.BorderCyan
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary

data class QuickCommand(
    val odiaText: String,
    val icon: ImageVector,
    val label: String
)

val SampleCommands = listOf(
    QuickCommand("ଜାରଭିସ୍, ତୁମେ କେମିତି ଅଛ?", Icons.AutoMirrored.Filled.Chat, "Greeting"),
    QuickCommand("ଜାରଭିସ୍, ମୋ ସହିତ ଓଡ଼ିଆରେ କଥା ହୁଅ", Icons.Default.RecordVoiceOver, "Speak Odia"),
    QuickCommand("ୟୁଟ୍ୟୁବ୍ ପାଇଁ ଗୋଟିଏ ଭଲ ଭିଡିଓ ଟପିକ୍ କୁହ", Icons.Default.Lightbulb, "Video Topic"),
    QuickCommand("ଆଜି ମୋତେ କଣ କରିବା ଉଚିତ?", Icons.Default.WbSunny, "Daily Advice"),
    QuickCommand("ୟୁଟ୍ୟୁବ୍ ଖୋଲନ୍ତୁ", Icons.Default.PlayCircle, "Open YouTube"),
    QuickCommand("କ୍ୟାମେରା ଖୋଲନ୍ତୁ", Icons.Default.CameraAlt, "Open Camera"),
    QuickCommand("ଏବେ ସମୟ କେତେ?", Icons.Default.AccessTime, "Tell Time"),
    QuickCommand("ସେଟିଙ୍ଗସ୍ ଖୋଲନ୍ତୁ", Icons.Default.Settings, "Settings"),
    QuickCommand("ଜାରଭିସ୍, ଏବେ ଚୁପ୍ ରୁହ", Icons.AutoMirrored.Filled.VolumeOff, "Silence")
)

@Composable
fun QuickCommandsBar(
    onCommandSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SampleCommands.forEachIndexed { index, cmd ->
            OutlinedButton(
                onClick = { onCommandSelected(cmd.odiaText) },
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderCyan.copy(alpha = 0.6f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = SurfaceDark.copy(alpha = 0.8f),
                    contentColor = TextPrimary
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.testTag("quick_command_$index")
            ) {
                Icon(
                    imageVector = cmd.icon,
                    contentDescription = null,
                    tint = ArcCyan,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "  ${cmd.odiaText}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            }
        }
    }
}
