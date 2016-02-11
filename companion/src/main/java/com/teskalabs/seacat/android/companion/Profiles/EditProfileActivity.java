package com.teskalabs.seacat.android.companion.Profiles;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.teskalabs.seacat.android.companion.DiscoverConfStore;
import com.teskalabs.seacat.android.companion.Model.ModelProfile;
import com.teskalabs.seacat.android.companion.R;

import java.util.regex.Pattern;

public class EditProfileActivity extends ActionBarActivity {
    public static final String TAG = "EditProfileActivity";

    private Integer profileId;
    public static final Integer ID_NEW = -1;

    public Button   buttonSave;
    public EditText editTextIP;
    public EditText editTextPort;
    public EditText editTextGatewayName;
    public EditText editTextProfileName;

    private DiscoverConfStore discoverConfStore;
    ModelProfile profile;

    protected String configFileName = "local-discover.conf";
    protected String configFilePath;

    private static final Pattern patternIP = Pattern.compile(
            "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
    private static final Pattern patternPort = Pattern.compile(
            "^\\d+$");
    private static final Pattern patternName = Pattern.compile(
            "^[0-9a-zA-Z\\-\\._]+$");


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
    protected String getValueProfileName() { return editTextProfileName.getText().toString(); }

    private boolean validateGatewayName(String appId) { return patternName.matcher(appId).matches(); }
    private boolean validateIP(String ip) { return patternIP.matcher(ip).matches(); }
    private boolean validatePort(String port) { return patternPort.matcher(port).matches(); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        buttonSave = (Button) findViewById(R.id.buttonSave);
        editTextIP = (EditText) findViewById(R.id.editTextIP);
        editTextPort = (EditText) findViewById(R.id.editTextPort);
        editTextGatewayName = (EditText) findViewById(R.id.editTextGatewayName);
        editTextProfileName = (EditText) findViewById(R.id.editTextProfileName);

        StateWatcher sWatcher = new StateWatcher();
        editTextIP.addTextChangedListener(sWatcher);
        editTextPort.addTextChangedListener(sWatcher);
        editTextGatewayName.addTextChangedListener(sWatcher);
        editTextProfileName.addTextChangedListener(sWatcher);

        Bundle extrasBundle = getIntent().getExtras();
        profileId = extrasBundle.getInt("ID", ID_NEW);
        profile = new ModelProfile(getApplicationContext());

        if (profileId != -1)
        {
            profile.findOneById(profileId);
            editTextIP.setText(profile.getIp());
            editTextPort.setText(profile.getPort());
            editTextGatewayName.setText(profile.getGatewayName());
            editTextProfileName.setText(profile.getProfileName());

            // Show remove profile button
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit_profile_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.remove_profile:
                removeProfile();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void removeProfile()
    {
        boolean success = profile.removeById(profileId) > 0;
        if (success) {
            Toast.makeText(getApplicationContext(), "Profile removed.", Toast.LENGTH_SHORT).show();
            finish();
        }
        else {
            Log.e(TAG, "Couldn't remove data from SQL.");
            Toast.makeText(getApplicationContext(), "Couldn't remove profile.", Toast.LENGTH_SHORT).show();;
        }
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
        // Store
        profile.setProfileName(getValueProfileName());
        profile.setGatewayName(getValueGatewayName());
        profile.setPort(getValuePort());
        profile.setIp(getValueIP());

        boolean success;
        if (profileId == -1) {
            success = profile.insert() != -1;
        }
        else {
            success = profile.update() > 0;
        }

        if (success) {
            Toast.makeText(getApplicationContext(), "Profile saved.", Toast.LENGTH_SHORT).show();
            finish();
        }
        else {
            Log.e(TAG, "Couldn't save data to SQL.");
            Toast.makeText(getApplicationContext(), "Couldn't save.", Toast.LENGTH_SHORT).show();;
        }
        return;
    }

    public void onButtonDeleteClick(View v)
    {
        ;
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
