package com.betterx.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AppRoot() {
    val store = LocalStore.current
    val d = store.data
    // Upload any profile changes that haven't reached Firebase yet; retries on each app start while still unsynced.
    LaunchedEffect(d.signedIn, d.profileDirty) {
        if (d.signedIn && d.profileDirty) store.pushProfileIfNeeded()
    }
    // Surface gives every Text a readable default colour on the black background.
    Surface(
        Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        when {
            !d.signedIn ->
                Box(Modifier.fillMaxSize().systemBarsPadding().imePadding()) { AuthScreen() }
            !d.onboarded ->
                Box(Modifier.fillMaxSize().systemBarsPadding().imePadding()) { OnboardingScreen() }
            else -> MainNav()
        }
    }
}

/** Tiny back-stack so we don't need the navigation library. Routes look like "course:calm". */
private class Nav(start: String) {
    val stack = mutableStateListOf(start)
    val current: String get() = stack.last()
    fun go(route: String) { stack.add(route) }
    fun tab(route: String) { stack.clear(); stack.add(route) }
    fun back() { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
}

@Composable
fun MainNav() {
    val nav = remember { Nav("home") }
    BackHandler(enabled = nav.stack.size > 1) { nav.back() }

    val tabs = listOf(
        NavItem("home", "Today", Icons.Filled.Home),
        NavItem("coach", "Coach", Icons.Filled.Face),
        NavItem("ghost", "Ghost", Icons.Filled.Person),
        NavItem("progress", "Progress", Icons.Filled.Star),
    )
    val route = nav.current
    val isTab = tabs.any { it.route == route }
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val ease = FastOutSlowInEasing

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { pad ->
        Box(Modifier.fillMaxSize().padding(top = pad.calculateTopPadding())) {
            // Pushing a screen slides it in from the right; going back slides it out; switching tabs cross-fades.
            AnimatedContent(
                targetState = route to nav.stack.size,
                transitionSpec = {
                    val from = initialState.second
                    val to = targetState.second
                    when {
                        to > from ->
                            (slideInHorizontally(tween(340, easing = ease)) { it / 3 } + fadeIn(tween(280))) togetherWith
                                (slideOutHorizontally(tween(340, easing = ease)) { -it / 6 } + fadeOut(tween(220)))
                        to < from ->
                            (slideInHorizontally(tween(340, easing = ease)) { -it / 6 } + fadeIn(tween(280))) togetherWith
                                (slideOutHorizontally(tween(340, easing = ease)) { it / 3 } + fadeOut(tween(220)))
                        else ->
                            (fadeIn(tween(260, delayMillis = 70)) + scaleIn(tween(260, delayMillis = 70), initialScale = 0.97f)) togetherWith
                                fadeOut(tween(110))
                    }
                },
                label = "route",
            ) { (r, _) ->
                val name = r.substringBefore(':')
                val arg = r.substringAfter(':', "")
                val tabbed = tabs.any { it.route == r }
                // Tab screens leave room for the floating nav. Home draws its sheet under it on purpose.
                val bottom = when {
                    name == "home" -> 0.dp
                    tabbed -> maxOf(pad.calculateBottomPadding(), navInset + 84.dp)
                    else -> pad.calculateBottomPadding()
                }
                Box(Modifier.fillMaxSize().padding(bottom = bottom).consumeWindowInsets(pad)) {
                    when (name) {
                        "home" -> HomeScreen(
                            onExercise = { nav.go("exercise:$it") },
                            onCheckIn = { nav.go("checkin") },
                            onPrograms = { nav.go("programs") },
                            onProfile = { nav.go("profile") },
                            onPaywall = { nav.go("paywall") },
                        )
                        "coach" -> CoachScreen(
                            onExercise = { nav.go("exercise:$it") },
                            onPaywall = { nav.go("paywall") },
                        )
                        "ghost" -> GhostScreen(onOpen = { nav.go("post:$it") }, onNew = { nav.go("newpost") })
                        "progress" -> ProgressScreen(onCheckIn = { nav.go("checkin") }, onReassess = { nav.go("assess") })
                        "programs" -> LearnScreen(onBack = nav::back, onCourse = { nav.go("course:$it") })
                        "course" -> CourseScreen(
                            id = arg,
                            onBack = nav::back,
                            onExercise = { nav.go("exercise:$it") },
                            onPaywall = { nav.go("paywall") },
                        )
                        "exercise" -> ExerciseScreen(id = arg, onBack = nav::back)
                        "checkin" -> CheckInScreen(onBack = nav::back)
                        "assess" -> AssessScreen(onBack = nav::back)
                        "post" -> PostScreen(id = arg, onBack = nav::back)
                        "newpost" -> NewPostScreen(onBack = nav::back)
                        "profile" -> ProfileScreen(onBack = nav::back, onPaywall = { nav.go("paywall") })
                        "paywall" -> PaywallScreen(onBack = nav::back)
                    }
                }
            }
            AnimatedVisibility(
                visible = isTab,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(spring(dampingRatio = 0.8f, stiffness = 300f)) { it } + fadeIn(),
                exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(160)),
            ) {
                PillNav(tabs, route, onSelect = { nav.tab(it) })
            }
        }
    }
}
