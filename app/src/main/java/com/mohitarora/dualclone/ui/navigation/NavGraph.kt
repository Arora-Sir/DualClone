package com.mohitarora.dualclone.ui.navigation

import android.app.Activity
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mohitarora.dualclone.core.designsystem.DcMotion
import com.mohitarora.dualclone.core.designsystem.rememberReduceMotion
import com.mohitarora.dualclone.ui.viewmodel.ProfileSelectionViewModel

object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val DEV_INFO = "dev_info"
    const val AUDIT_LOG = "audit_log"
    const val BACKUP = "backup"
    const val ORPHANS = "orphans"
    const val MORE = "more"
}

private const val DURATION = 300

/**
 * Shared axis transitions. Forward moves slide 96dp and fade, back reverses them.
 * Navigation 2.9 drives the same transitions from the predictive back gesture.
 */
@Composable
fun DualCloneNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val activity = LocalActivity.current as ComponentActivity
    // NOTE: Activity scoped so every screen shares one selected profile and the package monitor starts once.
    hiltViewModel<ProfileSelectionViewModel>(viewModelStoreOwner = activity)

    val distance = with(LocalDensity.current) { DcMotion.slideDistance.roundToPx() }
    val reduce = rememberReduceMotion()
    val spec = tween<Float>(if (reduce) 0 else DURATION)
    val slideSpec = tween<androidx.compose.ui.unit.IntOffset>(if (reduce) 0 else DURATION)

    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (reduce) fadeIn(spec) else slideInHorizontally(slideSpec) { distance } + fadeIn(spec)
    }
    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (reduce) fadeOut(spec) else slideOutHorizontally(slideSpec) { -distance } + fadeOut(spec)
    }
    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (reduce) fadeIn(spec) else slideInHorizontally(slideSpec) { -distance } + fadeIn(spec)
    }
    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (reduce) fadeOut(spec) else slideOutHorizontally(slideSpec) { distance } + fadeOut(spec)
    }

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier,
        enterTransition = enter,
        exitTransition = exit,
        popEnterTransition = popEnter,
        popExitTransition = popExit
    ) {
        composable(Routes.HOME) {
            HomeRoute(onSettings = { navController.navigate(Routes.SETTINGS) })
        }
        composable(Routes.SETTINGS) {
            SettingsRoute(
                onBack = { navController.popBackStack() },
                onMore = { navController.navigate(Routes.MORE) },
                onBackup = { navController.navigate(Routes.BACKUP) },
                onOrphans = { navController.navigate(Routes.ORPHANS) }
            )
        }
        composable(Routes.DEV_INFO) {
            DevInfoRoute(
                onBack = { navController.popBackStack() },
                onAuditLog = { navController.navigate(Routes.AUDIT_LOG) }
            )
        }
        composable(Routes.MORE) {
            MoreRoute(
                onBack = { navController.popBackStack() },
                onAuditLog = { navController.navigate(Routes.AUDIT_LOG) },
                onDiagnostics = { navController.navigate(Routes.DEV_INFO) }
            )
        }
        composable(Routes.AUDIT_LOG) { AuditLogRoute(onBack = { navController.popBackStack() }) }
        composable(Routes.BACKUP) { BackupRoute(onBack = { navController.popBackStack() }) }
        composable(Routes.ORPHANS) { OrphansRoute(onBack = { navController.popBackStack() }) }
    }
}
