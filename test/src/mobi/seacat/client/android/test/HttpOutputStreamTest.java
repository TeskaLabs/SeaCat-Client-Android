package mobi.seacat.client.android.test;

import android.test.AndroidTestCase;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import mobi.seacat.client.SeaCatClient;

public class HttpOutputStreamTest extends AndroidTestCase
{
	
	public void testReplay() throws IOException
	{
		URL url = new URL("https://testhost.seacat/put/replay");
		HttpURLConnection conn = SeaCatClient.open(url);

		conn.setRequestMethod("PUT");
		conn.setDoInput(true);
		conn.setDoOutput(true);
		conn.setFixedLengthStreamingMode(2);
		
		OutputStream os = conn.getOutputStream();
		assertNotNull(os);

		os.write(77);
		os.write(78);

		InputStream is = conn.getInputStream();
		assertNotNull(is);

		byte[] result = new byte[1024];
		int i = is.read(result);

		assertEquals(2, i);
		assertEquals(77, result[0]);
		assertEquals(78, result[1]);
	}


	public void testLateOpen() throws IOException
	{
		URL url = new URL("https://testhost.seacat/get/hello_world");
		HttpURLConnection conn = SeaCatClient.open(url);

		conn.setRequestMethod("POST");
		conn.setDoInput(true);
		conn.setDoOutput(true);

		InputStream is = conn.getInputStream();
		assertNotNull(is);
		
		try
		{
			conn.getOutputStream();
		}
		
		catch (IOException e)
		{
			return;
		}

		fail();
	}


	public void testMultipleClose() throws IOException
	{
		URL url = new URL("https://testhost.seacat/put/replay");
		HttpURLConnection conn = SeaCatClient.open(url);

		conn.setRequestMethod("PUT");
		conn.setDoInput(true);
		conn.setDoOutput(true);
		conn.setFixedLengthStreamingMode(2);
		
		OutputStream os = conn.getOutputStream();
		assertNotNull(os);

		os.write(77);
		os.write(78);

		os.close();
		os.close();
		os.close();

		InputStream is = conn.getInputStream();
		assertNotNull(is);

	}

	//TODO: Test, when getOutputStream() is called but no output is written (empty DATA frame just with FIN_FLAG needs to be sent)


}
