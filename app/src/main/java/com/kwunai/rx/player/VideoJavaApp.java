package com.kwunai.rx.player;

import android.app.Application;

import com.kwunai.neplayer.NEPlayerManager;

public class VideoJavaApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        NEPlayerManager.init(this);
    }
}
