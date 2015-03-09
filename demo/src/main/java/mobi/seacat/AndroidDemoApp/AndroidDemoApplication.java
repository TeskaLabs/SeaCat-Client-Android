package mobi.seacat.AndroidDemoApp;

import android.app.Application;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

import mobi.seacat.client.SeaCatClient;
import mobi.seacat.client.ui.SeaCatSplashActivity;

public class AndroidDemoApplication extends Application
{

    @Override
    public void onCreate()
    {
        super.onCreate();

        // Enable SeaCat
        try
        {
            SeaCatClient.configure(getApplicationContext());
        }
        catch (IOException e)
        {
            Toast.makeText(getApplicationContext(), "SeaCat failed to configure.", Toast.LENGTH_LONG).show();
            e.printStackTrace();
            System.exit(1);
        }
    }

}
