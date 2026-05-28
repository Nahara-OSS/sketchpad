package io.github.naharaoss.skpd.brush

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEvent
import dagger.hilt.android.AndroidEntryPoint
import io.github.naharaoss.skpd.brush.ui.BrushEditScreen
import io.github.naharaoss.skpd.brush.ui.BrushEditorRoute
import io.github.naharaoss.skpd.brush.ui.BrushEditorSceneStrategy
import io.github.naharaoss.skpd.brush.ui.BrushListMetadata
import io.github.naharaoss.skpd.brush.ui.BrushListScreen
import io.github.naharaoss.skpd.brush.ui.BrushMetadata
import io.github.naharaoss.skpd.brush.ui.DynamicEditScreen
import io.github.naharaoss.skpd.brush.ui.DynamicMetadata
import io.github.naharaoss.skpd.brush.ui.ScratchpadView
import io.github.naharaoss.skpd.ui.theme.SketchpadTheme

@AndroidEntryPoint
class BrushEditorActivity : ComponentActivity() {
    private val brushListViewModel: BrushListViewModel by viewModels()

    @OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postponeEnterTransition()
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val brushes by brushListViewModel.brushes.collectAsState()
            var backStack by rememberSerializable { mutableStateOf(listOf<BrushEditorRoute>(BrushEditorRoute.BrushList)) }

            fun goBack() {
                when {
                    backStack.size > 1 -> backStack = backStack.dropLast(1)
                    else -> finish()
                }
            }

            val entryProvider = entryProvider {
                entry<BrushEditorRoute.BrushList>(metadata = mapOf(BrushListMetadata)) {
                    BrushListScreen(
                        modifier = Modifier.fillMaxSize(),
                        viewModel = brushListViewModel,
                        windowSizeClass = windowSizeClass,
                        onBack = { goBack() },
                        onBrushSelect = { backStack = backStack + BrushEditorRoute.Brush(it.id) }
                    )
                }

                entry<BrushEditorRoute.Brush>(metadata = mapOf(BrushMetadata)) { key ->
                    BrushEditScreen(
                        modifier = Modifier.fillMaxSize(),
                        viewModel = brushListViewModel,
                        brushId = key.brushId,
                        onBack = { goBack() },
                        onDynamicEditor = { id, parameter ->
                            val existingIndex = backStack.indexOfFirst { it is BrushEditorRoute.Dynamic }
                            val newRoute = BrushEditorRoute.Dynamic(id, parameter)
                            backStack = when (existingIndex) {
                                -1 -> backStack + newRoute
                                else -> backStack.map { if (it is BrushEditorRoute.Dynamic) newRoute else it }
                            }
                        }
                    )
                }

                entry<BrushEditorRoute.Dynamic>(metadata = mapOf(DynamicMetadata)) { key ->
                    DynamicEditScreen(
                        modifier = Modifier.fillMaxSize(),
                        viewModel = brushListViewModel,
                        brushId = key.brushId,
                        parameter = key.parameter,
                        onBack = { goBack() }
                    )
                }
            }

            SketchpadTheme {
                val fadeMotion = tween<Float>(durationMillis = 300, easing = LinearEasing)
                val slideMotion = tween<IntOffset>(300, easing = FastOutSlowInEasing)
                val sizeMotion = MaterialTheme.motionScheme.defaultSpatialSpec<IntSize>()

                if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
                    Surface {
                        NavDisplay(
                            backStack = backStack,
                            transitionSpec = {
                                val enter = fadeIn(fadeMotion) + slideInHorizontally(slideMotion) { it / 4 }
                                val exit = fadeOut(fadeMotion) + slideOutHorizontally(slideMotion) { it / -4 }
                                enter togetherWith exit
                            },
                            popTransitionSpec = {
                                val enter = fadeIn(fadeMotion) + slideInHorizontally(slideMotion) { it / -4 }
                                val exit = fadeOut(fadeMotion) + slideOutHorizontally(slideMotion) { it / 4 }
                                enter togetherWith exit
                            },
                            predictivePopTransitionSpec = { edge ->
                                val edgeMul = when (edge) {
                                    NavigationEvent.EDGE_LEFT -> -1
                                    NavigationEvent.EDGE_RIGHT -> +1
                                    else -> -1
                                }

                                val enter = fadeIn(fadeMotion) + slideInHorizontally(slideMotion) { it * edgeMul / 4 }
                                val exit = fadeOut(fadeMotion) + slideOutHorizontally(slideMotion) { it * edgeMul / -4 }
                                enter togetherWith exit
                            },
                            onBack = { goBack() },
                            entryProvider = entryProvider
                        )
                    }
                } else {
                    val testAreaPadding = TopAppBarDefaults.windowInsets
                        .exclude(TopAppBarDefaults.windowInsets.only(WindowInsetsSides.Start))
                        .asPaddingValues()
                    var preset: BrushType.Preset? by remember { mutableStateOf(null) }
                    val presetFlow = backStack
                        .find { it is BrushEditorRoute.Brush }
                        ?.let { route ->
                            brushes
                                ?.find { it.id == (route as BrushEditorRoute.Brush).brushId }
                                ?.preset
                        }

                    LaunchedEffect(presetFlow) {
                        if (presetFlow != null) {
                            presetFlow.collect { preset = it }
                        } else {
                            preset = null
                        }
                    }

                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ScratchpadView(it) }
                    ) {
                        val preset = preset

                        if (preset != null) {
                            it.enableScratchpad = true
                            it.preset = preset
                        } else {
                            it.enableScratchpad = false
                        }
                    }

                    Row {
                        Box(Modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
                            Surface(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .consumeWindowInsets(TopAppBarDefaults.windowInsets.only(WindowInsetsSides.End)),
//                                shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                            ) {
                                SharedTransitionLayout {
                                    NavDisplay(
                                        backStack = backStack,
                                        sceneStrategies = listOf(BrushEditorSceneStrategy(windowSizeClass)),
                                        sizeTransform = SizeTransform(
                                            clip = true,
                                            sizeAnimationSpec = { _, _ -> sizeMotion }
                                        ),
                                        sharedTransitionScope = this,
                                        transitionSpec = {
                                            val enter = fadeIn(fadeMotion) + slideInHorizontally(slideMotion) { it / 4 }
                                            val exit = fadeOut(fadeMotion) + slideOutHorizontally(slideMotion) { it / -4 }
                                            enter togetherWith exit
                                        },
                                        popTransitionSpec = {
                                            val enter = fadeIn(fadeMotion) + slideInHorizontally(slideMotion) { it / -4 }
                                            val exit = fadeOut(fadeMotion) + slideOutHorizontally(slideMotion) { it / 4 }
                                            enter togetherWith exit
                                        },
                                        predictivePopTransitionSpec = { edge ->
                                            val edgeMul = when (edge) {
                                                NavigationEvent.EDGE_LEFT -> -1
                                                NavigationEvent.EDGE_RIGHT -> +1
                                                else -> -1
                                            }

                                            val enter = fadeIn(fadeMotion) + slideInHorizontally(slideMotion) { it * edgeMul / 4 }
                                            val exit = fadeOut(fadeMotion) + slideOutHorizontally(slideMotion) { it * edgeMul / -4 }
                                            enter togetherWith exit
                                        },
                                        onBack = { goBack() },
                                        entryProvider = entryProvider
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .padding(testAreaPadding)
                                .fillMaxWidth()
                        ) {
                            Surface(
                                modifier = Modifier.padding(16.dp),
                                shadowElevation = 2.dp,
                                shape = CircleShape
                            ) {
                                Box(Modifier.padding(16.dp, 8.dp)) {
                                    CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyMedium) {
                                        Text("Test area")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}