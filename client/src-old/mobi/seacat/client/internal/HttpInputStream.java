package mobi.seacat.client.internal;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import mobi.seacat.client.SeaCatClient;

public class HttpInputStream extends java.io.InputStream implements InboundStream
{
	private final int streamId;
	private final BlockingQueue<ByteBuffer> inboundFrameQueue = new LinkedBlockingQueue<ByteBuffer>();
	private ByteBuffer currentFrame = null;
	private boolean closed = false;
	private final WeakReference<HttpURLConnectionImpl> parent;

	static private final ByteBuffer QUEUE_IS_DEAD = ByteBuffer.allocate(0);
	
	///
	
	public HttpInputStream(int streamId, HttpURLConnectionImpl parent)
	{
		super();
		this.streamId = streamId;
		this.parent = new WeakReference<HttpURLConnectionImpl>(parent);
	}

	/// From InboundStream
	
	public void inboundSynReply(ByteBuffer frame)
	{
		HttpURLConnectionImpl conn = parent.get();
		if (conn != null) conn.inboundSynReply(frame);
	}

	public boolean inboundData(ByteBuffer frame)
	{
		if (closed) return true; //TODO: Also sent RST_STREAM frame
		inboundFrameQueue.add(frame);
		return false; // We will return frame to pool on our own
	}

	public void inboundRstStream(byte flags, int statusCode)
	{
	}

	public void inboundFIN()
	{
		closed = true;
		inboundFrameQueue.add(QUEUE_IS_DEAD); // Indicate final state of inbound queue
	}

	///
	
	protected ByteBuffer getCurrentFrame()
	{
		if (currentFrame != null)
		{
			if (currentFrame.remaining() == 0)
			{
				SeaCatClient.giveBack(currentFrame);
				currentFrame = null;
			}
		}

		while (currentFrame == null)
		{
			try
			{
				currentFrame = inboundFrameQueue.take();
			}
			catch (InterruptedException e) { continue ; }
			
			if (currentFrame == QUEUE_IS_DEAD)
			{
				inboundFrameQueue.add(QUEUE_IS_DEAD);
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
	public int read(byte[] buffer, int byteOffset, int byteCount)
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
		while (!inboundFrameQueue.isEmpty())
		{
			ByteBuffer frame = inboundFrameQueue.remove();
			if (frame != QUEUE_IS_DEAD) SeaCatClient.giveBack(frame);
		}

		if (currentFrame != null)
		{
			SeaCatClient.giveBack(currentFrame);
			currentFrame = null;
		}

		inboundFIN();
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
	
	// Getters & Setters ...

	public int getStreamId() { return streamId; }
	
}
