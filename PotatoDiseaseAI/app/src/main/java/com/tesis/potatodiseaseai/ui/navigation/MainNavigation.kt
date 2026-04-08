package com.tesis.potatodiseaseai.ui.navigation

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.tesis.potatodiseaseai.ui.screens.*
import androidx.core.content.edit

@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Verificar si es la primera vez que se abre la app
    val prefs = remember {
        context.getSharedPreferences("potato_disease_prefs", android.content.Context.MODE_PRIVATE)
    }
    val hasSeenOnboarding = remember { prefs.getBoolean("has_seen_onboarding", false) }
    val startRoute = if (hasSeenOnboarding) NavigationHelper.Routes.SCANNER
                     else NavigationHelper.Routes.ONBOARDING

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route

            // Ocultar bottom bar en onboarding y result
            if (currentRoute != NavigationHelper.Routes.ONBOARDING &&
                currentRoute?.startsWith(NavigationHelper.Routes.RESULT_BASE) != true
            ) {
                NavigationBar {
                    Screen.bottomTabs.forEach { screen ->
                        NavigationBarItem(
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { 
                                        saveState = true 
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startRoute
        ) {
            // Onboarding (Tutorial)
            composable(NavigationHelper.Routes.ONBOARDING) {
                OnboardingScreen(
                    onFinish = {
                        // Marcar como visto
                        prefs.edit { putBoolean("has_seen_onboarding", true) }
                        // Navegar al scanner y limpiar el backstack
                        navController.navigate(NavigationHelper.Routes.SCANNER) {
                            popUpTo(NavigationHelper.Routes.ONBOARDING) { inclusive = true }
                        }
                    }
                )
            }

            // Scanner
            composable(NavigationHelper.Routes.SCANNER) { 
                val vm: ScannerViewModel = viewModel()
                val uiState by vm.uiState.collectAsState()
                
                LaunchedEffect(uiState.shouldNavigateToResult) {
                    if (uiState.shouldNavigateToResult) {
                        val route = NavigationHelper.buildResultRoute(
                            imageUri = uiState.lastPhotoUri.toString(),
                            disease = uiState.classification ?: "",
                            confidence = uiState.confidence ?: 0f,
                            detectionId = uiState.savedDetectionId ?: 0L
                        )
                        navController.navigate(route)
                        vm.onNavigatedToResult()
                    }
                }
                
                ScannerScreen(padding) 
            }
            
            // History
            composable(NavigationHelper.Routes.HISTORY) { 
                HistoryScreen(
                    innerPadding = padding,
                    onNavigateToResult = { imageUri, disease, confidence, detectionId ->
                        val route = NavigationHelper.buildResultRoute(
                            imageUri = imageUri,
                            disease = disease,
                            confidence = confidence,
                            detectionId = detectionId
                        )
                        navController.navigate(route)
                    }
                )
            }
            
            // Help
            composable(NavigationHelper.Routes.HELP) { 
                HelpScreen(padding) 
            }
            
            // Result
            composable(
                route = NavigationHelper.Routes.RESULT_FULL,
                arguments = listOf(
                    navArgument(NavigationHelper.Args.IMAGE_URI) { type = NavType.StringType },
                    navArgument(NavigationHelper.Args.DISEASE) { type = NavType.StringType },
                    navArgument(NavigationHelper.Args.CONFIDENCE) { type = NavType.FloatType },
                    navArgument(NavigationHelper.Args.DETECTION_ID) { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val encodedUri = backStackEntry.arguments?.getString(NavigationHelper.Args.IMAGE_URI) ?: ""
                val encodedDisease = backStackEntry.arguments?.getString(NavigationHelper.Args.DISEASE) ?: ""
                val confidence = backStackEntry.arguments?.getFloat(NavigationHelper.Args.CONFIDENCE) ?: 0f
                val detectionId = backStackEntry.arguments?.getLong(NavigationHelper.Args.DETECTION_ID)
                
                ResultScreen(
                    imageUri = NavigationHelper.decodeUri(encodedUri),
                    disease = NavigationHelper.decodeUri(encodedDisease),
                    confidence = confidence,
                    detectionId = detectionId,
                    onBack = { navController.popBackStack() },
                    onDeleted = { navController.popBackStack() }
                )
            }
        }
    }
}
