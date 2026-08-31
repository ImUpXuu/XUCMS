package com.upxuu.xucms.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Creates a [ViewModel] from a plain lambda, keeping the app free of a DI
 * framework while still getting proper lifecycle scoping.
 */
@Composable
inline fun <reified VM : ViewModel> rememberViewModel(
  key: String? = null,
  crossinline builder: () -> VM,
): VM = viewModel(
  key = key,
  factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = builder() as T
  },
)
