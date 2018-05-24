package com.teskalabs.seacat.android.client;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.EnumSet;
import java.util.Iterator;

import com.teskalabs.seacat.android.client.core.Reactor;
import com.teskalabs.seacat.android.client.http.URLConnection;
import com.teskalabs.seacat.android.client.ping.Ping;
import com.teskalabs.seacat.android.client.socket.SocketConfig;
import com.teskalabs.seacat.android.client.util.RC;
import com.teskalabs.seacat.android.client.core.seacatcc;

import javax.crypto.spec.SecretKeySpec;


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
	static private Reactor reactor = null;

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


    public static final String ACTION_SEACAT_USER_AUTH_NEEDED = "mobi.seacat.client.intent.action.USER_AUTH_NEEDED";
    public final static String ACTION_SEACAT_SECURE_LOCK_NEEDED = "mobi.seacat.client.intent.action.SECURE_LOCK_NEEDED";

    /**
     * The <tt>Intent</tt> action used to inform that client state changed.
     * Detailed information about state change is in <tt>EXTRA_STATE</tt>.
     */
    public final static String ACTION_SEACAT_STATE_CHANGED = "mobi.seacat.client.intent.action.STATE_CHANGED";

    public final static String ACTION_SEACAT_CLIENTID_CHANGED = "mobi.seacat.client.intent.action.CLIENTID_CHANGED";


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
    public final static String EXTRA_PREV_STATE = "SEACAT_PREV_STATE";

    public final static String EXTRA_CLIENT_ID = "SEACAT_CLIENT_ID";
    public final static String EXTRA_CLIENT_TAG = "SEACAT_CLIENT_TAG";

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
        SeaCatClient.initialize(context, CSR.submitDefault(), null);
    }

    public static void initialize(Context context, String applicationIdSuffix)
    {
        SeaCatClient.initialize(context, CSR.submitDefault(), applicationIdSuffix);
    }

    public static void initialize(Context context, Runnable CSRworker)
    {
        SeaCatClient.initialize(context, CSRworker, null);
    }

    public static void initialize(Context context, Runnable CSRworker, String applicationIdSuffix)
    {
        SeaCatInternals.applicationIdSuffix = applicationIdSuffix;
        setCSRWorker(CSRworker);

        try {
            reactor = new Reactor(context);
        } catch (IOException e) {
            Log.e(SeaCatInternals.L, "Exception during SeaCat reactor start", e);
        }

        // Process plugins
        SeaCatPlugin.commitCharacteristics(context);
    }

    /**
     * Triggers sending of an <tt>ACTION_SEACAT_STATE_CHANGED</tt> <tt>Intent</tt> even if the state has not changed.
     *
     * <p>Safe to use, it helps to keep your UI code nice.</p>
     */
    public static void broadcastState()
    {
        Reactor reactor = getReactor();
        if (reactor != null) reactor.broadcastState();
    }

    ///

    static Reactor getReactor()
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

    public static void ping() throws IOException
    {
        getReactor().pingFactory.ping(reactor, new Ping() {});
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
	public static HttpURLConnection open(String url) throws IOException
	{
		return open(new URL(url));
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
     * Connect to SeaCat gateway.
     *
     *
     * @throws IOException if generic IO error occurred.
     */

    public static void connect() throws IOException
    {
        int rc = seacatcc.yield('c');
        RC.checkAndThrowIOException("seacatcc.yield(connect)", rc);
    }

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
        SeaCatInternals.getAuth().reset();
        int rc = seacatcc.yield('r');
        RC.checkAndThrowIOException("seacatcc.yield(reset)", rc);


    }

    public static void renew() throws IOException
    {
        int rc = seacatcc.yield('n');
        RC.checkAndThrowIOException("seacatcc.yield(renew)", rc);
    }

    ///

    public static void setCSRWorker(Runnable csrWorker)
    {
        SeaCatInternals.setCSRWorker(csrWorker);
    }

    ///

    public enum LogFlag
    {
        DEBUG_GENERIC(0x0000000000000001);

        public static final EnumSet<LogFlag> ALL_SET = EnumSet.allOf(LogFlag.class);
        public static final EnumSet<LogFlag> NONE_SET = EnumSet.noneOf(LogFlag.class);

        LogFlag(long v)
        {
            this.value = v;
        }
        private final long value;
    }

    public static void setLogMask(EnumSet<LogFlag> mask) throws IOException
    {
        Iterator itr = mask.iterator();
        long bitmask = 0L;
        while(itr.hasNext())
        {
            LogFlag flag = (LogFlag) itr.next();
            bitmask |= flag.value;
        }
        int rc = seacatcc.log_set_mask(bitmask);
        RC.checkAndThrowIOException("seacatcc.log_set_mask()", rc);

        SeaCatInternals.logDebug = mask.contains(LogFlag.DEBUG_GENERIC);
    }

    ///

    public static void configureSocket(int port, SocketConfig.Domain domain, SocketConfig.Type type, int protocol, String peerAddress, String peerPort) throws IOException
    {
        int rc = seacatcc.socket_configure_worker(port, domain.getValue(), type.getValue(), protocol, peerAddress, peerPort);
        RC.checkAndThrowIOException("seacatcc.socket_configure_worker()", rc);
    }

    ///

    public static void setPackageName(String packageName)
    {
        Reactor.setPackageName(packageName);
    }

    ///

    public static String getClientId()
    {
        Reactor r = getReactor();
        if (r == null) return null;
        return r.getClientId();
    }

    public static String getClientTag()
    {
        Reactor r = getReactor();
        if (r == null) return null;
        return r.getClientTag();
    }

    ///

	/**
	 * Construct a derived key. The key is derived from a primary private key.
	 * If identity is reset (by calling reset() method), derived keys will automatically change.
	 *
	 * <p>
	 * Example:
	 * <pre>
	 * {@code
	 * SecretKeySpec skeySpec = SeaCatClient.deriveKey("aes-key-1", "AES", 32);
	 * Cipher cipher = Cipher.getInstance("AES");
	 * cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
	 * cipher.doFinal("To be encrypted.".getBytes("UTF8"));
	 * }
	 * </pre>
	 * </p>
     *
	 *
	 * @param keyId Identification of the key.
	 * @param algorithm Name of the key algorithm such as 'AES'. See https://docs.oracle.com/javase/7/docs/technotes/guides/security/crypto/CryptoSpec.html#AppA for details
	 * @param length Length of the key in bytes. It must be 16 for AES-128), 24 for AES-192, or 32 for AES-256.
	 *
	 */

	public static SecretKeySpec deriveKey(String keyId, String algorithm, int length)
    {
        Reactor r = getReactor();
        if (r == null) return null;
        byte[] key =  r.deriveKey(keyId, length);
        if (key == null) return null;

        return new SecretKeySpec(key, algorithm);
    }

    ///

    public static void startAuth()
    {
        Reactor r = getReactor();
        if (r == null) return;
        SeaCatInternals.getAuth().startAuth(reactor);
    }

    public static void deauth()
    {
        Reactor r = getReactor();
        if (r == null) return;
        SeaCatInternals.getAuth().deauth();
    }


    ///

    private SeaCatClient() { } // This is static-only class, so we hide constructor
}
