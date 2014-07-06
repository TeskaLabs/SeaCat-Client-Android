package mobi.seacat.client.android.test;

import android.test.AndroidTestCase;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import mobi.seacat.client.SeaCatClient;

///

public class HttpInputStreamTest extends AndroidTestCase
{

	URL url;
	HttpURLConnection conn;
	InputStream is;

	@Override
	protected void setUp() throws Exception
	{
		super.setUp();

		this.url = new URL("https://testhost.seacat/get/hello_world");
		this.conn = SeaCatClient.open(url);
		
		is = this.conn.getInputStream();
		assertNotNull(is);
	}


	@Override
	protected void tearDown() throws Exception
	{
		super.tearDown();

		is = null;
		conn = null;
		SeaCatClient.disconnect();

		for (int i=0; i<100; i++)
		{
			System.gc();
			if (SeaCatClient.getFramePoolSize() == SeaCatClient.getFramePoolCapacity()) break;
		}
		
		assertTrue(SeaCatClient.getFramePoolSize() == SeaCatClient.getFramePoolCapacity());
	}


	public void testRead() throws IOException
	{
		for (char ch: "Hello World!".toCharArray())
		{
			int r = is.read();
			assertEquals(ch, r);
		}

		int r = is.read();
		assertEquals(-1, r);

		r = is.read();
		assertEquals(-1, r);
		
		r = is.read();
		assertEquals(-1, r);
	}


	public void testReadByteArrayIntInt() throws IOException
	{
		byte[] result = new byte[1024];
		java.util.Arrays.fill(result, (byte)77);

		int ret = is.read(result, 10, result.length-10);
		assertEquals(12, ret);

		for (int i=0;i<10;i++) assertEquals(result[i], 77);
		for (int i=22;i<result.length;i++) assertEquals(result[i], 77);

		int i = 0;
		for (char ch: "Hello World!".toCharArray())
		{
			assertEquals(ch, result[10+i]);
			i++;
		}

		ret = is.read(result, 10, result.length-10);
		assertEquals(-1, ret);

		ret = is.read(result, 10, result.length-10);
		assertEquals(-1, ret);

		ret = is.read(result, 10, result.length-10);
		assertEquals(-1, ret);
	}


	public void testReadByteArrayIntInt_IndexOutOfBoundsException() throws IOException
	{
		byte[] result = new byte[1024];

		try
		{
			is.read(result, 10, result.length);
			fail();
		}
		
		catch (IndexOutOfBoundsException e)
		{
			//OK
		}
	}
	

	public void testReadByteArray() throws IOException
	{
		byte[] result = new byte[1024];
		java.util.Arrays.fill(result, (byte)77);

		int ret = is.read(result);
		assertEquals(12, ret);

		for (int i=12;i<result.length;i++) assertEquals(result[i], 77);

		int i = 0;
		for (char ch: "Hello World!".toCharArray())
		{
			assertEquals(ch, result[i]);
			i++;
		}
	}


	public void testReadByteArray_SmallBuffer() throws IOException
	{
		byte[] result = new byte[5];

		int ret = is.read(result);
		assertEquals(result.length, ret);
		int i = 0;
		for (char ch: "Hello".toCharArray())
		{
			assertEquals(ch, result[i]);
			i++;
		}

		ret = is.read(result);
		assertEquals(result.length, ret);
		i = 0;
		for (char ch: " Worl".toCharArray())
		{
			assertEquals(ch, result[i]);
			i++;
		}

		ret = is.read(result);
		assertEquals(2, ret);
		i = 0;
		for (char ch: "d!".toCharArray())
		{
			assertEquals(ch, result[i]);
			i++;
		}

		ret = is.read(result);
		assertEquals(-1, ret);

		ret = is.read(result);
		assertEquals(-1, ret);

		ret = is.read(result);
		assertEquals(-1, ret);
	}


	public void testClose() throws IOException
	{
		int r = is.read();
		assertEquals('H', r);

		is.close();
		
		r = is.read();
		assertEquals(-1, r);
		
		r = is.read();
		assertEquals(-1, r);
		
		r = is.read();
		assertEquals(-1, r);
	}


	public void testCloseDouble() throws IOException
	{
		int r = is.read();
		assertEquals('H', r);

		is.close();
		
		r = is.read();
		assertEquals(-1, r);
		
		is.close();
		
		r = is.read();
		assertEquals(-1, r);
		
		is.close();
		is.close();

		r = is.read();
		assertEquals(-1, r);
	}


	public void testSkip() throws IOException
	{
		int r = is.read();
		assertEquals('H', r);

		long r1 = is.skip(5);
		assertEquals(5, r1);

		r = is.read();
		assertEquals('W', r);
	}

	
	public void testAvailable() throws IOException
	{
		int r = is.available();
		assertEquals(0, r); //TODO: Can be improved to return 12
	}


	public void testMarkSupported()
	{
		assertFalse(is.markSupported());
	}

	
	public void testMark()
	{
		is.mark(5);
	}


	public void testReset() throws IOException
	{
		try
		{
			is.reset();
			fail();
		}
		
		catch (IOException e)
		{
			// OK
		}
	}


}
