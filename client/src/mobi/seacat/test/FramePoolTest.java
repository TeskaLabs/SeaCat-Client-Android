package mobi.seacat.test;

import static org.junit.Assert.*;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;

import mobi.seacat.client.core.FramePool;

public class FramePoolTest
{

	@Test
	public void testNew()
	{
		new FramePool();
	}

	
	@Test
	public void testBorrowEmpty() throws Exception
	{
		FramePool pool = new FramePool();

		ByteBuffer buffer = pool.borrow("testBorrowEmpty");
		assertNotNull(buffer);
		assertEquals(16384, buffer.capacity());
		assertEquals(0, buffer.position());
		
		assertEquals(1, pool.capacity());
		assertEquals(0, pool.size());
	}

	
	@Test(expected=IOException.class)
	public void testBorrowHighWaterMarkReached() throws Exception
	{
		FramePool pool = new FramePool();
		
		for (int i=0; i<1025; i++)
		{
			ByteBuffer buffer = pool.borrow("testBorrowHighWaterMarkReached");
			assertNotNull(buffer);
			assertEquals(16384, buffer.capacity());
			assertEquals(0, buffer.position());
		}
	}

	
	@Test
	public void testBorrowGiveBack() throws Exception
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
