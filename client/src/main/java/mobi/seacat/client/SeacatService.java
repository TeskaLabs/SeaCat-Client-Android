package mobi.seacat.client;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import mobi.seacat.client.core.Reactor;

public class SeaCatService extends Service
{
    public SeaCatService()
    {
        Log.i("SeaCatService", "SeaCatService()");
    }

    @Override
    public void onCreate()
    {
        Log.i("SeaCatService", "onCreate()");
        if (SeaCatClient.getReactor() != null)
        {
            Log.e("SeaCatService", "Reactor is already created!");
            return;
        }

        try {
            SeaCatClient.setReactor(new Reactor(getApplicationContext()));
        } catch (Exception e) {
            Log.e("SeaCatService", "Reactor shutdown failed:", e);
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        //TODO: Prevent double starts
        SeaCatClient.getReactor().start();

        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        try {
            SeaCatClient.getReactor().shutdown();
        } catch (Exception e) {
            Log.e("SeaCatService", "Reactor shutdown failed:", e);
        }

        SeaCatClient.setReactor(null);
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
