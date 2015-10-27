package com.teskalabs.seacat.android.companion;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import java.util.regex.Pattern;


public class MainActivity extends ActionBarActivity {
    public static final String TAG = "compainion.MainActivity";

    public Button   buttonSave;
    public EditText editTextIP;
    public EditText editTextPort;
    public EditText editTextGatewayName;
    public CheckBox checkBoxEnabled;

    private DiscoverConfStore discoverConfStore;

    protected String configFileName = "local-discover.conf";
    protected String configFilePath;

    private static final Pattern patternIP = Pattern.compile(
            "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
    private static final Pattern patternPort = Pattern.compile(
            "^\\d+$");
    private static final Pattern patternName = Pattern.compile(
            "^[a-z1-9A-Z\\-\\._]+$");


    class StateWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { }
        @Override
        public void afterTextChanged(Editable s) { buttonSave.setEnabled(true);}
    }


    protected String getValueIP() { return editTextIP.getText().toString(); }
    protected String getValuePort() { return editTextPort.getText().toString(); }
    protected String getValueGatewayName() { return editTextGatewayName.getText().toString(); }

    private boolean validateGatewayName(String appId) { return patternName.matcher(appId).matches(); }
    private boolean validateIP(String ip) { return patternIP.matcher(ip).matches(); }
    private boolean validatePort(String port) { return patternPort.matcher(port).matches(); }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        configFilePath = getFilesDir()+"/"+configFileName;

        buttonSave = (Button) findViewById(R.id.buttonSave);
        checkBoxEnabled = (CheckBox) findViewById(R.id.checkBoxEnabled);
        editTextIP = (EditText) findViewById(R.id.editTextIP);
        editTextPort = (EditText) findViewById(R.id.editTextPort);
        editTextGatewayName = (EditText) findViewById(R.id.editTextGatewayName);

        StateWatcher sWatcher = new StateWatcher();
        editTextIP.addTextChangedListener(sWatcher);
        editTextPort.addTextChangedListener(sWatcher);
        editTextGatewayName.addTextChangedListener(sWatcher);
        checkBoxEnabled.addTextChangedListener(sWatcher);

        discoverConfStore = new DiscoverConfStore(getApplicationContext());

    }

    @Override
    protected void onResume()
    {
        super.onResume();
        discoverConfStore.LoadFromPrefs(this);
        editTextIP.setText(discoverConfStore.getIp());
        editTextPort.setText(discoverConfStore.getPort());
        editTextGatewayName.setText(discoverConfStore.getGatewayName());
        checkBoxEnabled.setChecked(discoverConfStore.getGwEnabled());

        buttonSave.setEnabled(false);
    }

    public void checkBoxEnabledOnClick(View v)
    {
        buttonSave.setEnabled(true);
    }

    public void buttonSaveOnClick(View v)
    {
        if (!validateForm())
        {
            return;
        }
        boolean previouslyEnabled = discoverConfStore.getGwEnabled();
        discoverConfStore.setGatewayName(getValueGatewayName());
        discoverConfStore.setIp(getValueIP());
        discoverConfStore.setPort(getValuePort());
        discoverConfStore.setGwEnabled(checkBoxEnabled.isChecked());

        // Store
        discoverConfStore.store(this);
        discoverConfStore.storeToPrefs(this);

        if (discoverConfStore.getGwEnabled())
            Toast.makeText(getApplicationContext(), "Gateway enabled", Toast.LENGTH_SHORT)
                    .show();
        else if (!discoverConfStore.getGwEnabled() && previouslyEnabled)
            Toast.makeText(getApplicationContext(), "Gateway disabled", Toast.LENGTH_SHORT)
                    .show();

        buttonSave.setEnabled(false);
        return;
    }

    private boolean validateForm()
    {
        boolean isValid = true;
        // Validate App ID
        if (!validateGatewayName(getValueGatewayName()))
        {
            isValid = false;
            editTextGatewayName.setError("must be a valid gateway name.");
        }
        // Validate IP
        if (!validateIP(getValueIP()))
        {
            isValid = false;
            editTextIP.setError("must be a valid IPv4 IP.");
        }
        // Validate port
        if (!validatePort(getValuePort()))
        {
            isValid = false;
            editTextPort.setError("must be a valid port.");
        }
        return isValid;
    }

}
