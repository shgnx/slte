package com.slte.app.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.slte.app.ui.screen.forgot.ForgotPasswordScreen
import com.slte.app.ui.screen.login.LoginScreen
import com.slte.app.ui.screen.register.RegisterScreen

/**
 * 认证流导航：登录页 / 忘记密码页 / 注册页。
 *
 * 页面切换采用 iOS 风格水平滑动 + 轻淡入淡出，
 * 推入从右侧滑入，返回向右侧滑出，时长 280ms。
 * 登录/注册成功后由 AuthRepository.sessionState StateFlow 自动切换到主界面。
 */
/** 页面转场时长（毫秒）：登录前后两套导航共用，避免两处各写一份 280 */
internal const val PAGE_TRANSITION_DURATION = 280

@Composable
fun AuthNavGraph() {
    val navController = rememberNavController()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = Routes.AUTH_LOGIN,
                enterTransition = {
                    slideInHorizontally(tween(PAGE_TRANSITION_DURATION)) { it / 3 } +
                        fadeIn(tween(PAGE_TRANSITION_DURATION))
                },
                exitTransition = {
                    slideOutHorizontally(tween(PAGE_TRANSITION_DURATION)) { -it / 3 } +
                        fadeOut(tween(PAGE_TRANSITION_DURATION))
                },
                popEnterTransition = {
                    slideInHorizontally(tween(PAGE_TRANSITION_DURATION)) { -it / 3 } +
                        fadeIn(tween(PAGE_TRANSITION_DURATION))
                },
                popExitTransition = {
                    slideOutHorizontally(tween(PAGE_TRANSITION_DURATION)) { it / 3 } +
                        fadeOut(tween(PAGE_TRANSITION_DURATION))
                },
            ) {
                composable(Routes.AUTH_LOGIN) {
                    LoginScreen(
                        onForgotPassword = {
                            navController.navigate(Routes.AUTH_FORGOT)
                        },
                        onCreateAccount = { emailVerify, inviteForce ->
                            navController.navigate(
                                "${Routes.AUTH_REGISTER}?emailVerify=$emailVerify&inviteForce=$inviteForce",
                            )
                        },
                    )
                }
                composable(Routes.AUTH_FORGOT) {
                    ForgotPasswordScreen(
                        onBackToLogin = {
                            navController.popBackStack()
                        },
                        onResetSuccess = {
                            navController.popBackStack(Routes.AUTH_LOGIN, inclusive = false)
                        },
                    )
                }
                composable(
                    route = "${Routes.AUTH_REGISTER}?emailVerify={emailVerify}&inviteForce={inviteForce}",
                    arguments =
                    listOf(
                        navArgument("emailVerify") {
                            type = NavType.BoolType
                            defaultValue = false
                        },
                        navArgument("inviteForce") {
                            type = NavType.BoolType
                            defaultValue = false
                        },
                    ),
                ) { backStackEntry ->
                    val emailVerify = backStackEntry.arguments?.getBoolean("emailVerify") ?: false
                    val inviteForce = backStackEntry.arguments?.getBoolean("inviteForce") ?: false
                    RegisterScreen(
                        emailVerifyEnabled = emailVerify,
                        inviteForceEnabled = inviteForce,
                        onBackToLogin = {
                            navController.popBackStack()
                        },
                    )
                }
            }
        }
    }
}
