package com.upxuu.xucms.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.upxuu.xucms.LocalAppContainer
import com.upxuu.xucms.data.NoteKind
import com.upxuu.xucms.feature.editor.NoteEditorScreen
import com.upxuu.xucms.feature.gallery.GalleryScreen
import com.upxuu.xucms.feature.home.HomeScreen
import com.upxuu.xucms.feature.login.LoginScreen
import com.upxuu.xucms.feature.settings.AboutScreen
import com.upxuu.xucms.feature.settings.SettingsScreen
import com.upxuu.xucms.feature.settings.ToolbarSettingsScreen
import com.upxuu.xucms.ui.theme.Motion
import java.net.URLDecoder
import java.net.URLEncoder

object Routes {
  const val LOGIN = "login"
  const val HOME = "home"
  const val GALLERY = "gallery"
  const val SETTINGS = "settings"
  const val TOOLBAR = "settings/toolbar"
  const val ABOUT = "about"

  private const val EDITOR_BASE = "editor"
  const val EDITOR = "$EDITOR_BASE/{kind}?filename={filename}&sha={sha}"

  fun editor(kind: NoteKind, filename: String? = null, sha: String? = null): String {
    val f = filename?.let { URLEncoder.encode(it, "UTF-8") }.orEmpty()
    val s = sha?.let { URLEncoder.encode(it, "UTF-8") }.orEmpty()
    return "$EDITOR_BASE/${kind.name}?filename=$f&sha=$s"
  }
}

@Composable
fun XucmsApp(navController: NavHostController = rememberNavController()) {
  val container = LocalAppContainer.current
  val signedIn by container.settings.signedInFlow.collectAsState()
  val start = if (signedIn) Routes.HOME else Routes.LOGIN

  // Screens arrive from the right and lift very slightly; the outgoing screen only
  // fades so two moving surfaces never fight for attention. Curves come from Motion
  // so every transition in the app shares the same feel.
  NavHost(
    navController = navController,
    startDestination = start,
    enterTransition = {
      fadeIn(Motion.enterTween()) +
        slideInHorizontally(tween(Motion.NORMAL, easing = Motion.Decelerate)) { it / 12 } +
        scaleIn(Motion.enterTween(), initialScale = 0.985f)
    },
    exitTransition = { fadeOut(Motion.exitTween()) + scaleOut(Motion.exitTween(), targetScale = 0.99f) },
    popEnterTransition = { fadeIn(Motion.enterTween()) + scaleIn(Motion.enterTween(), initialScale = 0.99f) },
    popExitTransition = {
      fadeOut(Motion.exitTween()) +
        slideOutHorizontally(tween(Motion.NORMAL, easing = Motion.Accelerate)) { it / 12 }
    },
  ) {
    composable(Routes.LOGIN) {
      LoginScreen(
        onSignedIn = {
          navController.navigate(Routes.HOME) {
            popUpTo(Routes.LOGIN) { inclusive = true }
          }
        },
      )
    }

    composable(Routes.HOME) {
      HomeScreen(
        onOpenNote = { kind, summary ->
          navController.navigate(Routes.editor(kind, summary.name, summary.sha))
        },
        onOpenDraft = { draft ->
          navController.navigate(Routes.editor(draft.noteKind, draft.filename, draft.sha))
        },
        onCreate = { kind -> navController.navigate(Routes.editor(kind)) },
        onOpenGallery = { navController.navigate(Routes.GALLERY) },
        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
      )
    }

    composable(
      route = Routes.EDITOR,
      arguments = listOf(
        navArgument("kind") { type = NavType.StringType },
        navArgument("filename") { type = NavType.StringType; defaultValue = "" },
        navArgument("sha") { type = NavType.StringType; defaultValue = "" },
      ),
    ) { entry ->
      val kind = runCatching {
        NoteKind.valueOf(entry.arguments?.getString("kind") ?: NoteKind.POST.name)
      }.getOrDefault(NoteKind.POST)
      val filename = entry.arguments?.getString("filename")
        ?.takeIf { it.isNotBlank() }
        ?.let { URLDecoder.decode(it, "UTF-8") }
      val sha = entry.arguments?.getString("sha")
        ?.takeIf { it.isNotBlank() }
        ?.let { URLDecoder.decode(it, "UTF-8") }

      NoteEditorScreen(
        kind = kind,
        filename = filename,
        initialSha = sha,
        onBack = { navController.popBackStack() },
        onSessionExpired = {
          container.settings.signOut()
          navController.navigate(Routes.LOGIN) { popUpTo(0) }
        },
      )
    }

    composable(Routes.GALLERY) {
      GalleryScreen(onBack = { navController.popBackStack() })
    }

    composable(Routes.SETTINGS) {
      SettingsScreen(
        onBack = { navController.popBackStack() },
        onAbout = { navController.navigate(Routes.ABOUT) },
        onToolbar = { navController.navigate(Routes.TOOLBAR) },
        onSignedOut = {
          navController.navigate(Routes.LOGIN) { popUpTo(0) }
        },
      )
    }

    composable(Routes.TOOLBAR) {
      ToolbarSettingsScreen(onBack = { navController.popBackStack() })
    }

    composable(Routes.ABOUT) {
      AboutScreen(onBack = { navController.popBackStack() })
    }
  }
}
