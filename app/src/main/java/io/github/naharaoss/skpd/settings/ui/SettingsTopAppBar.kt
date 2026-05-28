package io.github.naharaoss.skpd.settings.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.naharaoss.skpd.R
import io.github.naharaoss.skpd.ui.component.TooltipIconButton

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsTopAppBar(
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior,
    currentRoute: SettingsRoute,
    onBack: (() -> Unit)? = null
) {
    val (title, subtitle) = appBarTitlesByRoute(currentRoute)

    LargeFlexibleTopAppBar(
        modifier = modifier,
        title = {
            SettingsTopAppBarAnimatedText(targetState = title) { Text(it) }
        },
        subtitle = {
            SettingsTopAppBarAnimatedText(targetState = subtitle) { Text(it) }
        },
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            when (onBack) {
                null -> {}
                else -> TooltipIconButton(
                    painter = painterResource(R.drawable.arrow_back_24px),
                    description = "Go back",
                    onClick = onBack
                )
            }
        }
    )
}

@Composable
private fun <S> SettingsTopAppBarAnimatedText(
    modifier: Modifier = Modifier,
    targetState: S,
    content: @Composable AnimatedContentScope.(targetState: S) -> Unit,
) {
    AnimatedContent(
        modifier = modifier,
        targetState = targetState,
        transitionSpec = {
            val transition = fadeIn() togetherWith fadeOut()
            transition.using(SizeTransform(clip = false))
        },
        content = content
    )
}

private data class AppBarTitles(
    val title: String,
    val subtitle: String
)

@Composable
private fun appBarTitlesByRoute(route: SettingsRoute) = when (route) {
    is SettingsRoute.Main -> AppBarTitles(
        title = stringResource(R.string.settings_category_main_title),
        subtitle = stringResource(R.string.settings_category_main_subtitle)
    )
    is SettingsRoute.Appearance -> AppBarTitles(
        title = stringResource(R.string.settings_category_appearance_title),
        subtitle = stringResource(R.string.settings_category_appearance_subtitle)
    )
    is SettingsRoute.Toolbars -> AppBarTitles(
        title = stringResource(R.string.settings_category_toolbars_title),
        subtitle = stringResource(R.string.settings_category_toolbars_subtitle)
    )
    is SettingsRoute.Input -> AppBarTitles(
        title = stringResource(R.string.settings_category_input_title),
        subtitle = stringResource(R.string.settings_category_input_subtitle)
    )
    is SettingsRoute.Performance -> AppBarTitles(
        title = stringResource(R.string.settings_category_performance_title),
        subtitle = stringResource(R.string.settings_category_performance_subtitle)
    )
    is SettingsRoute.Miscellaneous -> AppBarTitles(
        title = stringResource(R.string.settings_category_miscellaneous_title),
        subtitle = stringResource(R.string.settings_category_miscellaneous_subtitle)
    )
    is SettingsRoute.About -> AppBarTitles(
        title = stringResource(R.string.settings_category_about_title),
        subtitle = stringResource(R.string.settings_category_about_subtitle)
    )
    else -> AppBarTitles(
        title = "Settings",
        subtitle = "Unknown category"
    )
}