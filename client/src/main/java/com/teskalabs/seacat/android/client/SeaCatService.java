package com.teskalabs.seacat.android.client;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.teskalabs.seacat.android.client.core.Reactor;

public class SeaCatService extends Service
{
    static SeaCatService instance = null;

    public SeaCatService()
    {
        instance = this;
    }

    @Override
    public void onCreate()
    {
        if (SeaCatClient.getReactor() != null)
        {
            Log.e(SeaCatInternals.L, "Reactor is already created!");
            return;
        }

        try {
            SeaCatClient.setReactor(new Reactor(getApplicationContext(), SeaCatInternals.applicationIdSuffix));
        } catch (Exception e) {
            Log.e(SeaCatInternals.L, "Reactor shutdown failed:", e);
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Reactor reactor = SeaCatClient.getReactor();

        if (reactor != null)
        {
            if (!reactor.isStarted())
                reactor.start();
        }
        else
        {
            Log.e(SeaCatInternals.L, "Reactor is broken!");
        }

        // We want this service to continue running until it is explicitly stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        Reactor reactor = SeaCatClient.getReactor();
        if (reactor != null)
        {
            try {
                reactor.shutdown();
            } catch (Exception e) {
                Log.e(SeaCatInternals.L, "Reactor shutdown failed:", e);
            }
            SeaCatClient.setReactor(null);
        }
    }


    // Binder

    public class LocalBinder extends Binder {
        SeaCatService getService()
        {
            return SeaCatService.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent)
    {
        return mBinder;
    }
}
