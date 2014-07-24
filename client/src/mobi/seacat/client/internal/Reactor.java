package mobi.seacat.client.internal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;

import mobi.seacat.client.SeaCatClosedException;
import mobi.seacat.client.SeaCatIOException;
import mobi.seacat.client.internal.FramePool.HighWaterMarkReachedException;

public class Reactor implements Runnable
{
	static private boolean instantiated = false; 
	
	final public FramePool framePool;
	final private Thread sessionThread;

	final private BlockingQueue<ByteBuffer> outboundFrameQueue = new  LinkedBlockingQueue<ByteBuffer>(); 
	
	final protected Status readyStatus = new Status(1); // Not ready
	final protected Status shutdownStatus = new Status(1); // Not shutdown
	
	final protected IntegerCounter pingIdSequence = new IntegerCounter(1);
	final protected ConcurrentHashMap<Integer, Integer> pingWaits = new ConcurrentHashMap<Integer, Integer>();

	private final IntegerCounter streamIdSequence = new IntegerCounter(1);
	private final ConcurrentHashMap<Integer, InboundStream> streams = new ConcurrentHashMap<Integer, InboundStream>();
	
	public Reactor(FramePool framePool) throws IOException
	{
		if (Reactor.instantiated) throw new IOException("Reactor is already created.");

		this.framePool = framePool;
		this.sessionThread = new Thread(this);
		this.sessionThread.setName("SeaCat Reactor");
		this.sessionThread.start();
		
		waitForReady();
		Reactor.instantiated = true;
	}

	
	public void shutdown() throws IOException
	{
		int rc;
		
		if (shutdownStatus.getStatus() != 1) return;
		
		synchronized(shutdownStatus)
		{
			while (shutdownStatus.getStatus() == 1)
			{
				// Since shutdown signal can be 'lost', we rather send that repeativelly times if needed 
				rc = JNI.seacat_reactor_shutdown();
				if (rc < 0) throw SeaCatIOException.create(rc);

				try {
					shutdownStatus.wait(500); // Milliseconds
				}				
				catch (InterruptedException e) {}
			}				
		}

		rc = shutdownStatus.getStatus();
		if (rc < 0) throw SeaCatIOException.create(rc);

		while (true)
		{
			try {
				this.sessionThread.join();
			}			
			catch (InterruptedException e) {
				e.printStackTrace();
			}
			break;
		}

		Reactor.instantiated = false;
	}
	
	
	public void waitForReady() throws IOException
	{
		int rc;

		if (shutdownStatus.getStatus() != 1)
			throw new SeaCatClosedException(shutdownStatus.getStatus());
		
		synchronized(readyStatus)
		{
			try
			{
				while (readyStatus.getStatus() == 1)
				{
					readyStatus.wait();
				}
			}
				
			catch (InterruptedException e)
			{
					e.printStackTrace();
			}
			
			rc = readyStatus.getStatus();
			if (rc < 0) throw SeaCatIOException.create(rc);	
		}
	}


	@Override
	public void run()
	{
		// We are in the session thread ...
		int rc;
		
		rc = JNI.seacat_reactor_init(this);

		// Tell the world that we are ready
		synchronized (readyStatus)
	    {
			readyStatus.setStatus(rc);
			readyStatus.notifyAll();
	    }
		if (readyStatus.getStatus() != 0) return;

		rc = JNI.seacat_reactor_run();

		// Tell the world that we are going down
		synchronized (shutdownStatus)
	    {
			shutdownStatus.setStatus(rc);
			shutdownStatus.notifyAll();
	    }

		JNI.seacat_reactor_fini();

		// Terminate all pending streams with faked FIN
		while (!streams.isEmpty())
		{
			for (Entry<Integer, InboundStream> entry : streams.entrySet()) 
			{
				int streamId = entry.getKey();
				InboundStream stream = entry.getValue();
				try
				{
					stream.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				streams.remove(streamId);					
			}
		}
		
		// Return all pending outboundFrameQueue
		while (!outboundFrameQueue.isEmpty())
		{
			ByteBuffer frame = outboundFrameQueue.remove();
			framePool.giveBack(frame);
		}
		
	}

	
	private void send(ByteBuffer frame) throws SeaCatClosedException
	{
		if (shutdownStatus.getStatus() != 1) throw new SeaCatClosedException(shutdownStatus.getStatus());

		while (true)
		{
			try
			{
				outboundFrameQueue.put(frame);
			}
			
			catch (InterruptedException e)
			{
				continue;
			}
			
			break;
		}
		JNI.seacat_reactor_yield();
	}

	
	public void sendDATA(ByteBuffer frame, int streamId, boolean fin_flag) throws IOException
	{
		int frameLen = 0;
		frameLen = frame.position() - SPDY.HEADER_SIZE;
		assert(frameLen >= 0);

		frame.putInt(0, streamId);
		frame.putInt(4, ((fin_flag ? SPDY.FLAG_FIN : 0) << 24) | frameLen);
		
		frame.flip();

		// Send a frame
		try
		{
			send(frame);
		}

		catch (SeaCatClosedException e)
		{
			framePool.giveBack(frame);
			throw e;
		}
	}
	

	public HttpInputStream sendSYN_STREAM(HttpURLConnectionImpl conn, boolean fin_flag, int priority) throws IOException
	{
		HttpInputStream stream;
		ByteBuffer frame = framePool.borrow("Reactor.sendSYN_STREAM");

		synchronized (streamIdSequence)
		{
			final int streamId = streamIdSequence.getAndAdd(2);
			stream = new HttpInputStream(streamId, conn);
	
			// Build SYN_STREAM frame
			SPDY.buildALX1SynStream(frame, streamId, conn.getURL(), conn.getRequestMethod(), conn.getRequestHeaders(), fin_flag, priority);
			frame.flip();
	
			// Register callbacks
			streams.put(streamId, stream);
			
			// Send a frame
			try
			{
				send(frame);
			}

			catch (SeaCatClosedException e)
			{
				streams.remove(streamId);
				framePool.giveBack(frame);
				throw e;
			}
		}

		return stream;
	}


	public void ping(long timeout) throws IOException, TimeoutException
	{
		ByteBuffer frame = framePool.borrow("Reactor.ping");
		int pingId;
		
		synchronized (pingIdSequence)
		{
			pingId = pingIdSequence.getAndAdd(2);
			SPDY.buildSPD3Ping(frame, pingId);
			frame.flip();
			pingWaits.put(pingId, 0);
			try
			{
				send(frame);
			}
			catch (SeaCatClosedException e)
			{
				pingWaits.remove(pingId);
				framePool.giveBack(frame);
				throw e;
			}

		}

		long tBefore=System.currentTimeMillis();
		int rc;
		synchronized (pingWaits)
	    {
			while (true)
			{
				rc = pingWaits.get(pingId);
				if (rc != 0) break;
				
				if ((System.currentTimeMillis() - tBefore) > timeout)
					throw new TimeoutException();

				try
				{
					pingWaits.wait(1000);
				}
				
				catch (InterruptedException e)
				{
					continue;
				}
			}
			pingWaits.remove(pingId);
	    }
		
	}
	
	
	protected ByteBuffer JNICallbackWriteReady()
	{
		return outboundFrameQueue.poll();
	}


	protected ByteBuffer JNICallbackReadReady()
	{
		try
		{
			return framePool.borrow("Reactor.JNICallbackReadReady");
		}
		
		catch (HighWaterMarkReachedException e)
		{
			return null;
		}
	}


	protected void JNICallbackFrameSent(ByteBuffer frame)
	{
		framePool.giveBack(frame);
	}


	protected void JNICallbackFrameReceived(ByteBuffer frame)
	{
		frame.flip();

		byte fb = frame.get(0);
		boolean giveBackFrame = true;

		try
		{
			if ((fb & (1L << 7)) != 0)
			{
				giveBackFrame = receivedControlFrame(frame);
			}
			
			else
			{
				giveBackFrame = receivedDataFrame(frame);
			}
		}
		
		finally
		{
			if (giveBackFrame) framePool.giveBack(frame);
		}
	}


	protected void JNICallbackFrameStalled(ByteBuffer frame)
	{
		// Called when reactor run() exits and some frames are still 'in use', stalled ...
		framePool.giveBack(frame);
	}

	
	protected boolean receivedControlFrame(ByteBuffer frame)
	{
		short frameVersion = (short)(frame.getShort() & 0x7fff);
		short frameType = frame.getShort();
		int frameLength = frame.getInt();
		byte frameFlags = (byte)(frameLength >> 24);
		frameLength &= 0xffffff;

		if (frameLength + SPDY.HEADER_SIZE != frame.limit())
		{
			System.out.println(String.format("Incorrect frame received: %d %x %x %d %x - closing connection", frame.limit(), frameVersion, frameType, frameLength, frameFlags));

			// Invalid frame received - shutdown a reactor (disconnect) ...
			try
			{
				shutdown();
			}
			
			catch (IOException e) { }
			return true;
		}
		
		if ((frameVersion == SPDY.CNTL_FRAME_VERSION_ALX1) && (frameType == SPDY.CNTL_TYPE_SYN_REPLY))
			return receivedControlFrame_ALX1_SYN_REPLY(frame, (frameFlags & SPDY.FLAG_FIN) == SPDY.FLAG_FIN);

		else if ((frameVersion == SPDY.CNTL_FRAME_VERSION_SPD3) && (frameType == SPDY.CNTL_TYPE_PING))
			return receivedControlFrame_SPD3_PING(frame);

		System.out.println(String.format("Unidentified Control frame received: %d %x %x %d %x", frame.limit(), frameVersion, frameType, frameLength, frameFlags));
		return true;
	}

	
	protected boolean receivedControlFrame_ALX1_SYN_REPLY(ByteBuffer frame, boolean fin_flag)
	{
		int streamId = frame.getInt();
		
		InboundStream stream = streams.get(streamId);
		if (stream == null)
		{
			System.out.println(String.format("Unlinkable ALX1 SYN_REPLY received: %d%s %d", frame.limit(), fin_flag ? " FIN" : "", streamId));

			//TODO: Send RST_STREAM - this is non-existent stream
			return true;
		}

		stream.inboundSynReply(frame);
		if (fin_flag)
		{
			stream.inboundFIN();
			streams.remove(streamId);
		}

		return true;
	}

	
	protected boolean receivedControlFrame_SPD3_PING(ByteBuffer frame)
	{
		int pingId = frame.getInt();
		if ((pingId % 2) == 1)
		{
			// Pong frame received ...
			synchronized (pingWaits)
		    {
				int value = pingWaits.get(pingId);
				pingWaits.replace(pingId, value + 1);
				pingWaits.notifyAll();
		    }

		}
	
		else
		{
			//TODO: This is so far untested code ... 
			try
			{
				ByteBuffer pongFrame = framePool.borrow("Reactor.receivedControlFrame_SPD3_PING");				
				frame.rewind();
				pongFrame.put(frame);
				pongFrame.flip();
				
				try
				{
					send(pongFrame);
				}
				catch (SeaCatClosedException e)
				{
					framePool.giveBack(pongFrame);
				}

			}
			
			catch (HighWaterMarkReachedException e){}
		}

		return true;
	}


	protected boolean receivedDataFrame(ByteBuffer frame)
	{
		int streamId = frame.getInt();
		int frameLength = frame.getInt();
		byte frameFlags = (byte)(frameLength >> 24);
		frameLength &= 0xffffff;

		if ((frameLength + SPDY.HEADER_SIZE != frame.limit()) || ((streamId & 0x80000000) != 0))
		{
			System.out.println(String.format("Incorrect frame received: %d %d %x - closing connection", frame.limit(), frameLength, frameFlags));

			// Invalid frame received - shutdown a reactor (disconnect) ...
			try
			{
				shutdown();
			}
			
			catch (IOException e) { }
			return true;
		}

		boolean fin_flag = (frameFlags & SPDY.FLAG_FIN) == SPDY.FLAG_FIN;
		
		InboundStream stream = streams.get(streamId);
		if (stream == null)
		{
			System.out.println(String.format("Unlinkable DATA received: %d%s %d", frame.limit(), fin_flag ? " FIN" : "", streamId));

			//TODO: Send RST_STREAM - this is non-existent stream
			return true;
		}

		boolean ret = stream.inboundData(frame);
		if (fin_flag)
		{
			stream.inboundFIN();
			streams.remove(streamId);
		}
		return ret;
	}

}
