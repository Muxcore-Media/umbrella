package systems.zem.muxcore

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kotlinx.coroutines.launch
import systems.zem.muxcore.ui.MuxApp

class MainActivity : ComponentActivity() {

    /** Invite token arriving via deep link (https://mux.zem.systems/invite/<token>). */
    private var pendingInviteToken by mutableStateOf<String?>(null)

    /** Set by the player screen so onUserLeaveHint can trigger PiP. */
    var pipEnterRequest: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as MuxCoreApp).container
        container.api.onUnauthorized = {
            // Clear on the app scope; UI reacts via SessionStore.state flow.
            container.appScope.launch { container.session.clearSession() }
        }
        handleIntent(intent)

        setContent {
            MuxApp(
                container = container,
                inviteToken = pendingInviteToken,
                onInviteConsumed = { pendingInviteToken = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        pipEnterRequest?.invoke()
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.pathSegments.firstOrNull() == "invite") {
            pendingInviteToken = data.lastPathSegment
        }
    }
}
