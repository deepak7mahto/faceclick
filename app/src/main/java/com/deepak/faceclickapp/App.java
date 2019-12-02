package com.deepak.faceclickapp;

import android.app.Application;
import android.content.Context;

public class App extends Application {

    private static App app;

    public static Context getContext() {
        return app;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        app = this;

    }
}
