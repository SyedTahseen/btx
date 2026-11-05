package com.betterx.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.betterx.app.data.Pin

@Composable
fun AppRoot() {
    val store = LocalStore.current
    val d = store.data
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when {
            d.pinHash.isNotEmpty() && !store.unlocked ->
                Box(Modifier.fillMaxSize().systemBarsPadding().imePadding()) { LockScreen() }
            !d.onboarded ->
                Box(Modifier.fillMaxSize().systemBarsPadding().imePadding()) { OnboardingScreen() }
            else -> MainNav()
        }
    }
}

@Composable
fun LockScreen() {
    val store = LocalStore.current
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("BetterX", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text("Enter your PIN", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it.filter(Char::isDigit).take(6); error = false },
            label = { Text("PIN") },
            singleLine = true,
            isError = error,
            supportingText = { if (error) Text("Incorrect PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                val d = store.data
                if (Pin.hash(pin, d.pinSalt) == d.pinHash) {
                    store.unlocked = true
                    pin = ""
                } else {
                    error = true
                }
            },
            enabled = pin.length >= 4,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Unlock") }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

@Composable
fun MainNav() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val tabs = listOf(
        Tab("home", "Today", Icons.Filled.Home),
        Tab("learn", "Learn", Icons.AutoMirrored.Filled.List),
        Tab("coach", "Coach", Icons.Filled.Face),
        Tab("ghost", "Ghost", Icons.Filled.Person),
        Tab("progress", "Progress", Icons.Filled.Star),
    )
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (tabs.any { it.route == route }) {
                NavigationBar {
                    tabs.forEach { t ->
                        NavigationBarItem(
                            selected = route == t.route,
                            onClick = {
                                nav.navigate(t.route) {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(t.icon, contentDescription = t.label) },
                            label = { Text(t.label) },
                        )
                    }
                }
            }
        },
    ) { pad ->
        NavHost(
            navController = nav,
            startDestination = "home",
            modifier = Modifier.padding(pad).consumeWindowInsets(pad),
        ) {
            composable("home") {
                HomeScreen(
                    onExercise = { nav.navigate("exercise/$it") },
                    onCheckIn = { nav.navigate("checkin") },
                    onProfile = { nav.navigate("profile") },
                    onPaywall = { nav.navigate("paywall") },
                )
            }
            composable("learn") { LearnScreen(onCourse = { nav.navigate("course/$it") }) }
            composable("coach") {
                CoachScreen(
                    onExercise = { nav.navigate("exercise/$it") },
                    onPaywall = { nav.navigate("paywall") },
                )
            }
            composable("ghost") {
                GhostScreen(onOpen = { nav.navigate("post/$it") }, onNew = { nav.navigate("newpost") })
            }
            composable("progress") {
                ProgressScreen(onCheckIn = { nav.navigate("checkin") }, onReassess = { nav.navigate("assess") })
            }
            composable("course/{id}") { e ->
                CourseScreen(
                    id = e.arguments?.getString("id").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onExercise = { nav.navigate("exercise/$it") },
                    onPaywall = { nav.navigate("paywall") },
                )
            }
            composable("exercise/{id}") { e ->
                ExerciseScreen(id = e.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() })
            }
            composable("checkin") { CheckInScreen(onBack = { nav.popBackStack() }) }
            composable("assess") { AssessScreen(onBack = { nav.popBackStack() }) }
            composable("post/{id}") { e ->
                PostScreen(id = e.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() })
            }
            composable("newpost") { NewPostScreen(onBack = { nav.popBackStack() }) }
            composable("profile") {
                ProfileScreen(onBack = { nav.popBackStack() }, onPaywall = { nav.navigate("paywall") })
            }
            composable("paywall") { PaywallScreen(onBack = { nav.popBackStack() }) }
        }
    }
}
