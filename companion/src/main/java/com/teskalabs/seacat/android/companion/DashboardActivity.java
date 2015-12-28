package com.teskalabs.seacat.android.companion;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

public class DashboardActivity extends ActionBarActivity {

    ImageButton buttonProfiles, buttonDiagnostics, buttonHttpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        buttonProfiles = (ImageButton) findViewById(R.id.imageButtonProfiles);
        buttonDiagnostics = (ImageButton) findViewById(R.id.imageButtonDiagnostics);
        buttonHttpClient = (ImageButton) findViewById(R.id.imageButtonHttpClient);

        buttonProfiles.setOnClickListener(buttonProfilesOnClick);
        buttonDiagnostics.setOnClickListener(buttonDiagnosticsOnClick);
        buttonHttpClient.setOnClickListener(buttonHttpClientOnClick);
    }

    View.OnClickListener buttonProfilesOnClick = new View.OnClickListener() {
        public void onClick(View v) {
            startActivity(new Intent(DashboardActivity.this, LocalDiscoverActivity.class));
            return;
        }
    };
    View.OnClickListener buttonDiagnosticsOnClick = new View.OnClickListener() {
        public void onClick(View v) {
            //startActivity(new Intent(DashboardActivity.this, XXX.class));
            return;
        }
    };
    View.OnClickListener buttonHttpClientOnClick = new View.OnClickListener() {
        public void onClick(View v) {
            startActivity(new Intent(DashboardActivity.this, HttpClientActivity.class));
            return;
        }
    };

}
