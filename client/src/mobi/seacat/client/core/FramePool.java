package mobi.seacat.client.core;

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

	final private AtomicInteger totalCount = new AtomicInteger(0);
	
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

	
	public ByteBuffer borrow(String reason) throws IOException
	{
		ByteBuffer frame;
		try
		{
			synchronized(stack)
			{
				frame = stack.pop();
			}
			
			assert(frame.getInt(0) == 0xFFF1FEFF);
		}

		catch (java.util.EmptyStackException e)
		{
			if (totalCount.intValue() >= highWaterMark) throw new IOException("No more available frames in the pool.");
			frame = createByteBuffer();
		}
		
		return frame;
	}

	
	public void giveBack(ByteBuffer frame)
	{
		if (totalCount.intValue() > lowWaterMark)
		{
			frame.clear();
			totalCount.decrementAndGet();
			System.err.println("TRACE: Frame discarted - current count: " + totalCount.get());
			// Discard frame
		}

		else
		{
			frame.clear();
			synchronized(stack)
			{
				frame.putInt(0, 0xFFF1FEFF);
				stack.push(frame);
			}
		}
    }
	

	private synchronized ByteBuffer createByteBuffer()
	{
		totalCount.incrementAndGet();
		ByteBuffer frame = ByteBuffer.allocateDirect(frameSize);
		System.err.println("TRACE: New frame created - current count: " + totalCount.get());
		return frame;
	}

	
	public int size()
	{
		synchronized(stack)
		{
			return stack.size();
		}
	}


	public int capacity()
	{
		return totalCount.get();
	}

}
