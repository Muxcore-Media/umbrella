package systems.zem.muxcore.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Live top inset for the status bar / display cutout (varies by device and orientation). */
@Composable
fun statusBarTop(): Dp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

@Composable
fun statusBarPaddingValues(): PaddingValues = WindowInsets.statusBars.asPaddingValues()

/** Top padding for lazy lists whose first row is a title (not a full-bleed hero). */
@Composable
fun listTopContentPadding(extraTop: Dp = 0.dp, horizontal: Dp = 0.dp, bottom: Dp = 0.dp): PaddingValues {
    val top = statusBarTop() + extraTop
    return PaddingValues(start = horizontal, end = horizontal, top = top, bottom = bottom)
}

/** Keeps interactive chrome below the status bar while imagery can remain full-bleed. */
fun Modifier.safeStatusBarPadding(): Modifier = statusBarsPadding()
