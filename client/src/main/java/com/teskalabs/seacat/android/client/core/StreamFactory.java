package com.teskalabs.seacat.android.client.core;

import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.teskalabs.seacat.android.client.SeaCatClient;

import com.teskalabs.seacat.android.client.SeaCatInternals;
import com.teskalabs.seacat.android.client.intf.*;
import com.teskalabs.seacat.android.client.util.IntegerCounter;

public class StreamFactory implements ICntlFrameConsumer, IFrameProvider
{
	final private IntegerCounter streamIdSequence = new IntegerCounter(1); // Synchronized access via streams!
	final private Map<Integer, IStream> streams = new HashMap<Integer, IStream>(); // Synchronized access!
	final private BlockingQueue<ByteBuffer> outboundFrameQueue = new LinkedBlockingQueue<ByteBuffer>(); // Access to this element has to be synchronized
	
	///
	
	protected StreamFactory()
	{
	}

	///
	
	synchronized public int registerStream(IStream stream)
	{
		int	streamId = streamIdSequence.getAndAdd(2);
		IStream prev = streams.put(streamId, stream);
		assert(prev == null);
		return streamId;
	}

	
	synchronized public void unregisterStream(int streamId)
	{
		streams.remove(streamId);
	}

	
	synchronized protected void reset()
	{
		Iterator<Entry<Integer, IStream>> it = streams.entrySet().iterator();
	    while (it.hasNext())
	    {
	        Map.Entry<Integer, IStream> pairs = (Map.Entry<Integer, IStream>)it.next();
	        IStream stream = pairs.getValue();
	        stream.reset();
	    }
	    
	    streamIdSequence.set(1);
		streams.clear();
	}

	
	synchronized protected IStream getStream(int streamId)
	{
		return streams.get(streamId);		
	}

	
	protected boolean receivedALX1_SYN_REPLY(Reactor reactor, ByteBuffer frame, int frameLength, byte frameFlags)
	{
		int streamId = frame.getInt();
		IStream stream = getStream(streamId);
		if (stream == null)
		{
            Log.w(SeaCatInternals.L, "receivedALX1_SYN_REPLY stream not found: " + streamId + " (can be closed already)");
			frame.clear();
			sendRST_STREAM(frame, reactor, streamId, SPDY.RST_STREAM_STATUS_INVALID_STREAM);
			return false;
		}

		
		boolean ret = stream.receivedALX1_SYN_REPLY(reactor, frame, frameLength, frameFlags);
		
		if ((frameFlags & SPDY.FLAG_FIN) == SPDY.FLAG_FIN) unregisterStream(streamId);

		return ret;
	}


	protected boolean receivedSPD3_RST_STREAM(Reactor reactor, ByteBuffer frame, int frameLength, byte frameFlags)
	{
		int streamId = frame.getInt();
		IStream stream = getStream(streamId);
		if (stream == null)
		{
            Log.w(SeaCatInternals.L, "receivedSPD3_RST_STREAM stream not found: " + streamId + " (can be closed already)");
            return true;
		}

		boolean ret = stream.receivedSPD3_RST_STREAM(reactor, frame, frameLength, frameFlags);

		// Remove stream from active map
		unregisterStream(streamId);
		
		return ret;
	}


	public boolean receivedDataFrame(Reactor reactor, ByteBuffer frame)
	{
		int streamId = frame.getInt();		
		IStream stream = getStream(streamId);
		if (stream == null)
		{
            Log.w(SeaCatInternals.L, "receivedDataFrame stream not found: " + streamId + " (can be closed already)");
			frame.clear();
			sendRST_STREAM(frame, reactor, streamId, SPDY.RST_STREAM_STATUS_INVALID_STREAM);
			return false;
		}

		int frameLength = frame.getInt();
		byte frameFlags = (byte)(frameLength >> 24);
		frameLength &= 0xffffff;
		
		boolean ret = stream.receivedDataFrame(reactor, frame, frameLength, frameFlags);

		if ((frameFlags & SPDY.FLAG_FIN) == SPDY.FLAG_FIN) unregisterStream(streamId);
		
		return ret;
	}

	
	@Override
	public boolean receivedControlFrame(Reactor reactor, ByteBuffer frame, int frameVersionType, int frameLength, byte frameFlags)
	{
		// Dispatch control frame
		switch(frameVersionType)
		{
			case (SPDY.CNTL_FRAME_VERSION_ALX1 << 16) | SPDY.CNTL_TYPE_SYN_REPLY:
				return receivedALX1_SYN_REPLY(reactor, frame, frameLength, frameFlags);

			case (SPDY.CNTL_FRAME_VERSION_SPD3 << 16) | SPDY.CNTL_TYPE_RST_STREAM:
				return receivedSPD3_RST_STREAM(reactor, frame, frameLength, frameFlags);

			default:
                Log.e(SeaCatInternals.L, "StreamFactory.receivedControlFrame cannot handle frame: "+ frameVersionType);
				return true;
		}
	}

	///
	
	public void sendRST_STREAM(ByteBuffer frame, Reactor reactor, int streamId, int statusCode)
	{
		SPDY.buildSPD3RstStream(frame, streamId, statusCode);
		try {
			addOutboundFrame(frame, reactor);
		} catch (IOException e) {
			reactor.framePool.giveBack(frame); // Return frame
			e.printStackTrace();
		}
	}
	
	private void addOutboundFrame(ByteBuffer frame, Reactor reactor) throws IOException
	{
		outboundFrameQueue.add(frame);
		reactor.registerFrameProvider(this, true);
	}

	
	@Override
	public Result buildFrame(Reactor reactor)
	{
		boolean keep;
		ByteBuffer frame;

		frame = outboundFrameQueue.poll();
		keep = !outboundFrameQueue.isEmpty();

		return new IFrameProvider.Result(frame, keep);
	}


	@Override
	public int getFrameProviderPriority()
	{
		return 1;
	}


}
