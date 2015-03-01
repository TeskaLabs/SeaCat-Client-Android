package mobi.seacat.client;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

public class SeaCatService extends Service
{
    public SeaCatService()
    {
    }


	@Override
	synchronized public int onStartCommand(Intent intent, int flags, int startId)
	{
        if (!SeaCatClient.isConfigured())
        {
            try {
                SeaCatClient.configure(getApplicationContext());
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

		return START_STICKY;
	}


	@Override
	public void onDestroy()
	{
		super.onDestroy();

        //TODO: Terminate Seacat
	}


    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }
}
