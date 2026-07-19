package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodels.TrackingViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppLayout()
            }
        }
    }
}

// Custom Navigation Routes Definition
object NavRoutes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    
    // Core Scaffold destinations
    const val CORE_HUB = "core_hub"
    const val CORE_MAP = "core_map"
    const val CORE_CHAT = "core_chat"
    const val CORE_ALERTS = "core_alerts"
    const val CORE_SETTINGS = "core_settings"
}

@Composable
fun MainAppLayout() {
    val navController = rememberNavController()
    val viewModel: TrackingViewModel = viewModel()
    
    // Observe authentication state to toggle navigation
    val currentUser by viewModel.currentUser.collectAsState()

    NavHost(
        navController = navController,
        startDestination = NavRoutes.SPLASH,
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Splash Screen
        composable(NavRoutes.SPLASH) {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate(NavRoutes.ONBOARDING) {
                        popUpTo(NavRoutes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        // 2. Onboarding Slide Carousel
        composable(NavRoutes.ONBOARDING) {
            OnboardingScreen(
                onOnboardingFinished = {
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(NavRoutes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        // 3. User Login
        composable(NavRoutes.LOGIN) {
            LoginScreen(
                viewModel = viewModel,
                onNavigateToRegister = {
                    navController.navigate(NavRoutes.REGISTER)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(NavRoutes.FORGOT_PASSWORD)
                },
                onLoginSuccess = {
                    navController.navigate(NavRoutes.CORE_HUB) {
                        popUpTo(NavRoutes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        // 4. User Registration
        composable(NavRoutes.REGISTER) {
            RegisterScreen(
                viewModel = viewModel,
                onNavigateToLogin = {
                    navController.navigate(NavRoutes.LOGIN)
                },
                onRegisterSuccess = {
                    navController.navigate(NavRoutes.CORE_HUB) {
                        popUpTo(NavRoutes.REGISTER) { inclusive = true }
                    }
                }
            )
        }

        // 5. Forgot Password Reset
        composable(NavRoutes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // 6. MAIN HUB SCAFFOLD LAYOUT DESTINATIONS
        composable(NavRoutes.CORE_HUB) {
            ScaffoldWrapper(navController, viewModel, NavRoutes.CORE_HUB) { modifier ->
                TrackingDashboardScreen(
                    viewModel = viewModel,
                    onNavigateToMap = { navController.navigate(NavRoutes.CORE_MAP) },
                    onNavigateToChat = { navController.navigate(NavRoutes.CORE_CHAT) },
                    onLogout = {
                        viewModel.logoutUser {
                            navController.navigate(NavRoutes.LOGIN) {
                                popUpTo(NavRoutes.CORE_HUB) { inclusive = true }
                            }
                        }
                    },
                    modifier = modifier
                )
            }
        }

        composable(NavRoutes.CORE_MAP) {
            ScaffoldWrapper(navController, viewModel, NavRoutes.CORE_MAP) { modifier ->
                MapScreen(viewModel = viewModel, modifier = modifier)
            }
        }

        composable(NavRoutes.CORE_CHAT) {
            ScaffoldWrapper(navController, viewModel, NavRoutes.CORE_CHAT) { modifier ->
                AiAssistantScreen(viewModel = viewModel, modifier = modifier)
            }
        }

        composable(NavRoutes.CORE_ALERTS) {
            ScaffoldWrapper(navController, viewModel, NavRoutes.CORE_ALERTS) { modifier ->
                NotificationsScreen(viewModel = viewModel, modifier = modifier)
            }
        }

        composable(NavRoutes.CORE_SETTINGS) {
            ScaffoldWrapper(navController, viewModel, NavRoutes.CORE_SETTINGS) { modifier ->
                SettingsAboutScreen(viewModel = viewModel, modifier = modifier)
            }
        }
    }
}

// --- HELPER COMPOSABLE: SCAFFOLD WRAPPER WITH MODERN BOTTOM BAR ---

@Composable
fun ScaffoldWrapper(
    navController: androidx.navigation.NavController,
    viewModel: TrackingViewModel,
    currentScreenRoute: String,
    content: @Composable (Modifier) -> Unit
) {
    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("app_bottom_nav_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                // Dashboard Hub tab
                NavigationBarItem(
                    selected = currentScreenRoute == NavRoutes.CORE_HUB,
                    onClick = {
                        navController.navigate(NavRoutes.CORE_HUB) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.DirectionsBus, "Hub") },
                    label = { Text("Hub", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_item_hub")
                )

                // GPS Live Map tracking tab
                NavigationBarItem(
                    selected = currentScreenRoute == NavRoutes.CORE_MAP,
                    onClick = {
                        navController.navigate(NavRoutes.CORE_MAP) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.Map, "Tracking Map") },
                    label = { Text("Track", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_item_map")
                )

                // Fleet AI Assistant tab
                NavigationBarItem(
                    selected = currentScreenRoute == NavRoutes.CORE_CHAT,
                    onClick = {
                        navController.navigate(NavRoutes.CORE_CHAT) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.SmartToy, "AI Copilot") },
                    label = { Text("Copilot", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_item_chat")
                )

                // Alerts Center tab
                NavigationBarItem(
                    selected = currentScreenRoute == NavRoutes.CORE_ALERTS,
                    onClick = {
                        navController.navigate(NavRoutes.CORE_ALERTS) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.Notifications, "Alerts Center") },
                    label = { Text("Alerts", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_item_alerts")
                )

                // Settings & Directory Directory tab
                NavigationBarItem(
                    selected = currentScreenRoute == NavRoutes.CORE_SETTINGS,
                    onClick = {
                        navController.navigate(NavRoutes.CORE_SETTINGS) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.Settings, "Settings Directory") },
                    label = { Text("Settings", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_item_settings")
                )
            }
        }
    ) { innerPadding ->
        content(Modifier.padding(innerPadding))
    }
}
