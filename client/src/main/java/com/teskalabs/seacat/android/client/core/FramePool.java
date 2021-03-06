package com.teskalabs.seacat.android.client.core;

import android.util.Log;

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
	final private int frameCapacity;

	final private AtomicInteger totalCount = new AtomicInteger(0);
	
	// Preference keys for this package
    private static final String LOW_WATER_MARK = "lowWaterMark";
    private static final String HIGH_WATER_MARK = "highWaterMark";
    private static final String FRAME_CAPACITY = "frameCapacity";
   
	public FramePool()
	{
		Preferences prefs = Preferences.userNodeForPackage(FramePool.class);

		this.lowWaterMark = prefs.getInt(LOW_WATER_MARK, 16);
		this.highWaterMark = prefs.getInt(HIGH_WATER_MARK, 40960);
		this.frameCapacity = prefs.getInt(FRAME_CAPACITY, 16*1024);
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
	

	private synchronized ByteBuffer createByteBuffer()
	{
		totalCount.incrementAndGet();
		ByteBuffer frame = ByteBuffer.allocateDirect(frameCapacity);
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

    protected double before = 0;
    public void heartBeat(double now) {
/*
        if (now > (before + 5))
        {
            before = now;
            Log.d("SeaCat", "FramePool stats / size:"+size()+", capacity:"+capacity());
        }
*/
    }
}
