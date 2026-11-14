package com.easyhome.senior.ui.common

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

/** Carica in background l'icona di un'app (con cache nel repository) e la espone come stato. */
@Composable
fun rememberAppIcon(
    packageName: String,
    sizePx: Int,
    loader: suspend (String, Int) -> Bitmap?,
): Bitmap? {
    val state: MutableState<Bitmap?> = remember(packageName, sizePx) { mutableStateOf(null) }
    LaunchedEffect(packageName, sizePx) {
        state.value = if (packageName.isNotEmpty()) loader(packageName, sizePx) else null
    }
    return state.value
}
