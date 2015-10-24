package mobi.seacat.client.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import mobi.seacat.client.core.SPDY;

public class OutboundStream extends java.io.OutputStream
{
	private final URLConnection conn;
	private BlockingQueue<ByteBuffer> frameQueue = new LinkedBlockingQueue<ByteBuffer>();
	private ByteBuffer currentFrame = null;
	private boolean closed = false;
    private int contentLength = 0;

	///
	
	public OutboundStream(URLConnection myConnection)
	{
		super();
		this.conn = myConnection;
	}

	///
	
	synchronized private ByteBuffer getCurrentFrame() throws IOException
	{
		if (closed) throw new IOException("OutputStream is already closed"); 

		if (currentFrame == null)
		{			
			currentFrame = conn.reactor.framePool.borrow("HttpOutputStream.getCurrentFrame");
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

        if (conn.isLaunched()) conn.reactor.registerFrameProvider(conn, true);
	}


	public ByteBuffer pollFrame()
	{
		return frameQueue.poll();
	}
	
	///

	@Override
	public void write(int oneByte) throws IOException
	{
		if (closed) new IOException("OutputStream is already closed");

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
			conn.reactor.framePool.giveBack(frame);
		}

		if (currentFrame != null)
		{
			conn.reactor.framePool.giveBack(currentFrame);
			currentFrame = null;
		}
	}
	
	///
	
	public boolean isQueueEmpty()
	{
		return frameQueue.isEmpty();
	}
	public boolean isClosed() { return closed; }
    public int getContentLength() { return contentLength; }
}
