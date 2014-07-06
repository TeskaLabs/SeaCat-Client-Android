package mobi.seacat.client.android.test;

import android.test.AndroidTestCase;

import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeoutException;

import mobi.seacat.client.SeaCatClient;
import mobi.seacat.client.internal.FramePool;
import mobi.seacat.client.internal.FramePool.HighWaterMarkReachedException;
import mobi.seacat.client.internal.Reactor;


public class ReactorTest extends AndroidTestCase
{
	FramePool framePool;

	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		this.framePool = new FramePool();
	}
	
	/*
	public void testShutdown() throws IOException
	{
		
		Reactor r = new Reactor(framePool);		
		r.shutdown();
	}
	*/

/*
	public void testPing() throws IOException, HighWaterMarkReachedException, TimeoutException
	{
		
		Reactor r = new Reactor(framePool);
		r.ping(5000);
		r.ping(5000);
		r.ping(5000);
		r.shutdown();
	}
*/
}
