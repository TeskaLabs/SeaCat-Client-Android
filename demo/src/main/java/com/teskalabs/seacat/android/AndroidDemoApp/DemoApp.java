package com.teskalabs.seacat.android.AndroidDemoApp;

import android.app.Application;
import android.content.Intent;

import com.teskalabs.seacat.android.client.SeaCatClient;
import com.teskalabs.seacat.android.client.SeaCatService;

public class DemoApp extends Application
{

    @Override
    public void onCreate()
    {
        super.onCreate();

        // Enable SeaCat
        SeaCatClient.setCSRWorker(null); // Disable default CSR worker
        startService(new Intent(getBaseContext(), SeaCatService.class));
    }

}
