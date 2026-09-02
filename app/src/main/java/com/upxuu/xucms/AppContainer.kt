package com.upxuu.xucms

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.upxuu.xucms.data.DraftStore
import com.upxuu.xucms.data.SettingsStore
import com.upxuu.xucms.data.UpdateChecker
import com.upxuu.xucms.data.XucmsApi

/** Hand-rolled dependency container; the app is small enough not to need Hilt. */
class AppContainer(context: Context) {
  val settings = SettingsStore(context)
  val drafts = DraftStore(context)
  val api = XucmsApi(settings)
  val updates = UpdateChecker(context, settings)
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
  error("AppContainer not provided")
}
