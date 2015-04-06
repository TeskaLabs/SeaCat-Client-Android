package mobi.seacat.client;

import android.content.Context;
import android.content.Intent;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import mobi.seacat.client.core.Reactor;
import mobi.seacat.client.core.seacatcc;
import mobi.seacat.client.ping.Ping;
import mobi.seacat.client.util.RC;

public final class SeaCatClient
{

    public final static String ACTION_SEACAT_EVLOOP_STARTED = "mobi.seacat.client.intent.action.EVLOOP_STARTED";
    public final static String ACTION_SEACAT_GWCONN_CONNECTED = "mobi.seacat.client.intent.action.GWCONN_CONNECTED";
    public final static String ACTION_SEACAT_GWCONN_RESET = "mobi.seacat.client.intent.action.GWCONN_RESET";
    public final static String ACTION_SEACAT_STATE_CHANGED = "mobi.seacat.client.intent.action.STATE_CHANGED";

    public final static String CATEGORY_SEACAT = "mobi.seacat.client.intent.category.SEACAT";

    public final static String EXTRA_STATE = "SEACAT_STATE";

    public final static String L = "SeaCat";

    ///

    static private Reactor reactor = null;

    synchronized protected static void setReactor(Reactor reactor)
    {
        SeaCatClient.reactor = reactor;
    }

    synchronized public static Reactor getReactor()
    {
        return SeaCatClient.reactor;
    }

    ///
	
	public static void ping(Ping ping) throws IOException
	{
        getReactor().pingFactory.ping(reactor, ping);
	}
	
	public static HttpURLConnection open(URL url) throws IOException
	{
		return new mobi.seacat.client.http.URLConnection(getReactor(), url, 3 /*priority*/);
	}

	public static HttpURLConnection open(String url) throws IOException, MalformedURLException
	{
		return open(new URL(url));
	}

    public static void reset() throws IOException
    {
        int rc = seacatcc.yield('r');
        RC.checkAndThrowIOException("seacatcc.yield(reset)", rc);
    }

    ///

    public static void broadcastState()
    {
        Reactor reactor = getReactor();
        if (reactor != null) reactor.broadcastState();
    }

    public static String getState()
    {
        Reactor reactor = getReactor();
        if (reactor != null) return reactor.getState();
        return "?????";
    }

	// disconnect;
	// reset

	///

    public static Intent createIntent(String action)
    {
        Intent Intent = new Intent(action);
        Intent.addCategory(SeaCatClient.CATEGORY_SEACAT);
        Intent.addFlags(android.content.Intent.FLAG_FROM_BACKGROUND);
        return Intent;
    }

    ///

	protected SeaCatClient() { } // This is static-only class, so we hide constructor
}
