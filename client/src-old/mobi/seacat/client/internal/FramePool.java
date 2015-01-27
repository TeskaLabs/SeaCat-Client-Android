package mobi.seacat.client.internal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import  java.util.prefs.*;

public class FramePool
{
	final private Stack<ByteBuffer> stack = new Stack<ByteBuffer>();
	final private int lowWaterMark;
	final private int highWaterMark;
	final private int frameSize;

	private AtomicInteger totalCount = new AtomicInteger(0);
	
	// Preference keys for this package
    private static final String LOW_WATER_MARK = "lowWaterMark";
    private static final String HIGH_WATER_MARK = "highWaterMark";
    private static final String FRAME_SIZE = "frameSize";
   
	public FramePool()
	{
		Preferences prefs = Preferences.userNodeForPackage(FramePool.class);

		this.lowWaterMark = prefs.getInt(LOW_WATER_MARK, 16);
		this.highWaterMark = prefs.getInt(HIGH_WATER_MARK, 128);
		this.frameSize = prefs.getInt(FRAME_SIZE, 16*1024);
	}

	
	public ByteBuffer borrow(String reason) throws HighWaterMarkReachedException
	{
		ByteBuffer buffer;
		try
		{
			synchronized(stack)
			{
				buffer = stack.pop();
			}
		}

		catch (java.util.EmptyStackException e)
		{
			if (totalCount.intValue() >= highWaterMark) throw new HighWaterMarkReachedException();
			buffer = createByteBuffer();
		}
		
		return buffer;
	}

	
	public void giveBack(ByteBuffer frame)
	{
		if (totalCount.intValue() > lowWaterMark)
		{
			totalCount.decrementAndGet();
			// Discard frame
		}

		else
		{
			frame.clear();
			synchronized(stack)
			{
				stack.push(frame);
			}
		}
    }
	

	private ByteBuffer createByteBuffer()
	{
		totalCount.incrementAndGet();
		return ByteBuffer.allocateDirect(frameSize);
	}

	
	public int size()
	{
		return stack.size();
	}

	public int capacity()
	{
		return totalCount.get();
	}

	///
	
	public class HighWaterMarkReachedException extends IOException
	{
		private static final long serialVersionUID = 8784869389817767261L;

		public HighWaterMarkReachedException()
	    {
	        super("High watermark of frame pool reached - too many frames in use.");
	    }
	}

}
