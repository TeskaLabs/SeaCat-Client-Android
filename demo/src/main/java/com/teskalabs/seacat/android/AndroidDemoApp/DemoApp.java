package com.teskalabs.seacat.android.AndroidDemoApp;

import android.app.Application;

import com.teskalabs.seacat.android.client.SeaCatClient;

public class DemoApp extends Application
{

    @Override
    public void onCreate()
    {
        super.onCreate();

        // Enable SeaCat
        SeaCatClient.setCSRWorker(null); // Disable default CSR worker
        SeaCatClient.initialize(getApplicationContext());
    }

}
