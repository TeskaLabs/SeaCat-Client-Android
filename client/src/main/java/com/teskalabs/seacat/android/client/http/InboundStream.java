package com.teskalabs.seacat.android.client.http;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.teskalabs.seacat.android.client.core.Reactor;
import com.teskalabs.seacat.android.client.core.SPDY;

/*
 * This class is used also by com.teskalabs.seacat.android.client.hc - so it is a bit universal
 */

public class InboundStream extends java.io.InputStream
{
    private final Reactor reactor;
    private int streamId = -1;

    // TODO: Allow parametrization of LinkedBlockingQueue capacity
	private final BlockingQueue<ByteBuffer> frameQueue = new LinkedBlockingQueue<ByteBuffer>();
	private ByteBuffer currentFrame = null;
	private boolean closed = false;

    int readTimeoutMillis = 30*1000;

	static private final ByteBuffer QUEUE_IS_CLOSED = ByteBuffer.allocate(0);

	///
	
	public InboundStream(Reactor reactor, int readTimeoutMillis)
	{
		super();
		this.reactor = reactor;
        this.readTimeoutMillis = readTimeoutMillis;
	}

	///

    public void setStreamId(int streamId)
    {
        this.streamId = streamId;
    }

    public void setReadTimeout(int readTimeoutMillis)
    {
        this.readTimeoutMillis = readTimeoutMillis;
    }

    ///

	public boolean inboundData(ByteBuffer frame)
	{
		if (closed)
		{
			// This stream is closed -> send RST_STREAM back
			reactor.streamFactory.sendRST_STREAM(frame, reactor, this.streamId, SPDY.RST_STREAM_STATUS_STREAM_ALREADY_CLOSED);
			return false;
		}
		frameQueue.add(frame);
		return false; // We will return frame to pool on our own
	}

	///
	
	protected ByteBuffer getCurrentFrame() throws SocketTimeoutException
	{
		if (currentFrame != null)
		{
            if (currentFrame == QUEUE_IS_CLOSED) return null;

            else if (currentFrame.remaining() == 0)
			{
				reactor.framePool.giveBack(currentFrame);
				currentFrame = null;
			}

			else return currentFrame;
		}

        long timeoutMillis = this.readTimeoutMillis;
        if (timeoutMillis == 0) timeoutMillis = 1000*60*3; // 3 minutes timeout
        long cutOfTimeMillis = (System.nanoTime() / 1000000L) + timeoutMillis;

		while (currentFrame == null)
		{
			try
			{
                long awaitMillis = cutOfTimeMillis - (System.nanoTime() / 1000000L);
                if (awaitMillis <= 0) throw new SocketTimeoutException(String.format("Read timeout: %d", this.readTimeoutMillis));

				currentFrame = frameQueue.poll(awaitMillis, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e) { continue ; }

			if (currentFrame == QUEUE_IS_CLOSED)
			{				
				frameQueue.add(QUEUE_IS_CLOSED);
				currentFrame = null;
                break;
			}
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
	 * Reset is call that closes this stream when error happens
	 */
	public void reset()
	{
		//TODO: Set some kind of error  

		frameQueue.add(QUEUE_IS_CLOSED);
		while (frameQueue.size() > 1)
		{
			ByteBuffer frame = frameQueue.remove();
			if (frame != QUEUE_IS_CLOSED) reactor.framePool.giveBack(frame);
		}

		if (currentFrame != null)
		{
			reactor.framePool.giveBack(currentFrame);
			currentFrame = null;
		}

		close();
	}
	
	// TODO: java.io.InputStream.skip() implementation

	protected void finalize() throws Throwable
	{
        if (currentFrame != null)
        {
            reactor.framePool.giveBack(currentFrame);
            currentFrame = null;
        }

        try {
         close();        // close open files
        } finally {
         super.finalize();
        }

	 }
	
	
}
