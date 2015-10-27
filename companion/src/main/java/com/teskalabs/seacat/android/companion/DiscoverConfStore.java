package com.teskalabs.seacat.android.companion;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;

public class DiscoverConfStore
{
    public static final String PREF_GWNAME   = "com.teskalabs.seacat.android.companion.PREF_GWNAME";
    public static final String PREF_IP      = "com.teskalabs.seacat.android.companion.PREF_IP";
    public static final String PREF_PORT    = "com.teskalabs.seacat.android.companion.PREF_PORT";

    private Context context;
    private String gatewayName;
    private String ip;
    private String port;

    protected final String TAG = "DiscoverConfStore";
    protected String configFileName = "seacat-devel-discover.conf";
    protected String configFilePath;

    public DiscoverConfStore(Context context)
    {
        this.context = context;
        this.configFilePath = context.getFilesDir().toString()+"/"+configFileName;
    }

    public void Store()
    {
        try {
            FileOutputStream fos = context.openFileOutput(configFilePath, Context.MODE_WORLD_READABLE);

            StringBuilder sb = new StringBuilder();
            sb.append("---").append("\n");
            sb.append(gatewayName).append("\n");
            sb.append(ip).append("\n");;
            sb.append(port).append("\n");;
            sb.append("---").append("\n");;

            fos.write(sb.toString().getBytes());
            fos.close();
        }
        catch (IOException e)
        {
            Log.e(TAG, "Error: " + e.getMessage());
        }
    }

    public void LoadFromPrefs(SharedPreferences sharedPref)
    {
        gatewayName = sharedPref.getString(PREF_GWNAME, "");
        ip          = sharedPref.getString(PREF_IP, "");
        port        = sharedPref.getString(PREF_PORT, "");
    }

    public void StoreToPrefs(SharedPreferences sharedPref)
    {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(PREF_GWNAME, this.gatewayName);
        editor.putString(PREF_IP, this.ip);
        editor.putString(PREF_PORT, this.port);
        editor.commit();
    }

}
