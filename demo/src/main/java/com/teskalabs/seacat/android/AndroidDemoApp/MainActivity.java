package com.teskalabs.seacat.android.AndroidDemoApp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
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
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.EnumSet;
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
                sendPing();
            }

        }, 0, 1000);

        // Start get timer
        if (getTimer == null) getTimer = new Timer();
        getTimer.schedule(new TimerTask() {
            @Override
            public void run() {
            try
            {
                int what = counter % 4;
                counter += 1;

                GetTimerMethod_HTTPClient_GET();

                //if (what == 0) NoSeaCat_GET();
                //else if (what == 1) NoSeaCat_HC_GET();

                //if (what == 0) GetTimerMethod_GET();
                //else if (what == 1) GetTimerMethod_PUT_ContentLenght();
                //else if (what == 2) GetTimerMethod_HTTPClient_GET();
                //else if (what == 3) GetTimerMethod_HTTPClient_PUT_ContentLenght();
                //else if (what == 4) GetTimerMethod_HTTPClient_PUT_chunked();
            }

            catch (Exception e)
            {
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

    protected void GetTimerMethod_HTTPClient_GET() throws IOException
    {
        BasicClientCookie c = new BasicClientCookie("TestSet", "AHoj");
        c.setDomain("evalhost.seacat");
        c.setPath("/");
        HTTPClient_cookieStore.addCookie(c);

        DefaultHttpClient httpclient = (DefaultHttpClient) SeaCatClient.httpClient();
        httpclient.setCookieStore(HTTPClient_cookieStore);

        HttpGet httpget = new HttpGet(String.format("https://evalhost.seacat/fortune?%s", getPackageName()));

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

        AbstractHttpEntity requestEntity = new AbstractHttpEntity() {

            public boolean isRepeatable() {
                return false;
            }

            public long getContentLength() {
                return -1;
            }

            public boolean isStreaming() {
                return true;
            }

            public InputStream getContent() throws IOException {
                // Should be implemented as well but is irrelevant for this case
                throw new UnsupportedOperationException();
            }

            public void writeTo(final OutputStream outstream) throws IOException
            {
                for(int i=0; i<100; i++)
                {
                    byte[] buffer = new byte[16*1024];
                    for (int j=0; j<16*1024; j++) buffer[j] = 'x';
                    outstream.write(buffer);
                }
                outstream.close();
            }

        };

        Log.i("XXX", "1111");
        httpput.setEntity(requestEntity);
        Log.i("XXX", "2222");
        HttpResponse response = httpclient.execute(httpput);
        Log.i("XXX", "3333");

        HttpEntity entity = response.getEntity();

        int length = 0;
        InputStream is = entity.getContent();
        while (true)
        {
            byte[] buffer = new byte[1024];
            int ret = is.read(buffer);
            if (ret == -1) break;
            length += ret;
        }

        final String output = "GetTimerMethod_HTTPClient_PUT_chunked: "+length;

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

        else if (id == R.id.action_long_request) {
            try
            {
                GetTimerMethod_HTTPClient_PUT_chunked();
            }

            catch (Exception e)
            {
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
