package com.example.voxtech;

import android.app.Application;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Config.init(this);
    }
}
