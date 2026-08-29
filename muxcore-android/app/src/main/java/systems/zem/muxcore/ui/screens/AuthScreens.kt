package systems.zem.muxcore.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import systems.zem.muxcore.data.ApiException
import systems.zem.muxcore.ui.Routes
import systems.zem.muxcore.ui.appContainer
import systems.zem.muxcore.ui.components.ErrorBanner

private const val DEFAULT_SERVER = "https://mux.zem.systems"

/** Extracts the value of the auth-local `session` cookie from a Cookie header string. */
internal fun sessionCookieFrom(cookieHeader: String?): String? {
    if (cookieHeader.isNullOrEmpty()) return null
    return cookieHeader.split(';')
        .map { it.trim() }
        .firstOrNull { it.startsWith("session=") }
        ?.removePrefix("session=")
        ?.takeIf { it.isNotEmpty() }
}

/** Auth callback URLs carry a one-time code — mux BFF or muxcore:// mobile redirect. */
internal fun authCallbackCode(url: String?, serverUrl: String): String? {
    if (url.isNullOrBlank()) return null
    return try {
        val uri = java.net.URI(url)
        val code = queryParam(uri, "code")
        if (code.isNullOrEmpty()) return null

        val scheme = uri.scheme?.lowercase()
        val host = uri.host?.lowercase()
        if (scheme == "muxcore" && host == "auth") return code

        val server = java.net.URI(serverUrl)
        val serverHost = server.host?.lowercase()
        if (serverHost == null || host != serverHost) return null
        val path = uri.path.orEmpty()
        if (path == "/auth/callback" || path.startsWith("/auth/callback/")) return code
        null
    } catch (_: Exception) {
        null
    }
}

internal fun isMuxcoreAuthDeepLink(url: String?): Boolean {
    if (url.isNullOrBlank()) return false
    return try {
        val uri = java.net.URI(url)
        uri.scheme?.lowercase() == "muxcore" && uri.host?.lowercase() == "auth"
    } catch (_: Exception) {
        false
    }
}

private fun queryParam(uri: java.net.URI, name: String): String? {
    val raw = uri.rawQuery ?: return null
    for (part in raw.split('&')) {
        val eq = part.indexOf('=')
        val key = if (eq >= 0) part.substring(0, eq) else part
        if (key == name) {
            val value = if (eq >= 0) part.substring(eq + 1) else ""
            return value.trim().takeIf { it.isNotEmpty() }
        }
    }
    return null
}

/** Logged-out flow: server URL entry → web login. Also hosts invite join + forgot password. */
@Composable
fun LoginFlow(inviteToken: String?, onInviteConsumed: () -> Unit) {
    var step by rememberSaveable { mutableStateOf("server") }
    var inviteTokenState by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(inviteToken) {
        if (inviteToken != null) {
            inviteTokenState = inviteToken
            step = "invite"
            onInviteConsumed()
        }
    }

    when (step) {
        "server" -> ServerUrlScreen(
            onContinue = { step = "login" },
            onForgotPassword = { step = "forgot" },
        )
        "login" -> WebLoginScreen(
            onBack = { step = "server" },
            onLoggedIn = { /* session flow flips MuxApp to LoggedInApp */ },
        )
        "forgot" -> ForgotPasswordContent(onBack = { step = "server" })
        "invite" -> InviteJoinContent(
            token = inviteTokenState.orEmpty(),
            onDone = { step = "login" },
            onBack = { step = "server" },
        )
    }
}

@Composable
private fun ServerUrlScreen(onContinue: () -> Unit, onForgotPassword: () -> Unit) {
    val container = appContainer()
    val scope = rememberCoroutineScope()
    var url by rememberSaveable {
        mutableStateOf(container.session.cache.serverUrl.ifEmpty { DEFAULT_SERVER })
    }
    var error by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
    ) {
        Text("MuxCore", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text(
            "Your movies and shows, anywhere.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(36.dp))
        OutlinedTextField(
            value = url,
            onValueChange = { url = it; error = null },
            label = { Text("Server URL") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth(),
        )
        if (error != null) {
            Spacer(Modifier.height(10.dp))
            ErrorBanner(error!!, modifier = Modifier.padding(horizontal = 0.dp))
        }
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = {
                val trimmed = url.trim().trimEnd('/')
                if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                    error = "Enter a full URL, e.g. https://mux.zem.systems"
                    return@Button
                }
                checking = true
                scope.launch {
                    container.session.setServerUrl(trimmed)
                    checking = false
                    onContinue()
                }
            },
            enabled = !checking,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (checking) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text("Continue")
            }
        }
        Spacer(Modifier.height(10.dp))
        TextButton(onClick = onForgotPassword) { Text("Forgot password?") }
    }
}

/** WebView login: mux /login → auth.zem.systems → mux /auth/callback?code=… */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebLoginScreen(onBack: () -> Unit, onLoggedIn: () -> Unit) {
    val container = appContainer()
    val api = container.api
    val scope = rememberCoroutineScope()
    val serverUrl = container.session.cache.serverUrl
    var loading by remember { mutableStateOf(true) }
    var captured by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun completeLogin(code: String, mobile: Boolean) {
        if (captured) return
        captured = true
        error = null
        scope.launch {
            try {
                val (token, username) = if (mobile) {
                    api.establishSessionFromMobileCode(code)
                } else {
                    val token = api.establishSessionFromAuthCode(code)
                    token to null
                }
                container.session.setSession(token, username)
                onLoggedIn()
            } catch (e: Exception) {
                captured = false
                error = e.message ?: "Sign-in failed"
            }
        }
    }

    fun inspectUrl(url: String?): Boolean {
        val code = authCallbackCode(url, serverUrl)
        if (code != null) {
            completeLogin(code, isMuxcoreAuthDeepLink(url))
            return true
        }
        return isMuxcoreAuthDeepLink(url)
    }

    Scaffold(
        topBar = { AuthTopBar(title = "Sign in", onBack = onBack) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean =
                                inspectUrl(url)

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?,
                            ): Boolean = inspectUrl(request?.url?.toString())

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                loading = true
                                inspectUrl(url)
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                loading = false
                                inspectUrl(url)
                            }
                        }
                        loadUrl("$serverUrl/login")
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
            if (loading && error == null) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
            if (error != null) {
                ErrorBanner(
                    error!!,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AuthTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
    )
}

// ---------------------------------------------------------------------------
// Forgot password (shared logged-out + logged-in)

@Composable
internal fun ForgotPasswordContent(onBack: () -> Unit) {
    val api = appContainer().api
    val scope = rememberCoroutineScope()
    var username by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Scaffold(topBar = { AuthTopBar("Forgot password", onBack) }) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Text(
                "Send a reset request to your server admin. They'll get a notification and reset your password.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note for admin (optional)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            if (error != null) ErrorBanner(error!!, modifier = Modifier.padding(horizontal = 0.dp))
            if (message != null) {
                Text(message!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            Button(
                onClick = {
                    busy = true
                    error = null
                    scope.launch {
                        try {
                            val res = api.requestPasswordReset(username.trim(), note.trim())
                            message = res.ifEmpty { "Request sent. Your admin will follow up." }
                        } catch (e: Exception) {
                            error = e.message ?: "Could not send the request"
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = username.isNotBlank() && !busy && message == null,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (busy) "Sending…" else "Send request") }
        }
    }
}

@Composable
fun ForgotPasswordScreen(nav: NavHostController) {
    ForgotPasswordContent(onBack = { nav.popBackStack() })
}

// ---------------------------------------------------------------------------
// Invite join (create account from an invite token)

@Composable
internal fun InviteJoinContent(token: String, onDone: () -> Unit, onBack: () -> Unit) {
    val api = appContainer().api
    val scope = rememberCoroutineScope()
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var peekError by remember { mutableStateOf<String?>(null) }
    var role by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }

    LaunchedEffect(token) {
        if (token.isEmpty()) {
            peekError = "This invite link is missing its token."
            return@LaunchedEffect
        }
        try {
            val peek = api.invitePeek(token)
            if (!peek.valid) peekError = peek.error ?: "This invite is no longer valid."
            else role = peek.role
        } catch (e: Exception) {
            peekError = (e as? ApiException)?.message ?: "Could not verify this invite."
        }
    }

    Scaffold(topBar = { AuthTopBar("Join MuxCore", onBack) }) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            when {
                peekError != null -> ErrorBanner(peekError!!, modifier = Modifier.padding(horizontal = 0.dp))
                done -> {
                    Text(
                        "Account created. Sign in with your new credentials.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Go to sign in") }
                }
                else -> {
                    Text(
                        "You've been invited to this MuxCore server" +
                            (role?.let { " as $it" } ?: "") + ". Pick a username and password.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = confirm,
                        onValueChange = { confirm = it },
                        label = { Text("Confirm password") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (error != null) ErrorBanner(error!!, modifier = Modifier.padding(horizontal = 0.dp))
                    Button(
                        onClick = {
                            if (password != confirm) {
                                error = "Passwords do not match."
                                return@Button
                            }
                            busy = true
                            error = null
                            scope.launch {
                                try {
                                    api.inviteRedeem(token, username.trim(), password)
                                    done = true
                                } catch (e: Exception) {
                                    error = e.message ?: "Could not create the account"
                                } finally {
                                    busy = false
                                }
                            }
                        },
                        enabled = username.isNotBlank() && password.length >= 4 && !busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (busy) "Creating…" else "Create account") }
                }
            }
        }
    }
}

@Composable
fun InviteJoinScreen(nav: NavHostController, token: String) {
    InviteJoinContent(
        token = token,
        onDone = { nav.popBackStack() },
        onBack = { nav.popBackStack() },
    )
}

// ---------------------------------------------------------------------------
// Quick Connect (approve a code for another device; requires session)

@Composable
fun QuickConnectScreen(nav: NavHostController) {
    val api = appContainer().api
    val scope = rememberCoroutineScope()
    var code by rememberSaveable { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Scaffold(topBar = { AuthTopBar("Quick Connect", onBack = { nav.popBackStack() }) }) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Text(
                "Enter the code shown on the device you want to sign in.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = code,
                onValueChange = { code = it.uppercase(); message = null; error = null },
                label = { Text("Code") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                modifier = Modifier.fillMaxWidth(),
            )
            if (error != null) ErrorBanner(error!!, modifier = Modifier.padding(horizontal = 0.dp))
            if (message != null) {
                Text(message!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            Button(
                onClick = {
                    busy = true
                    error = null
                    scope.launch {
                        try {
                            val res = api.approveQuickConnect(code.trim())
                            message = res.ifEmpty { "Device approved." }
                            code = ""
                        } catch (e: Exception) {
                            error = e.message ?: "Could not approve the code"
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = code.isNotBlank() && !busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (busy) "Approving…" else "Approve") }
        }
    }
}
