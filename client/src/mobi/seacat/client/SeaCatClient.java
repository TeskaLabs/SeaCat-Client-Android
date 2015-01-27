package mobi.seacat.client;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import mobi.seacat.client.core.Reactor;
import mobi.seacat.client.intf.IDelegate;

public final class SeaCatClient
{
	static private WeakReference<IDelegate> delegate = null;
	static private Reactor reactor = null; 
	
	///

	synchronized public static void configure(IDelegate delegate) throws IOException
	{
		if (reactor != null) throw new IOException("Already configured.");
		Reactor lreactor = new Reactor();
		
		if (delegate == null) SeaCatClient.delegate = null;
		else SeaCatClient.delegate = new WeakReference<IDelegate>(delegate);

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

	public static void configure() throws IOException
	{
		SeaCatClient.configure(null);
	}
	
	///
	
	public static int ping() throws IOException
	{
		if (reactor == null) throw new IOException("Not configured.");
		return reactor.pingFactory.ping(reactor);
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

	public static IDelegate getDelegate()
	{
		if (delegate == null) return null;
		return delegate.get();
	}

	///

	protected SeaCatClient() { }

}
