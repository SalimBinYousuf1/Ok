package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.UserSettings
import com.example.notifications.NotificationCoordinator
import com.example.ui.components.AppLockOverlay
import com.example.ui.screens.ComposeScreen
import com.example.ui.screens.ConversationDetailScreen
import com.example.ui.screens.InboxScreen
import com.example.ui.screens.ScheduledMessagesScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.SalimTheme
import com.example.ui.viewmodels.ComposeViewModel
import com.example.ui.viewmodels.ConversationViewModel
import com.example.ui.viewmodels.InboxViewModel
import com.example.ui.viewmodels.ScheduledViewModel
import com.example.ui.viewmodels.SearchViewModel
import com.example.ui.viewmodels.SettingsViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create notification channels
        NotificationCoordinator.createNotificationChannels(this)

        setContent {
            val context = LocalContext.current
            val userPrefs = remember { UserPreferencesRepository(context) }
            val settings by userPrefs.userSettings.collectAsStateWithLifecycle(initialValue = UserSettings())

            // One-time permissions check using SharedPreferences: asks once, never again on subsequent launches
            val sharedPrefs = remember {
                context.getSharedPreferences("salim_permission_prefs", Context.MODE_PRIVATE)
            }
            var permissionsAlreadyAsked by remember {
                mutableStateOf(sharedPrefs.getBoolean("KEY_PERMISSIONS_ASKED", false))
            }

            val permissionsLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) {
                sharedPrefs.edit().putBoolean("KEY_PERMISSIONS_ASKED", true).apply()
                permissionsAlreadyAsked = true
            }

            LaunchedEffect(permissionsAlreadyAsked) {
                if (!permissionsAlreadyAsked) {
                    sharedPrefs.edit().putBoolean("KEY_PERMISSIONS_ASKED", true).apply()
                    val permissionsToRequest = mutableListOf(
                        Manifest.permission.READ_SMS,
                        Manifest.permission.SEND_SMS,
                        Manifest.permission.RECEIVE_SMS,
                        Manifest.permission.READ_CONTACTS
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    permissionsLauncher.launch(permissionsToRequest.toTypedArray())
                }
            }

            // Functional App Lock: If enabled, require 4-digit PIN verification before displaying app content
            var isAppUnlocked by remember(settings.appLockEnabled) {
                mutableStateOf(!settings.appLockEnabled)
            }

            SalimTheme(
                themeMode = settings.themeMode,
                dynamicColor = settings.dynamicColor
            ) {
                if (settings.appLockEnabled && !isAppUnlocked) {
                    AppLockOverlay(
                        expectedPin = settings.appLockPin,
                        onUnlocked = { isAppUnlocked = true }
                    )
                } else {
                    SalimAppNavigation(intent = intent)
                }
            }
        }
    }
}

@Composable
fun SalimAppNavigation(intent: Intent?) {
    val navController = rememberNavController()

    // Handle incoming intent (e.g. from notification)
    LaunchedEffect(intent) {
        val convId = intent?.getLongExtra("extra_conversation_id", -1L) ?: -1L
        if (convId != -1L) {
            navController.navigate("conversation/$convId")
        }
    }

    NavHost(
        navController = navController,
        startDestination = "inbox",
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(220)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(220)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(220)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(220)
            )
        }
    ) {
        composable("inbox") {
            val inboxViewModel: InboxViewModel = viewModel()
            InboxScreen(
                viewModel = inboxViewModel,
                onNavigateToConversation = { id -> navController.navigate("conversation/$id") },
                onNavigateToCompose = { navController.navigate("compose") },
                onNavigateToSearch = { navController.navigate("search") },
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToScheduled = { navController.navigate("scheduled") }
            )
        }

        composable(
            route = "conversation/{conversationId}",
            arguments = listOf(navArgument("conversationId") { type = NavType.LongType })
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getLong("conversationId") ?: 0L
            val context = LocalContext.current
            val conversationViewModel: ConversationViewModel = viewModel(
                factory = ConversationViewModel.Factory(
                    application = context.applicationContext as android.app.Application,
                    conversationId = conversationId
                )
            )
            ConversationDetailScreen(
                viewModel = conversationViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("compose") {
            val composeViewModel: ComposeViewModel = viewModel()
            ComposeScreen(
                viewModel = composeViewModel,
                onNavigateBack = { navController.popBackStack() },
                onConversationCreated = { id ->
                    navController.popBackStack()
                    navController.navigate("conversation/$id")
                }
            )
        }

        composable("search") {
            val searchViewModel: SearchViewModel = viewModel()
            SearchScreen(
                viewModel = searchViewModel,
                onNavigateBack = { navController.popBackStack() },
                onConversationSelected = { id ->
                    navController.popBackStack()
                    navController.navigate("conversation/$id")
                }
            )
        }

        composable("scheduled") {
            val scheduledViewModel: ScheduledViewModel = viewModel()
            ScheduledMessagesScreen(
                viewModel = scheduledViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("settings") {
            val settingsViewModel: SettingsViewModel = viewModel()
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
