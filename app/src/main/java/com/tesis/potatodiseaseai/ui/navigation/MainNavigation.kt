package com.tesis.potatodiseaseai.ui.navigation

import android.net.Uri
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tesis.potatodiseaseai.ui.screens.*
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    
    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route
            
            // Ocultar bottom bar en pantalla de resultado
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
                val vm: ScannerViewModel = viewModel { ScannerViewModel(context) }
                val uiState by vm.uiState.collectAsState()
                
                // Navegar automáticamente al resultado
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
                        
                        navController.navigate("result/$encodedUri/$disease/$confidence")
                        vm.onNavigatedToResult()
                    }
                }
                
                ScannerScreen(padding) 
            }
            
            composable(Screen.History.route) { HistoryScreen(padding) }
            composable(Screen.Help.route) { HelpScreen(padding) }
            
            composable(
                route = "result/{imageUri}/{disease}/{confidence}",
                arguments = listOf(
                    navArgument("imageUri") { type = NavType.StringType },
                    navArgument("disease") { type = NavType.StringType },
                    navArgument("confidence") { type = NavType.FloatType }
                )
            ) { backStackEntry ->
                val encodedUri = backStackEntry.arguments?.getString("imageUri") ?: ""
                val encodedDisease = backStackEntry.arguments?.getString("disease") ?: ""
                
                val decodedUri = URLDecoder.decode(encodedUri, StandardCharsets.UTF_8.toString())
                val decodedDisease = URLDecoder.decode(encodedDisease, StandardCharsets.UTF_8.toString())
                
                ResultScreen(
                    imageUri = decodedUri,
                    disease = decodedDisease,
                    confidence = backStackEntry.arguments?.getFloat("confidence") ?: 0f,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}