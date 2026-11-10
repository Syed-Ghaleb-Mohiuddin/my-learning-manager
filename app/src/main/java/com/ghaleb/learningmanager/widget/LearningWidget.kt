package com.ghaleb.learningmanager.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.ghaleb.learningmanager.R
import com.ghaleb.learningmanager.mood.Mood
import com.ghaleb.learningmanager.streak.DayState

class LearningWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        WidgetUpdater.write(context, id, WidgetUpdater.snapshot(context))
        val open = actionStartActivity(ClaudeLauncher.intent(context))
        provideContent {
            val snapshot = WidgetUpdater.read(currentState<Preferences>())
            WidgetContent(snapshot, wide = LocalSize.current.width >= MEDIUM.width, onClick = open)
        }
    }

    companion object {
        val SMALL = DpSize(110.dp, 110.dp)
        val MEDIUM = DpSize(250.dp, 110.dp)
    }
}

class LearningWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LearningWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetRefreshWorker.schedule(context)
    }
}

@Composable
fun WidgetContent(snapshot: WidgetSnapshot, wide: Boolean, onClick: Action) {
    val textColor = ColorProvider(R.color.text)
    Box(
        modifier = GlanceModifier.fillMaxSize()
            .background(ImageProvider(R.drawable.widget_bg))
            .clickable(onClick)
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (wide) {
            Row(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    provider = ImageProvider(snapshot.mood.drawable),
                    contentDescription = snapshot.mood.line,
                    modifier = GlanceModifier.size(88.dp),
                )
                Spacer(GlanceModifier.width(12.dp))
                Column {
                    Text(
                        text = snapshot.mood.line,
                        style = TextStyle(color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold),
                        maxLines = 2,
                    )
                    Spacer(GlanceModifier.height(10.dp))
                    WeekRow(snapshot.week)
                }
            }
        } else {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    provider = ImageProvider(snapshot.mood.drawable),
                    contentDescription = snapshot.mood.line,
                    modifier = GlanceModifier.size(64.dp),
                )
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    text = snapshot.mood.short,
                    style = TextStyle(color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun WeekRow(week: List<DayState?>) {
    Row {
        week.forEachIndexed { i, state ->
            if (i > 0) Spacer(GlanceModifier.width(4.dp))
            Box(modifier = GlanceModifier.size(16.dp).background(ImageProvider(cellDrawable(state)))) {}
        }
    }
}

private fun cellDrawable(state: DayState?): Int = when (state) {
    DayState.SESSION -> R.drawable.cell_session
    DayState.FREEZE -> R.drawable.cell_freeze
    DayState.GAP -> R.drawable.cell_gap
    DayState.TODAY_PENDING -> R.drawable.cell_today
    DayState.FUTURE, null -> R.drawable.cell_future
}

// Previews of every mood at both sizes, so the look can be checked without waiting for the evening.

private val sampleWeek = listOf(
    DayState.SESSION, DayState.SESSION, DayState.FREEZE, DayState.TODAY_PENDING,
    DayState.FUTURE, DayState.FUTURE, DayState.FUTURE,
)

@Composable
private fun PreviewOf(mood: Mood, wide: Boolean) {
    val week = if (mood == Mood.PLEASED) sampleWeek.map { if (it == DayState.TODAY_PENDING) DayState.SESSION else it } else sampleWeek
    WidgetContent(WidgetSnapshot(mood, week), wide, actionStartActivity(Intent()))
}

@OptIn(ExperimentalGlancePreviewApi::class) @Preview(widthDp = 110, heightDp = 110) @Composable
private fun SmallCalm() = PreviewOf(Mood.CALM, wide = false)

@OptIn(ExperimentalGlancePreviewApi::class) @Preview(widthDp = 110, heightDp = 110) @Composable
private fun SmallNudging() = PreviewOf(Mood.NUDGING, wide = false)

@OptIn(ExperimentalGlancePreviewApi::class) @Preview(widthDp = 110, heightDp = 110) @Composable
private fun SmallAnnoyed() = PreviewOf(Mood.ANNOYED, wide = false)

@OptIn(ExperimentalGlancePreviewApi::class) @Preview(widthDp = 110, heightDp = 110) @Composable
private fun SmallAngry() = PreviewOf(Mood.ANGRY, wide = false)

@OptIn(ExperimentalGlancePreviewApi::class) @Preview(widthDp = 110, heightDp = 110) @Composable
private fun SmallFurious() = PreviewOf(Mood.FURIOUS, wide = false)

@OptIn(ExperimentalGlancePreviewApi::class) @Preview(widthDp = 110, heightDp = 110) @Composable
private fun SmallPleased() = PreviewOf(Mood.PLEASED, wide = false)

@OptIn(ExperimentalGlancePreviewApi::class) @Preview(widthDp = 110, heightDp = 110) @Composable
private fun SmallFreeze() = PreviewOf(Mood.FREEZE, wide = false)

@OptIn(ExperimentalGlancePreviewApi::class) @Preview(widthDp = 250, heightDp = 110) @Composable
private fun MediumCalm() = PreviewOf(Mood.CALM, wide = true)

@OptIn(ExperimentalGlancePreviewApi::class) @Preview(widthDp = 250, heightDp = 110) @Composable
private fun MediumNudging() = PreviewOf(Mood.NUDGING, wide = true)

@OptIn(ExperimentalGlancePreviewApi::class) @Preview(widthDp = 250, heightDp = 110) @Composable
private fun MediumAnnoyed() = PreviewOf(Mood.ANNOYED, wide = true)

@OptIn(ExperimentalGlancePreviewApi::class) @Preview(widthDp = 250, heightDp = 110) @Composable
private fun MediumAngry() = PreviewOf(Mood.ANGRY, wide = true)

@OptIn(ExperimentalGlancePreviewApi::class) @Preview(widthDp = 250, heightDp = 110) @Composable
private fun MediumFurious() = PreviewOf(Mood.FURIOUS, wide = true)

@OptIn(ExperimentalGlancePreviewApi::class) @Preview(widthDp = 250, heightDp = 110) @Composable
private fun MediumPleased() = PreviewOf(Mood.PLEASED, wide = true)

@OptIn(ExperimentalGlancePreviewApi::class) @Preview(widthDp = 250, heightDp = 110) @Composable
private fun MediumFreeze() = PreviewOf(Mood.FREEZE, wide = true)
