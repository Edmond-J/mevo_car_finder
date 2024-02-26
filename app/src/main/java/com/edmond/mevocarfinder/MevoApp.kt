package com.edmond.mevocarfinder

import android.app.Application
import com.mapbox.dash.sdk.Dash
import com.mapbox.dash.sdk.config.api.DashConfig

class MevoApp : Application() {

    override fun onCreate() {
        super.onCreate()

        val config = DashConfig.create(
            applicationContext = applicationContext,
            accessToken = getString(R.string.mapbox_access_token)
        )
        Dash.init(config)
    }
}