package io.github.naharaoss.skpd.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.naharaoss.skpd.settings.PerformanceSettings
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PerformanceSettingsPage(
    modifier: Modifier = Modifier,
    settings: PerformanceSettings,
    onSettingsChange: (PerformanceSettings) -> Unit
) {
    var defaultTileSizeLog by remember(settings.defaultTileSizeLog) { mutableIntStateOf(settings.defaultTileSizeLog) }
    var preloadRatio by remember(settings.preloadRatio) { mutableStateOf(settings.preloadRatio) }
    var maxUndoCount by remember(settings.maxUndoCount) { mutableStateOf(settings.maxUndoCount) }

    Column(modifier.verticalScroll(rememberScrollState())) {
        ListItem(
            onClick = {},
            content = { Text("Default tile size") },
            supportingContent = { Text("Each square tile is ${1 shl defaultTileSizeLog} pixels in size") }
        )
        Slider(
            modifier = Modifier.padding(16.dp, 8.dp),
            value = defaultTileSizeLog.toFloat(),
            onValueChange = { defaultTileSizeLog = it.roundToInt() },
            onValueChangeFinished = { onSettingsChange(settings.copy(defaultTileSizeLog = defaultTileSizeLog)) },
            valueRange = 6f..12f,
            steps = 5
        )

//        ListItem(
//            onClick = {},
//            content = { Text("Preload ratio") },
//            supportingContent = { Text("Load ${(preloadRatio * 100f).roundToInt()}% more tiles outside the screen") }
//        )
//        Slider(
//            modifier = Modifier.padding(16.dp, 8.dp),
//            value = preloadRatio,
//            onValueChange = { preloadRatio = it },
//            onValueChangeFinished = { onSettingsChange(settings.copy(preloadRatio = preloadRatio)) },
//            valueRange = 0f..1f
//        )

//        ListItem(
//            onClick = {},
//            content = { Text("Max undo count") },
//            supportingContent = { Text("Store up to $maxUndoCount undo attempts to sketches") }
//        )
//        Slider(
//            modifier = Modifier.padding(16.dp, 8.dp),
//            value = maxUndoCount.toFloat(),
//            onValueChange = { maxUndoCount = it.roundToInt() },
//            onValueChangeFinished = { onSettingsChange(settings.copy(maxUndoCount = maxUndoCount)) },
//            valueRange = 10f..200f
//        )
    }
}