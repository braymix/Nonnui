package com.easyhome.senior.ui.admin

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.easyhome.senior.domain.model.BatteryStatus
import com.easyhome.senior.domain.model.LauncherConfig
import com.easyhome.senior.ui.home.HomeScreen
import com.easyhome.senior.ui.home.HomeUiState
import com.easyhome.senior.ui.home.HomeViewModel
import com.easyhome.senior.ui.home.IconLoader
import com.easyhome.senior.ui.theme.EasyHomeTheme
import java.time.LocalDateTime

private val VIRTUAL_WIDTH = 360.dp
private val VIRTUAL_HEIGHT = 740.dp

/**
 * Anteprima dal vivo: la VERA HomeScreen viene composta a dimensione di un telefono
 * reale (360x740dp) e poi rimpicciolita, così proporzioni e testi sono fedeli.
 */
@Composable
fun LivePreview(config: LauncherConfig, iconLoader: IconLoader, modifier: Modifier = Modifier, scale: Float = 0.42f) {
    val now = remember { LocalDateTime.now() }
    val state = HomeUiState(
        config = config,
        timeText = HomeViewModel.formatTime(now),
        dateText = HomeViewModel.formatDate(now),
        battery = BatteryStatus(80, false),
        isOnline = true,
        isLoaded = true,
    )
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("ANTEPRIMA", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        val shape = RoundedCornerShape(14.dp)
        Box(
            Modifier
                .size(VIRTUAL_WIDTH * scale, VIRTUAL_HEIGHT * scale)
                .clip(shape)
                .border(3.dp, Color(0xFF424242), shape),
        ) {
            Box(
                Modifier
                    .wrapContentSize(Alignment.TopStart, unbounded = true)
                    .requiredSize(VIRTUAL_WIDTH, VIRTUAL_HEIGHT)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0f, 0f)
                    },
            ) {
                EasyHomeTheme(theme = config.theme) {
                    HomeScreen(
                        state = state,
                        iconLoader = iconLoader,
                        onButtonClick = {},
                        onAdminHoldCompleted = {},
                        interactive = false,
                    )
                }
            }
        }
    }
}
