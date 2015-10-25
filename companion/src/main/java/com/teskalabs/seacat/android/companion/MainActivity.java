package com.teskalabs.seacat.android.companion;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.regex.Pattern;


public class MainActivity extends ActionBarActivity {
    public Button buttonSave;
    public EditText editTextIP;
    public EditText editTextPort;

    private static final Pattern patternIP = Pattern.compile(
            "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
    private static final Pattern patternPort = Pattern.compile(
            "^\\d+$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonSave = (Button) findViewById(R.id.buttonSave);
        editTextIP = (EditText) findViewById(R.id.editTextIP);
        editTextPort = (EditText) findViewById(R.id.editTextPort);
    }

    public void buttonSaveOnClick(View v)
    {
        // Get Input Fields Values
        String strIP = editTextIP.getText().toString();
        String strPort = editTextPort.getText().toString();


        boolean isValid = true;
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
            return;

        Toast.makeText(getApplicationContext(),
                "IP: "+strIP+"; Port: "+strPort,
                Toast.LENGTH_LONG)
                    .show();
    }

    private boolean validateIP(String ip)
    {
        return patternIP.matcher(ip).matches();
    }

    private boolean validatePort(String port)
    {
        return patternPort.matcher(port).matches();
    }
}
