package com.teskalabs.seacat.android.companion.Profiles;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.teskalabs.seacat.android.companion.DashboardActivity;
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
        public ProfilesCursorAdapter(Context mContext, Cursor cursor) {
            super(mContext, cursor, 0);
        }
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.item_profile, parent, false);
        }
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final Integer id = cursor.getInt(cursor.getColumnIndex(ModelProfile.COL_ID));
            Log.i(TAG, "bindView id: " + id.toString());
            String profileName = cursor.getString(cursor.getColumnIndex(ModelProfile.COL_PROFILE_NAME));
            String gatewayName = cursor.getString(cursor.getColumnIndex(ModelProfile.COL_GATEWAY_NAME));;
            String ip = cursor.getString(cursor.getColumnIndex(ModelProfile.COL_IP));;
            String port = cursor.getString(cursor.getColumnIndex(ModelProfile.COL_PORT));;

            // Extract properties from cursor
            TextView textViewProfileName = (TextView) view.findViewById(R.id.textViewProfileName);
            TextView textViewGatewayName = (TextView) view.findViewById(R.id.textViewGatewayName);
            TextView textViewIp = (TextView) view.findViewById(R.id.textViewIp);

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

            ;
        }
    }
}
