package com.upxuu.xucms.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
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
import java.net.URLDecoder
import java.net.URLEncoder

object Routes {
  const val LOGIN = "login"
  const val HOME = "home"
  const val GALLERY = "gallery"
  const val SETTINGS = "settings"
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
  // fades so two moving surfaces never fight for attention.
  val enterSpec = tween<Float>(220, easing = FastOutSlowInEasing)

  NavHost(
    navController = navController,
    startDestination = start,
    enterTransition = {
      fadeIn(enterSpec) + slideInHorizontally(tween(260, easing = FastOutSlowInEasing)) { it / 10 } +
        scaleIn(enterSpec, initialScale = 0.98f)
    },
    exitTransition = { fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.99f) },
    popEnterTransition = { fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.99f) },
    popExitTransition = {
      fadeOut(tween(160)) + slideOutHorizontally(tween(220, easing = FastOutSlowInEasing)) { it / 10 }
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
        onSignedOut = {
          navController.navigate(Routes.LOGIN) { popUpTo(0) }
        },
      )
    }

    composable(Routes.ABOUT) {
      AboutScreen(onBack = { navController.popBackStack() })
    }
  }
}
