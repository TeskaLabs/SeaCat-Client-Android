package com.teskalabs.seacat.android.client;

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
    static private Runnable CSRWorker = CSR.createDefault();

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
		return new URLConnection(getReactor(), url, 3 /*priority*/);
	}

	public static HttpURLConnection open(String url) throws IOException, MalformedURLException
	{
		return open(new URL(url));
	}


    /*
     * Create org.apache.http.client.HttpClient hc.apache.org
     */
    public static HttpClient httpClient()
    {
        return new SeaCatHttpClient(getReactor());
    }

    public static HttpClient httpClient(final HttpParams params)
    {
        return new SeaCatHttpClient(params, getReactor());
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
        if (reactor != null) reactor.broadcastState(getState());
    }

    public static String getState()
    {
        return seacatcc.state();
    }

    ///

    public static void setCSRWorker(Runnable CSRWorker)
    {
        SeaCatClient.CSRWorker = CSRWorker;
    }

    public static Runnable getCSRWorker()
    {
        return SeaCatClient.CSRWorker;
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
