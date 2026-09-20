package com.d3ff96.twitchmodpanel

import android.app.Application
import com.d3ff96.twitchmodpanel.data.repository.AppContainer

class TwitchModPanelApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContainer.init(this)
    }
}
