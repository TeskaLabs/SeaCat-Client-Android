package mobi.seacat.AndroidDemoApp;

import android.app.Application;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

import mobi.seacat.client.SeaCatClient;
import mobi.seacat.client.intf.IDelegate;
import mobi.seacat.client.ui.SeaCatSplashActivity;

public class AndroidDemoApplication extends Application implements IDelegate
{

    @Override
    public void onCreate()
    {
        super.onCreate();

        // Enable SeaCat
        try
        {
            SeaCatClient.configure(getApplicationContext(), this);
        }
        catch (IOException e)
        {
            Toast.makeText(getApplicationContext(), "SeaCat failed to configure.", Toast.LENGTH_LONG).show();
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void pong(int pingId)
    {
        //Intent i = new Intent(this, SeaCatSplashActivity.class);
        //i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //startActivity(i);

        Log.i("AndroidDemoApplication", "Pong: "+pingId);
    }

}
