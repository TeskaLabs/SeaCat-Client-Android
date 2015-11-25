package com.teskalabs.seacat.android.AndroidDemoApp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.teskalabs.seacat.android.client.SeaCatClient;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends ActionBarActivity
{

    private TextView resultTextView = null;
    private TextView pingTextView = null;
    private Timer pingTimer = null;
    private Timer getTimer = null;
    private int counter = 0;

    private org.apache.http.client.CookieStore HTTPClient_cookieStore;

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

        HTTPClient_cookieStore = new BasicCookieStore();

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
                    int what = counter % 2;
                    counter += 1;

                    //if (what == 0) NoSeaCat_GET();
                    //else if (what == 1) NoSeaCat_HC_GET();

                    if (what == 0) GetTimerMethod_GET();
                    else if (what == 1) GetTimerMethod_HTTPClient_GET();

                    //else if (what == 1) GetTimerMethod_PUT_ContentLenght();
                    //else if (what == 2) GetTimerMethod_HTTPClient_GET();
                    //else if (what == 3) GetTimerMethod_HTTPClient_PUT_chunked();
                    //else if (what == 4) GetTimerMethod_HTTPClient_PUT_ContentLenght();
                }

                catch (Exception e)
                {
                    final String output = e.toString();
                    e.printStackTrace();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            resultTextView.setText(output);
                        }
                    });
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
        URL url = new URL(String.format("https://service.seacat/?%s", getPackageName()));
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

    protected void GetTimerMethod_HTTPClient_GET() throws IOException
    {
        BasicClientCookie c = new BasicClientCookie("TestSet", "AHoj");
        c.setDomain("evalhost.seacat");
        c.setPath("/");
        HTTPClient_cookieStore.addCookie(c);

        DefaultHttpClient httpclient = (DefaultHttpClient) SeaCatClient.httpClient();
        httpclient.setCookieStore(HTTPClient_cookieStore);

        HttpGet httpget = new HttpGet(String.format("https://service.seacat/?%s", getPackageName()));

        HttpResponse response = httpclient.execute(httpget);

        HttpEntity entity = response.getEntity();
        final String output = "GetTimerMethod_HTTPClient_GET\r\n" + EntityUtils.toString(entity);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                resultTextView.setText(output);
            }
        });
    }


    protected void GetTimerMethod_HTTPClient_PUT_chunked() throws IOException
    {
        HttpClient httpclient = SeaCatClient.httpClient();
        HttpPut httpput = new HttpPut(String.format("https://evalhost.seacat/put?%s", getPackageName()));

        String data = "som3Data4nyFormat=seeThat??SeaCat";

        //HttpEntity reqEntity = new StringEntity(data);

        InputStream istream = new ByteArrayInputStream(data.getBytes());
        HttpEntity reqEntity = new InputStreamEntity(istream, -1);
        httpput.setEntity(reqEntity);

        HttpResponse response = httpclient.execute(httpput);

        HttpEntity entity = response.getEntity();
        final String output = EntityUtils.toString(entity);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                resultTextView.setText(output);
            }
        });

    }


    protected void GetTimerMethod_HTTPClient_PUT_ContentLenght() throws IOException
    {
        HttpClient httpclient = SeaCatClient.httpClient();
        HttpPut httpput = new HttpPut(String.format("https://evalhost.seacat/put?%s", getPackageName()));

        String data = "som3Data4nyFormat=seeThat??SeaCat";
        HttpEntity reqEntity = new StringEntity(data);
        httpput.setEntity(reqEntity);

        HttpResponse response = httpclient.execute(httpput);

        HttpEntity entity = response.getEntity();
        final String output = EntityUtils.toString(entity);

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



    private void NoSeaCat_HC_GET() throws IOException
    {

        DefaultHttpClient httpclient = new DefaultHttpClient();
        httpclient.setCookieStore(HTTPClient_cookieStore);

        HttpGet httpget = new HttpGet("http://eval.seacat.mobi/");

        HttpResponse response = httpclient.execute(httpget);

        HttpEntity entity = response.getEntity();
        final String output = new String(EntityUtils.toByteArray(entity));

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

        if (id == R.id.action_gw_disconnect) {
            try {
                SeaCatClient.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return super.onOptionsItemSelected(item);
    }
}
