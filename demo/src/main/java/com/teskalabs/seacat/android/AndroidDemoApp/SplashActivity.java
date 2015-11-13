package com.teskalabs.seacat.android.AndroidDemoApp;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

import com.teskalabs.seacat.android.client.SeaCatClient;

public class SplashActivity extends ActionBarActivity
{
    private BroadcastReceiver receiver;
    private Handler handler;
    private Runnable stateChecker;

    private TextView statusTextView = null;
    private boolean readyToClose;
    long displayTime;

    CSRDialog csrDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        statusTextView = (TextView) findViewById(R.id.statusTextView);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.hasCategory(SeaCatClient.CATEGORY_SEACAT))
                {
                    String action = intent.getAction();
                    if (action.equals(SeaCatClient.ACTION_SEACAT_STATE_CHANGED)) {
                        SplashActivity.this.onStateChanged(intent.getStringExtra(SeaCatClient.EXTRA_STATE));
                        return;
                    }

                    if (action.equals(SeaCatClient.ACTION_SEACAT_CSR_NEEDED)) {
                        SplashActivity.this.onCSRNeeded();
                        return;
                    }

                }

                Log.w(SplashActivity.class.getCanonicalName(), "Unexpected intent: "+intent);
            }
        };

        handler = new Handler();
        readyToClose = false;

        stateChecker = new Runnable()
        {
            @Override
            public void run()
            {
                if (readyToClose)
                {
                    if ((System.currentTimeMillis() - displayTime) > 3000)
                    {
                        startActivity(new Intent(SplashActivity.this, MainActivity.class));
                        finish();
                        return;
                    }
                } else {
                    SeaCatClient.broadcastState(); // This triggers initial delivery of the actual state
                }
                handler.postDelayed(this, 500);
            }
        };

    }

    private void onCSRNeeded()
    {
        // Ensure that CSR dialog is displayed only once
        if (csrDialog != null) return;

        csrDialog = new CSRDialog(SplashActivity.this) {
            @Override
            protected void onStop()
            {
                super.onStop();
                SplashActivity.this.csrDialog = null;
            }
        };
        csrDialog.show();
    }


    @Override
    protected void onStart()
    {
        super.onStart();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SeaCatClient.ACTION_SEACAT_STATE_CHANGED);
        intentFilter.addAction(SeaCatClient.ACTION_SEACAT_CSR_NEEDED);
        intentFilter.addCategory(SeaCatClient.CATEGORY_SEACAT);
        registerReceiver(receiver, intentFilter);

        displayTime = System.currentTimeMillis();

        stateChecker.run();

    }

    @Override
    protected void onStop()
    {
        super.onStop();

        handler.removeCallbacks(stateChecker);
        unregisterReceiver(receiver);
    }


    private void onStateChanged(String state)
    {
        statusTextView.setText(state);
        if ((state.charAt(3) == 'Y') && (state.charAt(4) == 'N') && (state.charAt(0) != 'f'))
        {
            readyToClose = true;
        }

        if ((state.charAt(1) == 'C') && (csrDialog == null))
        {
            onCSRNeeded();
        }

        else if ((state.charAt(1) != 'C') && (csrDialog != null))
        {
            csrDialog.dismiss();
        }

    }

    // Menu

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_splash, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        if (id == R.id.seacat_reset)
        {
            onResetSelected();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void onResetSelected()
    {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        try {
                            SeaCatClient.reset();
                            Toast.makeText(getApplicationContext(), "Identity reset initiated.", Toast.LENGTH_LONG).show();
                        } catch (IOException e) {
                            Log.e(this.getClass().getCanonicalName(), " SeaCatClient.reset:", e);
                        }
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder
            .setMessage("Are you sure?")
            .setPositiveButton("Yes", dialogClickListener)
            .setNegativeButton("No", dialogClickListener)
            .show();
    }

}
