package com.oayilix.lodestar.demo

import android.app.Application
import com.oayilix.lodestar.api.Router

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Router.init()
    }
}
