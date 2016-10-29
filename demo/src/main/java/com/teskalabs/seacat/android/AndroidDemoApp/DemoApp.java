package com.teskalabs.seacat.android.AndroidDemoApp;

import android.app.Application;
import com.teskalabs.seacat.android.client.SeaCatClient;

import java.io.IOException;

public class DemoApp extends Application
{

    @Override
    public void onCreate()
    {
        super.onCreate();

        try {
            SeaCatClient.setLogMask(SeaCatClient.LogFlag.ALL_SET);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Enable SeaCat
        SeaCatClient.initialize(getApplicationContext(), "foobar2");
    }

}
