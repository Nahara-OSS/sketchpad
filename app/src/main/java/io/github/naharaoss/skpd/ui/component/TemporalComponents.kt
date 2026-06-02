package io.github.naharaoss.skpd.ui.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.naharaoss.skpd.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

@Composable
fun Duration.toElapsedString(): String {
    return when {
        inWholeSeconds <= 0L -> stringResource(R.string.time_elapsed_now)
        inWholeSeconds == 1L -> stringResource(R.string.time_elapsed_one_second)
        inWholeMinutes <= 0L -> stringResource(R.string.time_elapsed_many_seconds).format(inWholeSeconds)
        inWholeMinutes == 1L -> stringResource(R.string.time_elapsed_one_minute)
        inWholeHours <= 0L -> stringResource(R.string.time_elapsed_many_minutes).format(inWholeMinutes)
        inWholeHours == 1L -> stringResource(R.string.time_elapsed_one_hour)
        inWholeDays <= 0L -> stringResource(R.string.time_elapsed_many_hours).format(inWholeHours)
        inWholeDays == 1L -> stringResource(R.string.time_elapsed_one_day)
        else -> stringResource(R.string.time_elapsed_many_days).format(inWholeDays)
    }
}

@Composable
fun ElapsedText(time: Instant, modifier: Modifier = Modifier) {
    var now by remember { mutableStateOf(Clock.System.now()) }

    LaunchedEffect(time) {
        while (isActive) {
            delay(1000.milliseconds)
            now = Clock.System.now()
        }
    }

    Text((now - time).toElapsedString(), modifier)
}