package com.teskalabs.seacat.android.client.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.teskalabs.seacat.android.client.core.SPDY;

public class InboundStream extends java.io.InputStream
{
	private final URLConnection conn;
	private final BlockingQueue<ByteBuffer> frameQueue = new LinkedBlockingQueue<ByteBuffer>();
	private ByteBuffer currentFrame = null;
	private boolean closed = false;
		
	static private final ByteBuffer QUEUE_IS_CLOSED = ByteBuffer.allocate(0);
	static private final int requestTimeout = 500000; // Milliseconds 

	///
	
	public InboundStream(URLConnection myConnection)
	{
		super();
		this.conn = myConnection;
	}

	///
	
	public boolean inboundData(ByteBuffer frame)
	{
		if (closed)
		{
			// This stream is closed -> send RST_STREAM back
			conn.reactor.streamFactory.sendRST_STREAM(frame, conn.reactor, conn.getStreamId(), SPDY.RST_STREAM_STATUS_STREAM_ALREADY_CLOSED);
			return false;
		}
		frameQueue.add(frame);
		return false; // We will return frame to pool on our own
	}

	///
	
	protected ByteBuffer getCurrentFrame() throws IOException
	{
		if (currentFrame != null)
		{
			if (currentFrame.remaining() == 0)
			{
				conn.reactor.framePool.giveBack(currentFrame);
				currentFrame = null;
			}
			
			return currentFrame;
		}

		while (currentFrame == null)
		{
			try
			{
				currentFrame = frameQueue.poll(requestTimeout, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e) { continue ; }
			
			if (currentFrame == null)
			{
				throw new java.io.IOException("Request timeout");
			}
			
			if (currentFrame == QUEUE_IS_CLOSED)
			{				
				frameQueue.add(QUEUE_IS_CLOSED);
				currentFrame = null;
			}

			break;
		}

		return currentFrame;
	}
	
	// From java.io.InputStream
	
	@Override
	public int read() throws IOException
	{
		ByteBuffer frame = getCurrentFrame();
		if (frame == null) return -1;		
		return frame.get();
	}

	@Override
	public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException
	{
		if (byteOffset < 0 || byteCount < 0 || byteOffset + byteCount > buffer.length) throw new IndexOutOfBoundsException();

		ByteBuffer frame = getCurrentFrame();
		if (frame == null) return -1;		

		if (byteCount > frame.remaining()) byteCount = frame.remaining(); 
		frame.get(buffer, byteOffset, byteCount);
		return byteCount;
	}

	@Override
	public void close()
	{
		if (closed) return;
		closed = true;
		frameQueue.add(QUEUE_IS_CLOSED);
	}

	/*
	 * Reset is call that closes this stream when error occures
	 */
	public void reset()
	{
		//TODO: Set some kind of error  

		frameQueue.add(QUEUE_IS_CLOSED);
		while (frameQueue.size() > 1)
		{
			ByteBuffer frame = frameQueue.remove();
			if (frame != QUEUE_IS_CLOSED) conn.reactor.framePool.giveBack(frame);
		}

		if (currentFrame != null)
		{
			conn.reactor.framePool.giveBack(currentFrame);
			currentFrame = null;
		}

		close();
	}
	
	// TODO: java.io.InputStream.skip() implementation

	protected void finalize() throws Throwable
	{
	     try {
	         close();        // close open files
	     } finally {
	         super.finalize();
	     }
	 }
	
	
}
