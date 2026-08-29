package systems.zem.muxcore

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import systems.zem.muxcore.data.ApiClient
import systems.zem.muxcore.data.SessionStore
import systems.zem.muxcore.data.UserDataStore

/** Manual DI container — one instance of each service for the app process. */
class AppContainer(app: Application) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val session = SessionStore(app)
    val api = ApiClient(session)
    val userData = UserDataStore(app, api, appScope)
}

class MuxCoreApp : Application(), SingletonImageLoader.Factory {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.session.warmCacheBlocking()
    }

    /** Coil loader that shares the API OkHttp client so poster requests carry the session cookie. */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { container.api.http }))
            }
            .crossfade(true)
            .build()
}
