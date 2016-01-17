package com.teskalabs.seacat.android.client;

import android.content.Context;
import android.content.Intent;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import com.teskalabs.seacat.android.client.core.Reactor;
import com.teskalabs.seacat.android.client.hc.SeaCatHttpClient;
import com.teskalabs.seacat.android.client.http.URLConnection;
import com.teskalabs.seacat.android.client.ping.Ping;
import com.teskalabs.seacat.android.client.util.RC;
import com.teskalabs.seacat.android.client.core.seacatcc;

import org.apache.http.client.HttpClient;
import org.apache.http.params.HttpParams;


/**
 * This class represents the main interface of TeskaLabs SeaCat client for Android (aka Android SeaCat SDK).<br>
 * It consists exclusively of static methods that provide SeaCat functions.
 *
 * <p>SeaCat client needs to be initialised in <tt>Application</tt> class during <tt>onCreate()</tt> method
 * by calling <tt>SeaCatClient.initialize()</tt>.<br>
 * Here is the example of initialization:
 * <pre>
 * {@code
 * public class MyApplicaton extends Application {
 *      public void onCreate() {
 *          super.onCreate();
 *          SeaCatClient.initialize(getApplicationContext());
 *     }
 * }
 * }
 * </pre>
 * </p>
 *
 * <p>SeaCat client interacts with the application by <tt>Intent</tt>s sent via Android <tt>broadcastIntent</tt> feature.<br>
 * To register your <tt>Activity</tt> to receive these Intents, use following code:
 * <pre>
 * {@code
 * public class MyActivity extends Activity {
 *
 *      private BroadcastReceiver receiver;
 *
 *      protected void onCreate(Bundle savedInstanceState) {
 *          super.onCreate(savedInstanceState);
 *
 *          receiver = new BroadcastReceiver() {
 *              public void onReceive(Context context, Intent intent) {
 *                  if (intent.hasCategory(SeaCatClient.CATEGORY_SEACAT)) {
 *                      String action = intent.getAction();
 *                      if (action.equals(SeaCatClient.ACTION_SEACAT_STATE_CHANGED)) {
 *                          // Your code goes here ...
 *                          ...
 *                      }
 *                  }
 *             }
 *          }
 *      }
 *
 *      protected void onStart() {
 *          super.onStart();

 *          IntentFilter intentFilter = new IntentFilter();
 *          intentFilter.addCategory(SeaCatClient.CATEGORY_SEACAT);
 *          registerReceiver(receiver, intentFilter);
 *      }
 *
 *      protected void onStop() {
 *          super.onStop();
 *
 *          unregisterReceiver(receiver);
 *      }
 *
 * }
 * }
 * </pre>
 * </p>
 */
public final class SeaCatClient
{
    static Reactor reactor = null;

    /**
     * The <tt>Intent</tt> category for all Intents sent by SeaCat client.
     *
     * <p>
     * Use it as a category filter in your IntentFilter:
     * <pre>
     * {@code
     * intentFilter.addCategory(SeaCatClient.CATEGORY_SEACAT);
     * }
     * </pre>
     * </p>
     */
    public final static String CATEGORY_SEACAT = "mobi.seacat.client.intent.category.SEACAT";

    /**
     * The <tt>Intent</tt> action used to inform that client is up and running.
     */
    public final static String ACTION_SEACAT_EVLOOP_STARTED = "mobi.seacat.client.intent.action.EVLOOP_STARTED";

    /**
     * The <tt>Intent</tt> action used to inform that client is successfully connected to the gateway.
     */
    public final static String ACTION_SEACAT_GWCONN_CONNECTED = "mobi.seacat.client.intent.action.GWCONN_CONNECTED";

    /**
     * The <tt>Intent</tt> action used to inform that client disconnected from the gateway.
     */
    public final static String ACTION_SEACAT_GWCONN_RESET = "mobi.seacat.client.intent.action.GWCONN_RESET";

    /**
     * The <tt>Intent</tt> action used to inform that client needs to produce CSR.
     */
    public final static String ACTION_SEACAT_CSR_NEEDED = "mobi.seacat.client.intent.action.CSR_NEEDED";

    /**
     * The <tt>Intent</tt> action used to inform that client state changed.
     * Detailed information about state change is in <tt>EXTRA_STATE</tt>.
     */
    public final static String ACTION_SEACAT_STATE_CHANGED = "mobi.seacat.client.intent.action.STATE_CHANGED";

    /**
     * The key to <tt>Intent</tt> extras with information about client state.<br>
     * Used in <tt>ACTION_SEACAT_STATE_CHANGED</tt> Intents.<br>
     *
     * State string is the same as from <tt>getState()</tt> method of this class.
     *
     * <p>
     * Example:
     * <pre>
     * {@code
     * String state = intent.getStringExtra(SeaCatClient.EXTRA_STATE);
     * }
     * </pre>
     *
     * </p>
     */
    public final static String EXTRA_STATE = "SEACAT_STATE";

    ///

    /**
     * Initialize SeaCat Android client.<br/>
     * SeaCat client needs to be initialized prior any other function is called.<br/>
     * Please refer to example above.
     *
     * @param context Android <tt>Context</tt> in which service is started.
     */
    public static void initialize(Context context)
    {
        SeaCatClient.initialize(context, CSR.createDefault(), null);
    }

    public static void initialize(Context context, String applicationIdSuffix)
    {
        SeaCatClient.initialize(context, CSR.createDefault(), applicationIdSuffix);
    }

    public static void initialize(Context context, Runnable CSRworker)
    {
        SeaCatClient.initialize(context, CSRworker, null);
    }

    public static void initialize(Context context, Runnable CSRworker, String applicationIdSuffix)
    {
        SeaCatInternals.applicationIdSuffix = applicationIdSuffix;
        setCSRWorker(CSRworker);
        context.startService(new Intent(context, SeaCatService.class));
    }


    /**
     * Triggers sending of an <tt>ACTION_SEACAT_STATE_CHANGED</tt> <tt>Intent</tt> even if the state has not changed.
     *
     * <p>Safe to use, it helps to keep your UI code nice.</p>
     */
    public static void broadcastState()
    {
        Reactor reactor = getReactor();
        if (reactor != null) reactor.broadcastState(getState());
    }

    ///

    synchronized static void setReactor(Reactor reactor)
    {
        SeaCatClient.reactor = reactor;
    }

    synchronized static Reactor getReactor()
    {
        return SeaCatClient.reactor;
    }


    /**
     * Pings SeaCat gateway.
     *
     * <p>This function can be used to keep the connection to SeaCat gateway open.</p>
     *
     * <p>
     * Example:
     * <pre>
     * {@code
     * SeaCatClient.ping(new com.teskalabs.seacat.android.client.ping.Ping() {
     *      public void pong() {
     *          // Received pong from gateway
     *      }
     *
     *      public void cancel() {
     *          // Ping timeout occured
     *      }
     * });
     * }
     * </pre>
     * </p>
     *
     * @param ping Instance of <tt>com.teskalabs.seacat.android.client.ping.Ping</tt> class or its child.
     * @throws IOException Generic IO error occurred.
     */
	public static void ping(Ping ping) throws IOException
	{
        getReactor().pingFactory.ping(reactor, ping);
	}


    ///

    /**
     * The <tt>java.net</tt> compatible HTTP client interface.
     *
     * <p>Executes HTTP/HTTPS call via SeaCat gateway.</p>
     *
     * <p>
     * Example:
     * <pre>
     * {@code
     * URL url = new URL("https://backendhost.seacat/remote-api");
     * HttpURLConnection conn = SeaCatClient.open(url);
     *
     * InputStream is = conn.getInputStream();
     * String line;
     * String result = "";
     * BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
     * while ((line = rd.readLine()) != null) {
     *      result += line;
     * }
     * rd.close();
     * is.close();
     * }
     * </pre>
     * </p>
     *
     * @param url the URL that represents the resource that created <tt>HttpURLConnection</tt> will point to.
     * @return a new <tt>HttpURLConnection</tt> connection to the resource referred to by this URL.
     * @throws IOException if an error occurs while opening the connection.
     */
	public static HttpURLConnection open(URL url) throws IOException
	{
		return new URLConnection(getReactor(), url, 3 /*priority*/);
	}

    /**
     * This is a convenience function that translates string URL into <tt>java.net.URL</tt> and calls <tt>open(URL url)</tt> method of this class.
     *
     * @param url String with the URL that represents the resource that created <tt>HttpURLConnection</tt> will point to.
     * @return <tt>HttpURLConnection</tt> instance, see <tt>open(URL url)</tt>.
     * @throws IOException if generic IO error occurred.
     * @throws MalformedURLException if spec could not be parsed as a URL or has an unsupported protocol.
     */
	public static HttpURLConnection open(String url) throws IOException, MalformedURLException
	{
		return open(new URL(url));
	}

    ///

    /**
     * The <tt>org.apache.http.client</tt> compatible HTTP client interface (see <a href="http://hc.apache.org/">Apache HttpComponents Client</a> ).
     *
     * <p>Executes HTTP/HTTPS call via SeaCat gateway.</p>
     *
     * <p>
     * Example:
     * <pre>
     * {@code
     * HttpClient httpclient = SeaCatClient.httpClient();
     * HttpGet httpget = new HttpGet("https://backendhost.seacat/remote-api");
     * HttpResponse response = httpclient.execute(httpget);
     * HttpEntity entity = response.getEntity();
     * final String output = EntityUtils.toString(entity);
     * }
     * </pre>
     * </p>
     *
     */
    public static HttpClient httpClient(final HttpParams params)
    {
        return new SeaCatHttpClient(params, getReactor());
    }

    /**
     * This is a convenience function for <tt>httpClient(final HttpParams params)</tt> method.
     *
     * @return instance of <tt>HttpClient</tt>.
     */
    public static HttpClient httpClient()
    {
        return new SeaCatHttpClient(getReactor());
    }

    ///

    /**
     * Obtains the state string describing operational conditions of a SeaCat client.
     *
     * <p>
     * The state string is a fixed-length six characters long representation of different SeaCat components.
     * Refer to SeaCat C-Core documentation for detailed information.
     * </p>
     *
     * @return the actual state string.
     */
    public static String getState()
    {
        return seacatcc.state();
    }

    ///

    /**
     * Disconnect from SeaCat gateway.
     *
     * <p>
     * Instruct SeaCat client to close a connection to the SeaCat gateway.
     * There is only little need to call this function directly, SeaCat client control connection automatically.
     * </p>
     *
     * @throws IOException if generic IO error occurred.
     */

    public static void disconnect() throws IOException
    {
        int rc = seacatcc.yield('d');
        RC.checkAndThrowIOException("seacatcc.yield(disconnect)", rc);
    }

    /**
     * Resets the identity of the SeaCat client.
     *
     * <p>
     * Removes client private key and all relevant artifacts such as client certificate.
     * It puts client state to an initial form, effectively restarts all automated routines to obtain identity via CSR.
     * </p>
     *
     * @throws IOException if generic IO error occurred.
     */
    public static void reset() throws IOException
    {
        int rc = seacatcc.yield('r');
        RC.checkAndThrowIOException("seacatcc.yield(reset)", rc);
    }


    ///

    public static void setCSRWorker(Runnable csrWorker)
    {
        SeaCatInternals.setCSRWorker(csrWorker);
    }

    ///

	private SeaCatClient() { } // This is static-only class, so we hide constructor
}
