package com.teskalabs.seacat.android.companion;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class DiscoverConfStore
{
    public static final String SHARED_PREF = "com.teskalabs.seacat.android.companion.SHARED_PREF";
    public static final String PREF_GWNAME   = "com.teskalabs.seacat.android.companion.PREF_GWNAME";
    public static final String PREF_IP      = "com.teskalabs.seacat.android.companion.PREF_IP";
    public static final String PREF_PORT    = "com.teskalabs.seacat.android.companion.PREF_PORT";
    public static final String PREF_GWENABLED = "com.teskalabs.seacat.android.companion.PREF_ENABLED";

    private String gatewayName;
    private String ip;
    private String port;
    private Boolean gwEnabled;

    protected final String TAG = "DiscoverConfStore";
    protected String configFileName = "local-discover.conf";
    protected String configFilePath;
    private Context context;

    public DiscoverConfStore(Context context)
    {
        this.configFilePath = context.getFilesDir().toString()+"/"+configFileName;
        this.context = context;
    }

    public String getGatewayName() { return this.gatewayName; }
    public String getIp() { return this.ip; }
    public String getPort() { return this.port; }
    public Boolean getGwEnabled() { return this.gwEnabled; }

    public void setGatewayName(String gatewayName) { this.gatewayName = gatewayName; }
    public void setIp(String ip) { this.ip = ip; }
    public void setPort(String port) { this.port = port; }
    public void setGwEnabled(boolean value) { this.gwEnabled = value; }

    public void store()
    {
        if (!gwEnabled.booleanValue())
        {
            File configFile = new File(configFilePath);
            if (configFile.exists())
                configFile.delete();
            return;
        }

        try {
            FileOutputStream fos = context.openFileOutput(configFileName, Context.MODE_WORLD_READABLE);

            StringBuilder sb = new StringBuilder();
            sb.append(gatewayName).append("\n");
            sb.append(ip).append("\n");
            sb.append(port).append("\n");

            fos.write(sb.toString().getBytes());
            fos.close();
        }
        catch (IOException e)
        {
            Log.e(TAG, "Error: " + e.getMessage());
        }
    }

    public void LoadFromPrefs()
    {
        SharedPreferences sharedPref = context.getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE);
        gatewayName = sharedPref.getString(PREF_GWNAME, "");
        ip          = sharedPref.getString(PREF_IP, "");
        port        = sharedPref.getString(PREF_PORT, "");
        gwEnabled   = sharedPref.getBoolean(PREF_GWENABLED, false);
    }

    public void storeToPrefs()
    {
        SharedPreferences sharedPref = context.getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(PREF_GWNAME, this.gatewayName);
        editor.putString(PREF_IP, this.ip);
        editor.putString(PREF_PORT, this.port);
        editor.putBoolean(PREF_GWENABLED, this.gwEnabled);
        editor.commit();
    }

}
