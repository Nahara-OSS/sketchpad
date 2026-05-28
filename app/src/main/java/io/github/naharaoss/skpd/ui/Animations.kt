package io.github.naharaoss.skpd.ui

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

fun slideTransition(reversed: Boolean): ContentTransform {
    val incoming = slideInHorizontally { if (reversed) -it / 2 else it / 2 } + fadeIn()
    val outgoing = slideOutHorizontally { if (reversed) it / 2 else -it / 2 } + fadeOut()
    return incoming togetherWith outgoing
}