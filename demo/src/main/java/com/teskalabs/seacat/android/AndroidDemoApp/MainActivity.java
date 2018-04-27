package com.teskalabs.seacat.android.AndroidDemoApp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.teskalabs.seacat.android.client.SeaCatClient;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.EnumSet;
import java.util.Timer;
import java.util.TimerTask;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends ActionBarActivity
{
    private TextView resultTextView = null;
    private TextView pingTextView = null;
    private TextView statusTextView = null;

    private Timer pingTimer = null;
    private Timer getTimer = null;
    private int counter = 0;

    private BroadcastReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        resultTextView = (TextView) findViewById(R.id.resultTextView);
        pingTextView = (TextView) findViewById(R.id.pingTextView);
        statusTextView = (TextView) findViewById(R.id.statusTextView);
    }

    @Override
    protected void onStart() {
		super.onStart();

		// Start ping timer
		if (pingTimer == null) pingTimer = new Timer();
		pingTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				sendPing();
			}

		}, 0, 1000);

		// Start get timer
		if (getTimer == null) getTimer = new Timer();
		getTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					int what = counter % 4;
					counter += 1;

					//if (what == 0) NoSeaCat_GET();
					//else if (what == 1) NoSeaCat_HC_GET();

					//if (what == 0) GetTimerMethod_GET();
					//else if (what == 1) GetTimerMethod_PUT_ContentLenght();
					//else if (what == 2) GetTimerMethod_HTTPClient_GET();
					//else if (what == 3) GetTimerMethod_HTTPClient_PUT_ContentLenght();
					//else if (what == 4) GetTimerMethod_HTTPClient_PUT_chunked();
				} catch (Exception e) {
					final String output = e.toString();
					Log.e("XXX", "Exception: ", e);

					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							resultTextView.setText(output);
						}
					});
				}
			}
		}, 0, 1000);


		receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (intent.hasCategory(SeaCatClient.CATEGORY_SEACAT)) {
					String action = intent.getAction();
					if (action.equals(SeaCatClient.ACTION_SEACAT_STATE_CHANGED)) {
						statusTextView.setText(intent.getStringExtra(SeaCatClient.EXTRA_STATE));
						return;
					}
				}
			}
		};

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(SeaCatClient.ACTION_SEACAT_STATE_CHANGED);
		intentFilter.addCategory(SeaCatClient.CATEGORY_SEACAT);
		registerReceiver(receiver, intentFilter);

		try {
			SecretKeySpec skeySpec = SeaCatClient.deriveKey("aes-key-1", "AES", 32);
			Cipher cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
			cipher.doFinal("To be encrypted.".getBytes("UTF8"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


    @Override
    protected void onStop()
    {
        super.onStop();

        getTimer.cancel();
        getTimer = null;

        pingTimer.cancel();
        pingTimer = null;
    }


    private void sendPing()
    {
        try {
            SeaCatClient.ping(new com.teskalabs.seacat.android.client.ping.Ping() {
                @Override
                public void pong()
                {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pingTextView.setText("Ping received: "+pingId);
                        }
                    });
                }

                @Override
                public void cancel()
                {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pingTextView.setText("Ping failed :-(");
                        }
                    });
                }

            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void GetTimerMethod_GET() throws IOException
    {
        URL url = new URL(String.format("https://evalhost.seacat/fortune"));
        HttpURLConnection conn = SeaCatClient.open(url);

        InputStream is = conn.getInputStream();
        assert(is != null);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();

        final String output = "GetTimerMethod_GET\r\n" + new String(buffer.toByteArray());

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                resultTextView.setText(output);
            }
        });

    }


    private void GetTimerMethod_PUT_ContentLenght() throws IOException
    {
        URL url = new URL(String.format("https://evalhost.seacat/put?%s", getPackageName()));
        HttpURLConnection conn = SeaCatClient.open(url);

        conn.setRequestMethod("PUT");
        conn.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
        conn.setDoOutput(true);

        DataOutputStream outputStream = new DataOutputStream(conn.getOutputStream());
        String outdata = "som3Data4nyFormat=seeThat??SeaCat";
        outputStream.writeBytes(outdata);
        outputStream.flush();
        outputStream.close();

        InputStream is = conn.getInputStream();
        assert(is != null);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();

        final String output = new String(buffer.toByteArray());

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                resultTextView.setText(output);
            }
        });
    }


    private void NoSeaCat_GET() throws IOException
    {
        URL url = new URL("http://eval.seacat.mobi/");
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();

        InputStream is = conn.getInputStream();
        assert(is != null);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();

        final String output = new String(buffer.toByteArray());

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                resultTextView.setText(output);
            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_reset_identity) {
            try {
                SeaCatClient.reset();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Toast.makeText(getApplicationContext(), "Identity reset!", Toast.LENGTH_LONG).show();
            startActivity(new Intent(MainActivity.this, SplashActivity.class));
            finish();
            return true;
        }

        else if (id == R.id.action_renew_certificate) {
            try {
                SeaCatClient.renew();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Toast.makeText(getApplicationContext(), "Certificate renewal process initialised!", Toast.LENGTH_LONG).show();
            return true;
        }

        else if (id == R.id.action_gw_disconnect) {
            try {
                SeaCatClient.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        else if (id == R.id.action_enable_debug_messages) {
            try {
                SeaCatClient.setLogMask(EnumSet.of(SeaCatClient.LogFlag.DEBUG_GENERIC));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        else if (id == R.id.action_disable_debug_messages) {
            try {
                SeaCatClient.setLogMask(SeaCatClient.LogFlag.NONE_SET);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return super.onOptionsItemSelected(item);
    }
}
