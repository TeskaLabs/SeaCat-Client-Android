package mobi.seacat.client;

import android.content.Context;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import mobi.seacat.client.core.Reactor;
import mobi.seacat.client.ping.Ping;

public final class SeaCatClient
{
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

	///

	// disconnect;
	// reset

	///

	protected SeaCatClient() { }
}
