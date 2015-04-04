package mobi.seacat.AndroidDemoApp;

import android.app.Application;
import android.content.Intent;

import mobi.seacat.client.SeaCatService;

public class DemoApp extends Application
{

    @Override
    public void onCreate()
    {
        super.onCreate();

        // Enable SeaCat
        startService(new Intent(getBaseContext(), SeaCatService.class));
    }

}
