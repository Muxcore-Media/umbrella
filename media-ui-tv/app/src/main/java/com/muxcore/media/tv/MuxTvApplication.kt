package com.muxcore.media.tv

import android.app.Application
import com.muxcore.media.tv.data.Capabilities
import com.muxcore.media.tv.data.DefaultCapabilities
import com.muxcore.media.tv.data.MuxRepository
import com.muxcore.media.tv.data.PreferencesStore
import com.muxcore.media.tv.data.UserdataStore

class MuxTvApplication : Application() {
    lateinit var preferences: PreferencesStore
    lateinit var repository: MuxRepository
    lateinit var userdata: UserdataStore
    var capabilities: Capabilities = DefaultCapabilities

    override fun onCreate() {
        super.onCreate()
        preferences = PreferencesStore(this)
        repository = MuxRepository(preferences)
        userdata = UserdataStore(this, repository)
    }
}
