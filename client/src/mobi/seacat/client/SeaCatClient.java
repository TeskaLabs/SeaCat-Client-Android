package mobi.seacat.client;

import java.io.IOException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeoutException;

import mobi.seacat.client.internal.FramePool;
import mobi.seacat.client.internal.FramePool.HighWaterMarkReachedException;
import mobi.seacat.client.internal.Reactor;

public final class SeaCatClient
{
	static final private FramePool framePool = new FramePool();
	static private Reactor reactor = null;

	public static synchronized Reactor connect() throws IOException
	{
		if (reactor != null)
		{
			try
			{
				reactor.waitForReady();
			}
			
			catch (SeaCatClosedException e)
			{
				reactor = null;
			}
		}

		if (reactor == null)
		{
			reactor = new Reactor(framePool);
		}

		return reactor;
	}

	
	public static void disconnect() throws IOException
	{
		Reactor lreactor = reactor;
		if (lreactor != null) lreactor.shutdown();
	}

	///
	
	public static HttpURLConnection open(URL url)
	{
		return new mobi.seacat.client.internal.HttpURLConnectionImpl(url);
	}

	///

	public static void ping(long timeout) throws IOException, HighWaterMarkReachedException, TimeoutException
	{
		connect();
		Reactor lreactor = reactor;
		if (lreactor != null) lreactor.ping(timeout);
	}

	///
	
	public static void giveBack(ByteBuffer frame)
	{
		framePool.giveBack(frame);
	}
	
	/*
	 * Mainly for testing 
	 */
	public static int getFramePoolSize()
	{
		return framePool.size();
	}

	/*
	 * Mainly for testing 
	 */
	public static int getFramePoolCapacity()
	{
		return framePool.capacity();
	}

	///

	protected SeaCatClient() { }

}
