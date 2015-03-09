package mobi.seacat.client.ping;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import mobi.seacat.client.core.Reactor;
import mobi.seacat.client.core.SPDY;
import mobi.seacat.client.intf.*;
import mobi.seacat.client.util.IntegerCounter;

public class PingFactory implements ICntlFrameConsumer, IFrameProvider
{

	final private IntegerCounter idSequence = new IntegerCounter(1);
	final private BlockingQueue<Ping> outboundPingQueue = new LinkedBlockingQueue<Ping>();
	final private Map<Integer, Ping> waitingPingDict = new HashMap<Integer, Ping>();

	
	synchronized public void ping(Reactor reactor, Ping ping) throws IOException
	{
		outboundPingQueue.add(ping);	
		reactor.registerFrameProvider(this, true);		
	}


	public synchronized void reset()
	{
		idSequence.set(1);
		
		for(Iterator<Map.Entry<Integer, Ping>> it = waitingPingDict.entrySet().iterator(); it.hasNext(); )
		{
			Map.Entry<Integer, Ping> entry = it.next();
			Ping ping = entry.getValue();
			it.remove();

			ping.cancel();
		}
	}

	
	public synchronized void heartBeat(double now)
	{
		for(Iterator<Map.Entry<Integer, Ping>> it = waitingPingDict.entrySet().iterator(); it.hasNext(); )
		{
			Map.Entry<Integer, Ping> entry = it.next();
			Ping ping = entry.getValue();

			if (ping.isExpired(now))
			{
				it.remove();
				ping.cancel();
			}
		}

		for( Iterator<Ping> it = outboundPingQueue.iterator(); it.hasNext(); )
		{
			Ping ping = it.next();

			if (ping.isExpired(now))
			{
				it.remove();
				ping.cancel();
			}
		}

	}


	@Override
	synchronized public Result buildFrame(Reactor reactor) throws IOException
	{
		ByteBuffer frame = null;

		//Integer pingId
		Ping ping = outboundPingQueue.poll();
		if (ping == null) return new IFrameProvider.Result(null, false);

		// This is pong object (response to gateway)
		if (ping instanceof Pong)
		{

		}

		// This is ping object (request to gateway)
		else
		{
			ping.setPingId(idSequence.getAndAdd(2));
			waitingPingDict.put(ping.pingId, ping);
		}
		
		frame = reactor.framePool.borrow("PingFactory.ping");
		SPDY.buildSPD3Ping(frame, ping.pingId);
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
			Ping ping = waitingPingDict.remove(pingId);
			if (ping != null) ping.pong();
			else System.err.println("SeaCat: received pong with unknown id: "+pingId);
			
		}
	
		else
		{
			//Send pong back to server
			outboundPingQueue.add(new Pong(pingId));
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
