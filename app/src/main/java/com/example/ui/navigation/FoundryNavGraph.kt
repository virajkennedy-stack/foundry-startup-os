package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import com.example.data.ChatRepository
import com.example.data.UserRepository
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.auth.SignUpScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.onboarding.OnboardingScreen
import com.example.ui.screens.placeholder.WorkspaceDetailScreen
import com.example.ui.screens.settings.PlanComparisonScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.splash.SplashScreen
import com.example.ui.screens.welcome.WelcomeScreen

object FoundryDestinations {
    const val SPLASH = "splash"
    const val WELCOME = "welcome"
    const val LOGIN = "login"
    const val SIGN_UP = "sign_up"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val PLAN_COMPARISON = "plan_comparison"
    const val WORKSPACE = "workspace/{workspaceId}"
}

@Composable
fun FoundryAppNavigation(
    userRepository: UserRepository,
    chatRepository: ChatRepository,
    navController: NavHostController = rememberNavController()
) {
    val activeUser by userRepository.activeUserFlow.collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = FoundryDestinations.SPLASH
    ) {
        composable(FoundryDestinations.SPLASH) {
            SplashScreen(
                userRepository = userRepository,
                onNavigateToWelcome = {
                    navController.navigate(FoundryDestinations.WELCOME) {
                        popUpTo(FoundryDestinations.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToOnboarding = {
                    navController.navigate(FoundryDestinations.ONBOARDING) {
                        popUpTo(FoundryDestinations.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(FoundryDestinations.HOME) {
                        popUpTo(FoundryDestinations.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(FoundryDestinations.WELCOME) {
            WelcomeScreen(
                onNavigateToLogin = { navController.navigate(FoundryDestinations.LOGIN) },
                onNavigateToSignUp = { navController.navigate(FoundryDestinations.SIGN_UP) }
            )
        }

        composable(FoundryDestinations.LOGIN) {
            LoginScreen(
                userRepository = userRepository,
                onLoginSuccess = {
                    coroutineScope.launch {
                        val user = userRepository.getActiveUser()
                        if (user?.isOnboardingComplete == true) {
                            navController.navigate(FoundryDestinations.HOME) {
                                popUpTo(FoundryDestinations.WELCOME) { inclusive = true }
                            }
                        } else {
                            navController.navigate(FoundryDestinations.ONBOARDING) {
                                popUpTo(FoundryDestinations.WELCOME) { inclusive = true }
                            }
                        }
                    }
                },
                onNavigateToSignUp = {
                    navController.navigate(FoundryDestinations.SIGN_UP) {
                        popUpTo(FoundryDestinations.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(FoundryDestinations.SIGN_UP) {
            SignUpScreen(
                userRepository = userRepository,
                onSignUpSuccess = {
                    navController.navigate(FoundryDestinations.ONBOARDING) {
                        popUpTo(FoundryDestinations.WELCOME) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(FoundryDestinations.LOGIN) {
                        popUpTo(FoundryDestinations.SIGN_UP) { inclusive = true }
                    }
                }
            )
        }

        composable(FoundryDestinations.ONBOARDING) {
            OnboardingScreen(
                userRepository = userRepository,
                onOnboardingComplete = {
                    navController.navigate(FoundryDestinations.HOME) {
                        popUpTo(FoundryDestinations.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(FoundryDestinations.HOME) {
            HomeScreen(
                userRepository = userRepository,
                chatRepository = chatRepository,
                onNavigateToSettings = { navController.navigate(FoundryDestinations.SETTINGS) },
                onLogout = {
                    navController.navigate(FoundryDestinations.WELCOME) {
                        popUpTo(FoundryDestinations.HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(FoundryDestinations.SETTINGS) {
            SettingsScreen(
                userRepository = userRepository,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlanComparison = { navController.navigate(FoundryDestinations.PLAN_COMPARISON) },
                onLogout = {
                    navController.navigate(FoundryDestinations.WELCOME) {
                        popUpTo(FoundryDestinations.HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(FoundryDestinations.PLAN_COMPARISON) {
            PlanComparisonScreen(
                userRepository = userRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(FoundryDestinations.WORKSPACE) { backStackEntry ->
            val workspaceId = backStackEntry.arguments?.getString("workspaceId") ?: "default"
            WorkspaceDetailScreen(
                workspaceId = workspaceId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
