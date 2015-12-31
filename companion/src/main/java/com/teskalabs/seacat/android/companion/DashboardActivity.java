package com.teskalabs.seacat.android.companion;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.widget.ImageButton;

import com.teskalabs.seacat.android.companion.Base.BaseActivity;

public class DashboardActivity extends BaseActivity {

    ImageButton buttonProfiles, buttonDiagnostics, buttonHttpClient;

    protected SharedPreferences sharedPref;
    private String PREF_LEARNED_DRAW="_PREF_LEARNED_DRAW";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contentStub.setLayoutResource(R.layout.activity_dashboard);
        contentStub.inflate();
        // Inform base activity that this is the dashboard
        isHome = true;

        buttonProfiles = (ImageButton) findViewById(R.id.imageButtonProfiles);
        buttonDiagnostics = (ImageButton) findViewById(R.id.imageButtonDiagnostics);
        buttonHttpClient = (ImageButton) findViewById(R.id.imageButtonHttpClient);

        buttonProfiles.setOnClickListener(buttonProfilesOnClick);
        buttonDiagnostics.setOnClickListener(buttonDiagnosticsOnClick);
        buttonHttpClient.setOnClickListener(buttonHttpClientOnClick);

        sharedPref = getPreferences(Context.MODE_PRIVATE);
        if (!sharedPref.getBoolean(PREF_LEARNED_DRAW, false))
            mDrawerLayout.openDrawer(mDrawerList);
        mDrawerLayout.setDrawerListener(new BaseDrawerListener());

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

    private class BaseDrawerListener implements DrawerLayout.DrawerListener {
        @Override
        public void onDrawerOpened(View drawerView) {
            SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(PREF_LEARNED_DRAW, true);
            editor.commit();
        }
        @Override
        public void onDrawerClosed(View drawerView) {
        }
        @Override
        public void onDrawerSlide(View drawerView, float slideOffset) {
        }
        @Override
        public void onDrawerStateChanged(int newState) {
        }
    }

}
