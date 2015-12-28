    package com.teskalabs.seacat.android.companion;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;

public class SplashActivity extends ActionBarActivity
{
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        handler = new Handler();
    }

    @Override
    protected void onStart()
    {

        super.onStart();

        Runnable splashCloser = new Runnable()
        {
            @Override
            public void run()
            {
                startActivity(new Intent(SplashActivity.this, HttpClientActivity.class));
                finish();
                return;
            }
        };

        handler.postDelayed(splashCloser, 1000);
    }
}
