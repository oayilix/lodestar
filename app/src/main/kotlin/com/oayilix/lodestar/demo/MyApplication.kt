package com.oayilix.lodestar.demo

import android.app.Application
import com.oayilix.lodestar.api.Lodestar

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Lodestar.initOrThrow()
    }
}
