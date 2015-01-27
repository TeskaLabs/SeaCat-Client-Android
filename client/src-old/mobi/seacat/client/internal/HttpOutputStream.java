package mobi.seacat.client.internal;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

public class HttpOutputStream extends OutputStream
{
	private final Reactor reactor;
	private final WeakReference<HttpURLConnectionImpl> parent;

	private int streamId = -1;
	private boolean closed = false;
	private ByteBuffer currentFrame = null;

	
	public HttpOutputStream(HttpURLConnectionImpl parent, Reactor reactor)
	{
		super();
		this.reactor = reactor;
		this.parent = new WeakReference<HttpURLConnectionImpl>(parent);
	}

	
	protected ByteBuffer getCurrentFrame() throws IOException
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

	void flushCurrentFrame(boolean fin_flag) throws IOException
	{
		assert(currentFrame != null);
		if (streamId <= 0) throw new IOException("Invalid Stream id: "+streamId);
		
		ByteBuffer frame = currentFrame;
		currentFrame = null;

		reactor.sendDATA(frame, streamId, fin_flag);
	}

	void checkCurrentFrame() throws IOException
	{
		if (currentFrame == null) return;
		if (currentFrame.remaining() == 0) flushCurrentFrame(false);
	}
	
	///
	
	@Override
	public void write(int oneByte) throws IOException
	{
		if (closed) new IOException("OutputStream is already closed");

		ByteBuffer frame = getCurrentFrame();
		frame.put((byte)oneByte);
		checkCurrentFrame();
	}


	@Override
	public void close() throws IOException
	{
		super.close();
		if (closed) return; // Multiple calls to close() method are supported (and actually required)

		if (streamId == -1)
		{
			// We are closed even before SYN_FRAME was sent
			HttpURLConnectionImpl conn = parent.get();
			if (conn != null) conn.advance(HttpURLConnectionImpl.OutboundState.SYN_STREAM_SENT);

		}
		
		if (currentFrame == null) getCurrentFrame();
		flushCurrentFrame(true);
		closed = true;

		HttpURLConnectionImpl conn = parent.get();
		if (conn != null) conn.setFIN_FLAG_SENT();
	}

	///

	public void setStreamId(int streamId)
	{
		assert(streamId > 0);
		assert((streamId & 0x80000000) == 0);
		this.streamId = streamId;
	}
	

}
