package io.github.naharaoss.skpd.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import io.github.naharaoss.skpd.AppSettings
import io.github.naharaoss.skpd.R
import kotlinx.serialization.Serializable

@Serializable
private sealed interface Page {
    val title: String @Composable get
    val subtitle: String @Composable get

    @Serializable
    object Main : Page {
        override val title: String @Composable get() = stringResource(R.string.settings_category_main_title)
        override val subtitle: String @Composable get() = stringResource(R.string.settings_category_main_subtitle)
    }

    @Serializable
    object About : Page {
        override val title: String @Composable get() = stringResource(R.string.settings_category_about_title)
        override val subtitle: String @Composable get() = stringResource(R.string.settings_category_about_subtitle)
    }

    @Serializable
    object Unknown : Page {
        override val title: String @Composable get() = "Huh?"
        override val subtitle: String @Composable get() = "How did you get here?"
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppSettingsScreen(
    modifier: Modifier = Modifier,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val backStack = remember { mutableStateListOf<Page>(Page.Main) } // TODO: implement parcel and use saveable
    val currentPage = backStack.lastOrNull() ?: Page.Unknown
    val spatialSpec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    val effectsSpec = MaterialTheme.motionScheme.slowEffectsSpec<Float>()
    val spatialSpecInt = MaterialTheme.motionScheme.slowSpatialSpec<IntOffset>()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    AnimatedContent(
                        targetState = Pair(currentPage, backStack.size),
                        transitionSpec = {
                            val pushing = targetState.second >= initialState.second
                            val enter = slideInVertically(spatialSpecInt) { if (pushing) -it else it } + fadeIn(effectsSpec)
                            val exit = slideOutVertically(spatialSpecInt) { if (pushing) it else -it } + fadeOut(effectsSpec)
                            (enter togetherWith exit).using(SizeTransform(clip = false))
                        }
                    ) { state ->
                        Text(state.first.title)
                    }
                },
                subtitle = {
                    AnimatedContent(
                        targetState = Pair(currentPage, backStack.size),
                        transitionSpec = {
                            val enter = fadeIn(effectsSpec)
                            val exit = fadeOut(effectsSpec)
                            (enter togetherWith exit).using(SizeTransform(clip = false))
                        }
                    ) { state ->
                        Text(state.first.subtitle)
                    }
                },
                navigationIcon = {
                    AnimatedContent(
                        targetState = backStack.size > 1,
                        transitionSpec = {
                            val enter = slideInVertically(spatialSpecInt) { if (targetState) -it else it } + fadeIn(effectsSpec)
                            val exit = slideOutVertically(spatialSpecInt) { if (targetState) it else -it } + fadeOut(effectsSpec)
                            (enter togetherWith exit).using(SizeTransform(clip = false))
                        }
                    ) { showButton ->
                        if (showButton) {
                            IconButton({ backStack.removeLastOrNull() }) {
                                Icon(
                                    painter = painterResource(R.drawable.arrow_back_24px),
                                    contentDescription = null
                                )
                            }
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        NavDisplay(
            modifier = Modifier.padding(innerPadding),
            backStack = backStack,
            transitionSpec = {
                ContentTransform(
                    scaleIn(spatialSpec, 0.75f) + fadeIn(effectsSpec),
                    scaleOut(spatialSpec, 1.25f) + fadeOut(effectsSpec)
                )
            },
            popTransitionSpec = {
                ContentTransform(
                    scaleIn(spatialSpec, 1.25f) + fadeIn(effectsSpec),
                    scaleOut(spatialSpec, 0.75f) + fadeOut(effectsSpec)
                )
            },
            onBack = { backStack.removeLastOrNull() },
            entryProvider = { key ->
                when (key) {
                    is Page.Main -> NavEntry(key) {
                        MainPage(
                            onAbout = { backStack.add(Page.About) }
                        )
                    }

                    is Page.About -> NavEntry(key) {
                        AboutPage()
                    }

                    else -> NavEntry(Page.Unknown) {
                        // we can crash the app here
                        Box(modifier = Modifier.fillMaxSize()) {
                            Text("Unknown route")
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun MainPage(
    modifier: Modifier = Modifier,
    onAbout: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxHeight()
            .verticalScroll(scrollState)
    ) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_category_about_title)) },
            supportingContent = { Text(stringResource(R.string.settings_category_about_subtitle)) },
            modifier = Modifier.clickable { onAbout() }
        )

        VersionFooter(
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AboutPage(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxHeight()
            .verticalScroll(scrollState)
    ) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_about_version_title)) },
            supportingContent = {
                Text(String.format(
                    stringResource(R.string.settings_about_version_subtitle),
                    stringResource(R.string.app_version),
                    stringResource(R.string.app_git_hash)
                ))
            },
        )

        ListItem(
            headlineContent = { Text("License") },
            supportingContent = { Text(stringResource(R.string.app_license)) },
        )
    }
}

@Composable
private fun VersionFooter(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.bodySmall,
                LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Text(stringResource(R.string.app_name_full))
                Text(
                    String.format(
                        stringResource(R.string.settings_about_version_subtitle),
                        stringResource(R.string.app_version),
                        stringResource(R.string.app_git_hash)
                    )
                )
            }
        }
    }
}
