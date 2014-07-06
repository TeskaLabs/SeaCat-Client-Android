package mobi.seacat.client.android.test;

import android.test.AndroidTestCase;
import java.nio.ByteBuffer;

import mobi.seacat.client.internal.FramePool;

public class FramePoolTest extends AndroidTestCase
{

	public void testNew()
	{
		new FramePool();
	}

	
	public void testBorrowEmpty() throws FramePool.HighWaterMarkReachedException
	{
		FramePool pool = new FramePool();

		ByteBuffer buffer = pool.borrow("testBorrowEmpty");
		assertNotNull(buffer);
		assertEquals(16384, buffer.capacity());
		assertEquals(0, buffer.position());
		
		assertEquals(1, pool.capacity());
		assertEquals(0, pool.size());
	}

	
	public void testBorrowHighWaterMarkReached() throws FramePool.HighWaterMarkReachedException
	{
		FramePool pool = new FramePool();
		
		try
		{
			for (int i=0; i<1025; i++)
			{
				ByteBuffer buffer = pool.borrow("testBorrowHighWaterMarkReached");
				assertNotNull(buffer);
				assertEquals(16384, buffer.capacity());
				assertEquals(0, buffer.position());
			}
			fail();
		}
		catch (FramePool.HighWaterMarkReachedException e)
		{
			// OK
		}
	}

	
	public void testBorrowGiveBack() throws FramePool.HighWaterMarkReachedException
	{
		FramePool pool = new FramePool();
		
		for (int i=0; i<5000; i++)
		{
			ByteBuffer buffer = pool.borrow("testBorrowGiveBack");
			assertNotNull(buffer);
			assertEquals(16384, buffer.capacity());
			assertEquals(0, buffer.position());
			buffer.position(i);
			pool.giveBack(buffer);
		}

		assertEquals(1, pool.capacity());
		assertEquals(1, pool.size());
	}

}
