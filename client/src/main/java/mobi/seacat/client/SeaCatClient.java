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
	
	///

	synchronized public static void configure(Context context) throws IOException
	{
		if (reactor != null) throw new IOException("Already configured.");
		Reactor lreactor = new Reactor(context);

		lreactor.start();
		reactor = lreactor;

		Runtime.getRuntime().addShutdownHook(new Thread()
		{
			public void run()
			{
				try {
					reactor.shutdown();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	///
	
	public static void ping(Ping ping) throws IOException
	{
		if (reactor == null) throw new IOException("Not configured.");
		reactor.pingFactory.ping(reactor, ping);
	}
	
	public static HttpURLConnection open(URL url) throws IOException
	{
		if (reactor == null) throw new IOException("Not configured.");
		return new mobi.seacat.client.http.URLConnection(reactor, url, 3 /*priority*/);
	}

	public static HttpURLConnection open(String url) throws IOException, MalformedURLException
	{
		return open(new URL(url));
	}

	/// Getters
	
	public static boolean isConfigured()
	{
		return (reactor != null);
	}

	///

	protected SeaCatClient() { }

}
