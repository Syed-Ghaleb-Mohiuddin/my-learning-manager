package com.ghaleb.learningmanager.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ghaleb.learningmanager.R
import com.ghaleb.learningmanager.ui.theme.AppTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Milestone moment. With reduced motion (animations off in system settings) it is fully static. */
@Composable
fun Celebration(days: Int, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val c = AppTheme.colors
    val reduceMotion = remember {
        android.provider.Settings.Global.getFloat(
            context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f,
        ) == 0f
    }
    val burst = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduceMotion) burst.animateTo(1f, tween(durationMillis = 1200, easing = FastOutSlowInEasing))
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.clip(RoundedCornerShape(28.dp)).background(c.card).padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val p = burst.value
                    if (p < 1f) {
                        val square = 12.dp.toPx()
                        repeat(12) { i ->
                            val angle = i * (2 * PI / 12)
                            val r = size.minDimension / 2 * (0.35f + 0.65f * p)
                            val x = center.x + cos(angle).toFloat() * r
                            val y = center.y + sin(angle).toFloat() * r
                            drawRoundRect(
                                color = if (i % 3 == 0) c.freeze else c.session,
                                topLeft = Offset(x - square / 2, y - square / 2),
                                size = Size(square, square),
                                cornerRadius = CornerRadius(3.dp.toPx()),
                                alpha = 1f - p,
                            )
                        }
                    }
                }
                Image(
                    painterResource(R.drawable.mood_pleased),
                    contentDescription = null,
                    modifier = Modifier.size(120.dp).scale(0.8f + 0.2f * burst.value),
                )
            }
            Text("$days", style = MaterialTheme.typography.displayLarge, color = c.session)
            Text("days shown up", style = MaterialTheme.typography.titleLarge, color = c.text)
            Spacer(Modifier.height(4.dp))
            Text("Keep showing up.", color = c.textMuted)
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = c.session, contentColor = c.ink),
            ) { Text("Nice") }
        }
    }
}
