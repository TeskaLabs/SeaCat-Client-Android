package com.teskalabs.seacat.android.companion;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Pattern;


public class MainActivity extends ActionBarActivity {
    public static final String TAG = "compainion.MainActivity";
    public ToggleButton buttonSave;
    public EditText editTextIP;
    public EditText editTextPort;
    public EditText editTextAppId;

    private SharedPreferences sharedPref;
    public static final String PREF_APPID   = "com.teskalabs.seacat.android.companion.PREF_APPID";
    public static final String PREF_IP      = "com.teskalabs.seacat.android.companion.PREF_IP";
    public static final String PREF_PORT    = "com.teskalabs.seacat.android.companion.PREF_PORT";

    public Integer STATE_DISABLED       = 0;
    public Integer STATE_ENABLED        = 1;

    protected String configFileName = "seacat-devel-discover.conf";
    protected String configFilePath;

    private static final Pattern patternIP = Pattern.compile(
            "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
    private static final Pattern patternPort = Pattern.compile(
            "^\\d+$");
    private static final Pattern patternAppId = Pattern.compile(
            "^[a-z1-9A-Z\\-]+(?:(?:\\.)[a-z1-9A-Z\\-]+)+$");


    class StateWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { }
        @Override
        public void afterTextChanged(Editable s) {updateStateLayout();}
    }


    protected String getValueIP() { return editTextIP.getText().toString(); }
    protected String getValuePort() { return editTextPort.getText().toString(); }
    protected String getValueAppId() { return editTextAppId.getText().toString(); }

    private boolean validateAppId(String appId) { return patternAppId.matcher(appId).matches(); }
    private boolean validateIP(String ip) { return patternIP.matcher(ip).matches(); }
    private boolean validatePort(String port) { return patternPort.matcher(port).matches(); }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        configFilePath = getFilesDir()+"/"+configFileName;

        buttonSave = (ToggleButton) findViewById(R.id.buttonSave);
        editTextIP = (EditText) findViewById(R.id.editTextIP);
        editTextPort = (EditText) findViewById(R.id.editTextPort);
        editTextAppId = (EditText) findViewById(R.id.editTextAppId);

        StateWatcher sWatcher = new StateWatcher();
        editTextIP.addTextChangedListener(sWatcher);
        editTextPort.addTextChangedListener(sWatcher);
        editTextAppId.addTextChangedListener(sWatcher);

    }

    @Override
    protected void onResume()
    {
        super.onResume();
        sharedPref = getPreferences(Context.MODE_PRIVATE);
        editTextIP.setText(sharedPref.getString(PREF_IP, ""));
        editTextPort.setText(sharedPref.getString(PREF_PORT, ""));
        editTextAppId.setText(sharedPref.getString(PREF_APPID, ""));
        updateStateLayout();
    }

    protected void updateStateLayout()
    {
        if (getConfigState() == STATE_ENABLED)
            buttonSave.setChecked(true);
        else
            buttonSave.setChecked(false);
    }

    public void buttonSaveOnClick(View v)
    {
        // Temporarily disable button
        buttonSave.setEnabled(false);

        if (!buttonSave.isChecked())
        {
            // Disabling Gateway = remove file
            File configFile = new File(configFilePath);
            if (configFile.exists())
                configFile.delete();
            Toast.makeText(getApplicationContext(), "Gateway disabled", Toast.LENGTH_SHORT)
                    .show();
            buttonSave.setEnabled(true);
            return;
        }

        // Get Input Fields Values
        String strAppId = getValueAppId();
        String strIP = getValueIP();
        String strPort = getValuePort();

        boolean isValid = true;
        // Validate App ID
        if (!validateAppId(strAppId))
        {
            isValid = false;
            editTextAppId.setError("must be a valid app Id.");
        }
        // Validate IP
        if (!validateIP(strIP))
        {
            isValid = false;
            editTextIP.setError("must be a valid IPv4 IP.");
        }
        // Validate port
        if (!validatePort(strPort))
        {
            isValid = false;
            editTextPort.setError("must be a valid port.");
        }

        if (!isValid)
        {
            buttonSave.setEnabled(true);
            buttonSave.setChecked(false);
            return;
        }

        // Write to seacat devel discover config file
        BufferedWriter out = null;
        try
        {
            FileWriter fstream = new FileWriter(configFilePath);
            out = new BufferedWriter(fstream);
            out.write("---\n");
            out.write(strAppId+"\n");
            out.write(strIP+"\n");
            out.write(strPort+"\n");
            out.close();
        }
        catch (IOException e)
        {
            Log.e(TAG, "Error: " + e.getMessage());
            buttonSave.setEnabled(true);
            buttonSave.setChecked(false);
            return;
        }

        // Store to preferences
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(PREF_APPID, strAppId);
        editor.putString(PREF_IP, strIP);
        editor.putString(PREF_PORT, strPort);
        editor.commit();

        // Display message
        Toast.makeText(getApplicationContext(),
                "Gateway enabled."+strPort,
                Toast.LENGTH_SHORT)
                    .show();

        buttonSave.setEnabled(true);
        return;
    }

    public int getConfigState()
    {
        String prefAppId = sharedPref.getString(PREF_APPID, "");
        String prefIP    = sharedPref.getString(PREF_IP, "");
        String prefPort  = sharedPref.getString(PREF_PORT, "");
        File fileConfig = new File(configFilePath);

        if (!fileConfig.exists())
            return STATE_DISABLED;

        if (!prefAppId.equals(getValueAppId())
                || !prefIP.equals(getValueIP())
                || !prefPort.equals(getValuePort()))
            return STATE_DISABLED;

        return STATE_ENABLED;
    }
}
