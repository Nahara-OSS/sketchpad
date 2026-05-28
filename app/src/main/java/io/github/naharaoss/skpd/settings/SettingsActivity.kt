package io.github.naharaoss.skpd.settings

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEvent
import dagger.hilt.android.AndroidEntryPoint
import io.github.naharaoss.skpd.R
import io.github.naharaoss.skpd.settings.ui.MainSettingsPage
import io.github.naharaoss.skpd.settings.ui.SettingsPage
import io.github.naharaoss.skpd.settings.ui.SettingsRoute
import io.github.naharaoss.skpd.settings.ui.SettingsTopAppBar
import io.github.naharaoss.skpd.settings.ui.rememberSettingsBackStack
import io.github.naharaoss.skpd.ui.theme.SketchpadTheme

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {
    private val viewModel: SettingsViewModel by viewModels()

    @OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val (backStack, goBack, navigateTo) = rememberSettingsBackStack(windowSizeClass = windowSizeClass, onExit = { finish() })
            val sourceUrl = stringResource(R.string.app_source_url).toUri()
            val fadeMotion = tween<Float>(durationMillis = 300, easing = LinearEasing)
            val slideMotion = tween<IntOffset>(300, easing = FastOutSlowInEasing)

            SketchpadTheme {
                when (windowSizeClass.widthSizeClass) {
                    WindowWidthSizeClass.Compact -> {
                        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            topBar = {
                                SettingsTopAppBar(
                                    currentRoute = backStack.last(),
                                    scrollBehavior = scrollBehavior,
                                    onBack = goBack
                                )
                            }
                        ) { innerPadding ->
                            NavDisplay(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                                backStack = backStack,
                                onBack = goBack,
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
                                entryProvider = { key ->
                                    NavEntry(key) {
                                        SettingsPage(
                                            modifier = Modifier.fillMaxSize(),
                                            route = key,
                                            viewModel = viewModel,
                                            onNavigate = { navigateTo(it, false) },
                                            onOpenSourceCode = {
                                                val intent = Intent(Intent.ACTION_VIEW, sourceUrl)
                                                startActivity(intent)
                                            }
                                        )
                                    }
                                }
                            )
                        }
                    }

                    else -> {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

                            Scaffold(
                                modifier = (if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium) Modifier.weight(1f) else Modifier.width(400.dp))
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(0.dp, 16.dp, 16.dp, 0.dp))
                                    .consumeWindowInsets(ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.End)),
                                containerColor = MaterialTheme.colorScheme.background,
                                topBar = { SettingsTopAppBar(scrollBehavior = scrollBehavior, currentRoute = SettingsRoute.Main, onBack = goBack) }
                            ) { innerPadding ->
                                Box(Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection)) {
                                    MainSettingsPage(
                                        modifier = Modifier.padding(innerPadding).fillMaxSize(),
                                        currentRoute = backStack[0],
                                        onNavigate = { navigateTo(it, true) }
                                    )
                                }
                            }

                            Surface {
                                NavDisplay(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(16.dp, 0.dp, 0.dp, 16.dp)),
                                    backStack = backStack,
                                    onBack = goBack,
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
                                    entryProvider = { key ->
                                        NavEntry(key) {
                                            val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

                                            Scaffold(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .consumeWindowInsets(ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Start)),
                                                topBar = { SettingsTopAppBar(scrollBehavior = scrollBehavior, currentRoute = key) }
                                            ) { innerPadding ->
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                                                        .padding(innerPadding)
                                                ) {
                                                    SettingsPage(
                                                        modifier = Modifier.fillMaxSize(),
                                                        route = key,
                                                        viewModel = viewModel,
                                                        onNavigate = { navigateTo(it, false) },
                                                        onOpenSourceCode = {
                                                            val intent = Intent(Intent.ACTION_VIEW, sourceUrl)
                                                            startActivity(intent)
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}