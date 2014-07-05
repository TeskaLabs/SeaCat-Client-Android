package mobi.seacat.client.android.test;

import android.test.AndroidTestCase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import mobi.seacat.client.SeaCatClient;

public class SeaCatClientTest extends AndroidTestCase
{


	public void testConnect() throws IOException
	{
		SeaCatClient.connect();
	}

	
	public void testDoubleConnect() throws IOException
	{
		SeaCatClient.connect();
		SeaCatClient.connect();
	}

	
	public void testDisconnect() throws IOException
	{
		SeaCatClient.disconnect();
	}

	
	public void testDoubleDisconnect() throws IOException
	{
		SeaCatClient.disconnect();
		SeaCatClient.disconnect();
	}


	public void testPing() throws Exception
	{
		SeaCatClient.ping(1000);
	}


	public void testGETHelloWorld() throws IOException
	{
		URL url = new URL("https://testhost.seacat/get/hello_world");
		HttpURLConnection conn = SeaCatClient.open(url);

		InputStream is = conn.getInputStream();
		assertNotNull(is);

		String line;
		String result = "";
	
		BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		while ((line = rd.readLine()) != null)
		{
			result += line;
		}
		rd.close();

		assertEquals(HttpURLConnection.HTTP_OK, conn.getResponseCode());
		assertEquals("Hello World!", result);
		
		is.close();
	}

}
