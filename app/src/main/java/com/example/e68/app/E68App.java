package com.example.e68.app;

import android.app.Application;
import com.yandex.mapkit.MapKitFactory;
import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class E68App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        MapKitFactory.setApiKey("4868119d-06f4-4a09-9c36-9fdfcf546583");
        MapKitFactory.initialize(this);
    }
}