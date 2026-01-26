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
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route
            
            if (currentRoute?.startsWith("result") != true) {
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
            startDestination = Screen.Scanner.route
        ) {
            composable(Screen.Scanner.route) { 
                // viewModel() sin parámetros (usa ViewModelProvider por defecto)
                val vm: ScannerViewModel = viewModel()
                val uiState by vm.uiState.collectAsState()
                
                LaunchedEffect(uiState.shouldNavigateToResult) {
                    if (uiState.shouldNavigateToResult) {
                        val encodedUri = URLEncoder.encode(
                            uiState.lastPhotoUri.toString(),
                            StandardCharsets.UTF_8.toString()
                        )
                        val disease = URLEncoder.encode(
                            uiState.classification ?: "",
                            StandardCharsets.UTF_8.toString()
                        )
                        val confidence = uiState.confidence ?: 0f
                        val detectionId = uiState.savedDetectionId ?: 0L
                        
                        navController.navigate("result/$encodedUri/$disease/$confidence/$detectionId")
                        vm.onNavigatedToResult()
                    }
                }
                
                ScannerScreen(padding) 
            }
            
            composable(Screen.History.route) { 
                HistoryScreen(
                    innerPadding = padding,
                    onNavigateToResult = { imageUri, disease, confidence, detectionId ->
                        val encodedUri = URLEncoder.encode(
                            imageUri,
                            StandardCharsets.UTF_8.toString()
                        )
                        val encodedDisease = URLEncoder.encode(
                            disease,
                            StandardCharsets.UTF_8.toString()
                        )
                        navController.navigate("result/$encodedUri/$encodedDisease/$confidence/$detectionId")
                    }
                )
            }
            
            composable(Screen.Help.route) { HelpScreen(padding) }
            
            composable(
                route = "result/{imageUri}/{disease}/{confidence}/{detectionId}",
                arguments = listOf(
                    navArgument("imageUri") { type = NavType.StringType },
                    navArgument("disease") { type = NavType.StringType },
                    navArgument("confidence") { type = NavType.FloatType },
                    navArgument("detectionId") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val encodedUri = backStackEntry.arguments?.getString("imageUri") ?: ""
                val encodedDisease = backStackEntry.arguments?.getString("disease") ?: ""
                val detectionId = backStackEntry.arguments?.getLong("detectionId")
                
                ResultScreen(
                    imageUri = URLDecoder.decode(encodedUri, StandardCharsets.UTF_8.toString()),
                    disease = URLDecoder.decode(encodedDisease, StandardCharsets.UTF_8.toString()),
                    confidence = backStackEntry.arguments?.getFloat("confidence") ?: 0f,
                    detectionId = detectionId,
                    onBack = { navController.popBackStack() },
                    onDeleted = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}