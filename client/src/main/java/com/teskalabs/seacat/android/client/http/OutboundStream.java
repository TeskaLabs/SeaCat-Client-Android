package com.teskalabs.seacat.android.client.http;

import com.teskalabs.seacat.android.client.core.Reactor;
import com.teskalabs.seacat.android.client.core.SPDY;
import com.teskalabs.seacat.android.client.intf.IFrameProvider;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class OutboundStream extends java.io.OutputStream implements IFrameProvider
{
    private final Reactor reactor;
    private int streamId = -1;


	private BlockingQueue<ByteBuffer> frameQueue = new LinkedBlockingQueue<ByteBuffer>();
	private ByteBuffer currentFrame = null;

    private boolean launched = false;
	private boolean closed = false;

    private int contentLength = 0;
    private int priority;

	///
	
	public OutboundStream(Reactor reactor, int priority)
	{
		super();
		this.reactor = reactor;
        this.priority = priority;
	}

	///

    public void launch() throws IOException
    {
        if (launched) throw new IOException("OutputStream is already launched");

        reactor.registerFrameProvider(this, true);
    }

    ///

	synchronized private ByteBuffer getCurrentFrame() throws IOException
	{
		if (closed) throw new IOException("OutputStream is already closed"); 

		if (currentFrame == null)
		{			
			currentFrame = reactor.framePool.borrow("HttpOutputStream.getCurrentFrame");
			// Make sure that there is a space for DATA header
			currentFrame.position(SPDY.HEADER_SIZE);
		}
		
		return currentFrame;
	}


	synchronized private void flushCurrentFrame(boolean fin_flag) throws IOException
	{
		assert(currentFrame != null);
		assert(fin_flag == closed);

        ByteBuffer aFrame = currentFrame;
        currentFrame = null;

		SPDY.buildDataFrameFlagLength(aFrame, fin_flag);
		
		frameQueue.add(aFrame);

        if (launched) reactor.registerFrameProvider(this, true);
	}


    ///

	@Override
	public void write(int oneByte) throws IOException
	{
		if (closed) throw new IOException("OutputStream is already closed");

		ByteBuffer frame = getCurrentFrame();
		frame.put((byte)oneByte);
        contentLength += 1;
		
		if (currentFrame.remaining() == 0) flushCurrentFrame(false);
	}


	@Override
	public void close() throws IOException
	{
		super.close();
		if (closed) return; // Multiple calls to close() method are supported (and actually required)
		
		if (currentFrame == null)
		{
            // TODO: This means that sub-optimal flush()/close() cycle happened - we will have to send empty DATA frame with FIN_FLAG set
			getCurrentFrame();
		}
		closed = true;
		flushCurrentFrame(true);
	}

	@Override
	public void flush() throws IOException
	{
        super.flush();
        if (currentFrame != null) flushCurrentFrame(false);
	}


	/*
	 * This is emergency 'close' method -> terminate stream functionality at all cost with no damage
	 */
	public void reset()
	{
		closed = true;

		while (!frameQueue.isEmpty())
		{
			ByteBuffer frame = frameQueue.remove();
			reactor.framePool.giveBack(frame);
		}

		if (currentFrame != null)
		{
			reactor.framePool.giveBack(currentFrame);
			currentFrame = null;
		}
	}
	
	///
	
	public int getContentLength() { return contentLength; }

    ///

    @Override
    public Result buildFrame(Reactor reactor) throws IOException
    {
        boolean keep = false;

        assert(streamId > 0);

        ByteBuffer frame = frameQueue.poll();
        if (frame != null)
        {
            frame.putInt(0, streamId);
            keep = !frameQueue.isEmpty();
            if ((frame.getShort(4) & SPDY.FLAG_FIN) == SPDY.FLAG_FIN)
            {
                assert(keep == false);
            }
        }
        return new IFrameProvider.Result(frame, keep);
    }

    @Override
    public int getFrameProviderPriority()
    {
        return priority;
    }

    public void setStreamId(int streamId)
    {
        this.streamId = streamId;
    }

}
