package com.teskalabs.seacat.android.companion.Profiles;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.teskalabs.seacat.android.companion.DiscoverConfStore;
import com.teskalabs.seacat.android.companion.Model.ModelProfile;
import com.teskalabs.seacat.android.companion.R;

public class ProfilesActivity extends ActionBarActivity {
    public static final String TAG = "ProfilesActivity";
    ProfilesCursorAdapter profilesCursorAdapter;
    ListView listViewProfiles;
    Button buttonAddProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profiles);

        ModelProfile modelProfile = new ModelProfile(getApplicationContext());
        profilesCursorAdapter = new ProfilesCursorAdapter(this, modelProfile.findAll());

        listViewProfiles = (ListView) findViewById(R.id.listViewProfiles);
        listViewProfiles.setAdapter(profilesCursorAdapter);

        buttonAddProfile = (Button) findViewById(R.id.buttonAddNewProfile);
        buttonAddProfile.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startEditProfileActivity(-1);
            }
        });
    }

    protected void onResume(Bundle savedInstanceSTate) {
        super.onResume();
        refreshProfilesList();
        // Refresh profiles list

    }

    protected void startEditProfileActivity(Integer id)
    {
        Intent intent = new Intent(ProfilesActivity.this, EditProfileActivity.class);
        intent.putExtra("ID", id);
        startActivityForResult(intent, 1);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            refreshProfilesList();
        }
    }

    protected void refreshProfilesList()
    {
        ModelProfile modelProfile = new ModelProfile(getApplicationContext());
        profilesCursorAdapter.swapCursor(modelProfile.findAll());
        profilesCursorAdapter.notifyDataSetChanged();;
    }

    public class ProfilesCursorAdapter extends CursorAdapter {
        private DiscoverConfStore confStore;
        public ProfilesCursorAdapter(Context mContext, Cursor cursor) {
            super(mContext, cursor, 0);
            confStore = new DiscoverConfStore(getApplicationContext());
            confStore.LoadFromPrefs();
        }
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.item_profile, parent, false);
        }
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final Integer id = cursor.getInt(cursor.getColumnIndex(ModelProfile.COL_ID));
            final String profileName = cursor.getString(cursor.getColumnIndex(ModelProfile.COL_PROFILE_NAME));
            final String gatewayName = cursor.getString(cursor.getColumnIndex(ModelProfile.COL_GATEWAY_NAME));;
            final String ip = cursor.getString(cursor.getColumnIndex(ModelProfile.COL_IP));;
            final String port = cursor.getString(cursor.getColumnIndex(ModelProfile.COL_PORT));;

            // Extract properties from cursor
            TextView textViewProfileName = (TextView) view.findViewById(R.id.textViewProfileName);
            TextView textViewGatewayName = (TextView) view.findViewById(R.id.textViewGatewayName);
            TextView textViewIp = (TextView) view.findViewById(R.id.textViewIp);
            ToggleButton buttonActive = (ToggleButton) view.findViewById(R.id.toggleButtonActive);
            if (ip.equals(confStore.getIp()) &&
                    port.equals(confStore.getPort()) &&
                    gatewayName.equals(confStore.getGatewayName()) &&
                    confStore.getGwEnabled())
                buttonActive.setChecked(true);
            else
                buttonActive.setChecked(false);

            textViewProfileName.setText(profileName);
            textViewGatewayName.setText(gatewayName);
            StringBuilder sb = new StringBuilder();
            sb.append(ip).append(":").append(port);
            textViewIp.setText(sb.toString());

            view.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    startEditProfileActivity(id);
                }
            });

            buttonActive.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ToggleButton that = (ToggleButton) v;
                    boolean isChecked = that.isChecked();
                    if (isChecked) {
                        confStore.setGatewayName(gatewayName);
                        confStore.setIp(ip);
                        confStore.setPort(port);
                        confStore.setGwEnabled(true);
                    } else {
                        confStore.setGwEnabled(false);
                    }
                    confStore.store();
                    confStore.storeToPrefs();

                    refreshProfilesList();
                }
            });

        }
    }
}
