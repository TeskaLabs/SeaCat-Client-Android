package com.teskalabs.seacat.android.AndroidDemoApp;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.teskalabs.seacat.android.client.SeaCatClient;

import java.io.IOException;

public class DemoApp extends Application
{
    private BroadcastReceiver receiver;

    @Override
    public void onCreate()
    {
        super.onCreate();

        try {
            SeaCatClient.setLogMask(SeaCatClient.LogFlag.ALL_SET);
        } catch (IOException e) {
            e.printStackTrace();
        }

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.hasCategory(SeaCatClient.CATEGORY_SEACAT))
                {
                    String action = intent.getAction();

                    if (action.equals(SeaCatClient.ACTION_SEACAT_USER_AUTH_NEEDED)) {
                        Intent n_intent = new Intent(context, AuthActivity.class);
                        n_intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(n_intent);
                        return;
                    }

                    else if (action.equals(SeaCatClient.ACTION_SEACAT_SECURE_LOCK_NEEDED)) {
                        Intent n_intent = new Intent(context, AuthActivity.class);
                        n_intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(n_intent);
                        return;
                    }
                }

                Log.w(SplashActivity.class.getCanonicalName(), "Unexpected intent: "+intent);
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SeaCatClient.ACTION_SEACAT_USER_AUTH_NEEDED);
        intentFilter.addAction(SeaCatClient.ACTION_SEACAT_SECURE_LOCK_NEEDED);
        intentFilter.addCategory(SeaCatClient.CATEGORY_SEACAT);
        registerReceiver(receiver, intentFilter);


        // Enable SeaCat
        SeaCatClient.initialize(getApplicationContext());
    }

    private DemoPlugin demoSeaCatPlugin = new DemoPlugin();

}
