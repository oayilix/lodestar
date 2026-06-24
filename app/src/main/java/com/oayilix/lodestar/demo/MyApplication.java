package com.oayilix.lodestar.demo;

import android.app.Application;

import com.oayilix.lodestar.api.Router;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Router.init();
    }
}
