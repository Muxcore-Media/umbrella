package systems.zem.muxcore.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState

/** Simple async load state for screen-level fetches. */
sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Ready<T>(val value: T) : Load<T>
    data class Failed(val message: String) : Load<Nothing>
}

/** Fetch [block] once per [keys] change; re-runs when keys change. */
@Composable
fun <T> rememberLoad(vararg keys: Any?, block: suspend () -> T): State<Load<T>> =
    produceState<Load<T>>(initialValue = Load.Loading, keys = keys) {
        value = Load.Loading
        value = try {
            Load.Ready(block())
        } catch (e: Exception) {
            Load.Failed(e.message ?: "Something went wrong")
        }
    }
