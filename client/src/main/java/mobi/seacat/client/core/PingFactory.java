package mobi.seacat.client.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import mobi.seacat.client.SeaCatClient;
import mobi.seacat.client.intf.*;
import mobi.seacat.client.util.IntegerCounter;

public class PingFactory implements ICntlFrameConsumer, IFrameProvider
{

	final private IntegerCounter idSequence = new IntegerCounter(1);
	final private BlockingQueue<Integer> outboundPingQueue = new LinkedBlockingQueue<Integer>();
	

	synchronized public int ping(Reactor reactor) throws IOException
	{
		int pingId = idSequence.getAndAdd(2);
		outboundPingQueue.add(pingId);	
		reactor.registerFrameProvider(this, true);
		return pingId;
	}


	synchronized protected void reset()
	{
		idSequence.set(1);
		outboundPingQueue.clear();
	}


	@Override
	synchronized public Result buildFrame(Reactor reactor) throws IOException
	{
		ByteBuffer frame = null;

		Integer pingId = outboundPingQueue.poll();
		if (pingId == null) return new IFrameProvider.Result(null, false);

		frame = reactor.framePool.borrow("PingFactory.ping");

		SPDY.buildSPD3Ping(frame, pingId);
		return new IFrameProvider.Result(frame, !outboundPingQueue.isEmpty());
	}

	
	@Override
	synchronized public boolean receivedControlFrame(Reactor reactor, ByteBuffer frame, int frameVersionType, int frameLength, byte frameFlags)
	{
		//TODO: pingId is unsigned (based on SPDY specifications)
		int pingId = frame.getInt();
		if ((pingId % 2) == 1)
		{
			// Pong frame received ...
			IDelegate d = SeaCatClient.getDelegate();
			if (d != null) d.pong(pingId);
		}
	
		else
		{
			// Send pong back to server
			outboundPingQueue.add(pingId);
			try {
				reactor.registerFrameProvider(this, true);
			} catch (Exception e) {
				// We can ignore error in this case, right?
			}
		}

		return true;
	}

	
	@Override
	public int getFrameProviderPriority()
	{
		return 0;
	}

}
