package mobi.seacat.AndroidDemoApp;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import mobi.seacat.client.SeaCatClient;

public class MainActivity extends ActionBarActivity
{

    private TextView resultTextView = null;
    private TextView pingTextView = null;
    private Timer pingTimer = null;
    private Timer getTimer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        resultTextView = (TextView) findViewById(R.id.resultTextView);
        pingTextView = (TextView) findViewById(R.id.pingTextView);
    }


    @Override
    protected void onStart()
    {
        super.onStart();

        // Start ping timer
        if (pingTimer == null) pingTimer = new Timer();
        pingTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                PingTimerMethod();
            }

        }, 0, 1000);

        // Start get timer
        if (getTimer == null) getTimer = new Timer();
        getTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try
                {
                    GetTimerMethod_GET();
                }

                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }

        }, 0, 1000);

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


    private void PingTimerMethod()
    {
        try {
            SeaCatClient.ping(new mobi.seacat.client.ping.Ping() {
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
        URL url = new URL(String.format("https://evalhost.seacat/?%s", getPackageName()));
        HttpURLConnection conn = SeaCatClient.open(url);

        InputStream is = conn.getInputStream();
        assert(is != null);


        String line;
        String result = "";
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        while ((line = rd.readLine()) != null)
        {
            result += line;
        }
        rd.close();
        is.close();

        final String output = result;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                resultTextView.setText(output);
            }
        });

    }


    private void GetTimerMethod_PUT() throws IOException
    {
        URL url = new URL(String.format("https://evalhost.seacat/put?%s", getPackageName()));
        HttpURLConnection conn = SeaCatClient.open(url);

        conn.setRequestMethod("PUT");
        conn.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
        conn.setDoOutput(true);

        DataOutputStream outputStream = new DataOutputStream(conn.getOutputStream());
        String data = "som3Data4nyFormat=seeThat??SeaCat";
        outputStream.writeBytes(data);
        outputStream.flush();
        outputStream.close();

        InputStream is = conn.getInputStream();
        assert(is != null);


        String line;
        String result = "";
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        while ((line = rd.readLine()) != null)
        {
            result += line;
        }
        rd.close();
        is.close();

        final String output = result;

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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
