package mobi.seacat.client.android.test;

import android.test.AndroidTestCase;

import java.net.FileNameMap;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import mobi.seacat.client.SeaCatClient;
import mobi.seacat.client.okhttp.HttpDate;

import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpURLConnectionImplTest extends AndroidTestCase
{
	URL url;

	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		this.url = new URL("https://test.seacat/resource.json");
	}


	public void test_RequestMethod_SetGet() throws Exception
	{
		HttpURLConnection conn = SeaCatClient.open(url);
		conn.setRequestMethod("OPTIONS");
		assertEquals(conn.getRequestMethod(), "OPTIONS");
	}


	public void test_getURL() throws Exception
	{
		HttpURLConnection conn = SeaCatClient.open(url);
		assertEquals(conn.getURL(), this.url);
	}


	public void test_toString()
	{
		HttpURLConnection conn = SeaCatClient.open(url);
		assertNotNull(conn.toString());
	}


	public void test_usingProxy() throws Exception
	{
		HttpURLConnection conn = SeaCatClient.open(url);
		assertEquals(conn.usingProxy(), true);
	}


	public void test_requestProperties() throws Exception
	{
		HttpURLConnection conn = SeaCatClient.open(url);

		conn.addRequestProperty("X-Test-1", "foo");
		conn.addRequestProperty("X-Test-1", "bar");
		
		conn.setRequestProperty("X-Test-2", "foo");
		conn.setRequestProperty("X-Test-2", "bar");
		
		assertEquals("bar", conn.getRequestProperty("X-Test-1"));
		assertEquals("bar", conn.getRequestProperty("X-Test-2"));
		assertNull(conn.getRequestProperty("X-Test-3"));

		Map<String, List<String>> map = conn.getRequestProperties();
		
		Map<String, List<String>> expected = new HashMap<String, List<String>>();
		expected.put("X-Test-1", Arrays.asList("foo", "bar"));
		expected.put("X-Test-2", Arrays.asList("bar"));
		
		assertEquals(expected, map);
	}


	public void test_setRequestProperties_nullPropertyName() throws Exception
	{
		HttpURLConnection conn = SeaCatClient.open(url);
		try
		{
			conn.setRequestProperty(null, "foo");
			fail();
		}
		catch (NullPointerException e)
		{
			//OK
		}
	}

	
	public void test_setRequestProperties_nullPropertyValue() throws Exception
	{
		HttpURLConnection conn = SeaCatClient.open(url);
		conn.setRequestProperty("X-Test-NULLValue", "foo");
		conn.setRequestProperty("X-Test-NULLValue", null);
		assertEquals("foo",conn.getRequestProperty("X-Test-NULLValue"));
	}


	public void test_addRequestProperties_nullPropertyName() throws Exception
	{
		HttpURLConnection conn = SeaCatClient.open(url);
		try
		{
			conn.addRequestProperty(null, "foo");
			fail();
		}
		catch (NullPointerException e)
		{
			//OK
		}
	}

	
	public void test_addRequestProperties_nullPropertyValue() throws Exception
	{
		HttpURLConnection conn = SeaCatClient.open(url);
		conn.setRequestProperty("X-Test-NULLValue", "foo");
		conn.addRequestProperty("X-Test-NULLValue", null);
		assertEquals("foo",conn.getRequestProperty("X-Test-NULLValue"));
	}

	
	public void test_useCaches() throws Exception
	{
		HttpURLConnection conn = SeaCatClient.open(url);

		conn.setUseCaches(true);
		assertTrue(conn.getUseCaches());

		conn.setUseCaches(false);
		assertFalse(conn.getUseCaches());
	}


	public void test_defaultUseCaches() throws Exception
	{
		HttpURLConnection conn = SeaCatClient.open(url);

		conn.setDefaultUseCaches(true);
		assertTrue(conn.getDefaultUseCaches());

		conn.setDefaultUseCaches(false);
		assertFalse(conn.getDefaultUseCaches());
	}


	public void test_allowUserInteraction() throws Exception
	{
		HttpURLConnection conn = SeaCatClient.open(url);
		
		conn.setAllowUserInteraction(true);
		assertTrue(conn.getAllowUserInteraction());

		conn.setAllowUserInteraction(false);
		assertFalse(conn.getAllowUserInteraction());
	}

	
	public void test_connectTimeout() throws Exception
	{
		HttpURLConnection conn = SeaCatClient.open(url);

		conn.setConnectTimeout(5);
		assertEquals(5, conn.getConnectTimeout());
	}


	public void test_readTimeout() throws Exception
	{
		HttpURLConnection conn = SeaCatClient.open(url);

		conn.setReadTimeout(5);
		assertEquals(5, conn.getReadTimeout());
	}

	
	public void test_instanceFollowRedirects() throws Exception
	{
		HttpURLConnection conn = SeaCatClient.open(url);

		conn.setInstanceFollowRedirects(true);
		assertTrue(conn.getInstanceFollowRedirects());

		conn.setInstanceFollowRedirects(false);
		assertFalse(conn.getInstanceFollowRedirects());
	}


	public void test_followRedirects() throws Exception
	{
		mobi.seacat.client.internal.HttpURLConnectionImpl.setFollowRedirects(true);
		assertTrue(mobi.seacat.client.internal.HttpURLConnectionImpl.getFollowRedirects());

		mobi.seacat.client.internal.HttpURLConnectionImpl.setFollowRedirects(false);
		assertFalse(mobi.seacat.client.internal.HttpURLConnectionImpl.getFollowRedirects());
	}


	public void test_fixedLengthStreamingMode() throws Exception
	{
		HttpURLConnection conn = SeaCatClient.open(url);

		conn.setFixedLengthStreamingMode(1024);
	}


	public void test_chunkedStreamingMode() throws Exception
	{
		HttpURLConnection conn = SeaCatClient.open(url);

		conn.setChunkedStreamingMode(512);
	}


	public void test_doOutput() throws Exception
	{
		HttpURLConnection conn = SeaCatClient.open(url);

		conn.setDoOutput(true);
		assertTrue(conn.getDoOutput());

		conn.setDoOutput(false);
		assertFalse(conn.getDoOutput());
	}


	public void test_doInput() throws Exception
	{
		HttpURLConnection conn = SeaCatClient.open(url);

		conn.setDoInput(true);
		assertTrue(conn.getDoInput());

		conn.setDoInput(false);
		assertFalse(conn.getDoInput());
	}

	
	public void test_getInputStream() throws Exception
	{
		HttpURLConnection conn = SeaCatClient.open(url);
		InputStream is = conn.getInputStream();

		assertNotNull(is);
		is.close();
	}


	public void test_connect() throws Exception
	{
		HttpURLConnection conn = SeaCatClient.open(url);
		conn.connect();
	}


	public void test_connectDouble() throws Exception
	{
		HttpURLConnection conn = SeaCatClient.open(url);
		conn.connect();
		conn.connect();
	}

	
	public void test_disconnect() throws Exception
	{
		HttpURLConnection conn = SeaCatClient.open(url);
		conn.disconnect();
	}

	
	public void test_disconnectDouble() throws Exception
	{
		HttpURLConnection conn = SeaCatClient.open(url);
		conn.disconnect();
		conn.disconnect();
	}

	
	public void test_getHeaderFieldByPosition() throws Exception
	{
		HttpURLConnection conn = SeaCatClient.open(url);
		String value;
		
		value = conn.getHeaderField(0);
		assertNull(value);
		value = conn.getHeaderField(1);
		assertNull(value);

		InputStream is = conn.getInputStream();
		is.read();

		value = conn.getHeaderField(0);
		assertEquals("HTTP/1.1 404 Not Found", value);

		value = conn.getHeaderField(1);
		assertNotNull(value);
	}

	
	public void test_getHeaderFieldKeyByPosition() throws Exception
	{
		HttpURLConnection conn = SeaCatClient.open(url);
		String value;
		
		value = conn.getHeaderFieldKey(0);
		assertNull(value);
		value = conn.getHeaderFieldKey(1);
		assertNull(value);

		InputStream is = conn.getInputStream();
		is.read();

		value = conn.getHeaderFieldKey(0);
		assertNull(value); // this is Status line

		value = conn.getHeaderFieldKey(1);
		assertNotNull(value);
	}


	public void test_getHeaderField() throws Exception
	{
		HttpURLConnection conn = SeaCatClient.open(url);
		String value;
		
		value = conn.getHeaderField(null);
		assertNull(value);
		value = conn.getHeaderField("content-type");
		assertNull(value);

		InputStream is = conn.getInputStream();
		is.read();

		value = conn.getHeaderField(null);
		assertEquals("HTTP/1.1 404 Not Found", value);

		value = conn.getHeaderField("content-type");
		assertEquals("text/html", value);
	}


	public void test_getHeaderFields() throws Exception
	{
		HttpURLConnection conn = SeaCatClient.open(url);
		Map<String, List<String>> m;
		
		m = conn.getHeaderFields();
		assertNull(m);

		InputStream is = conn.getInputStream();
		is.read();

		m = conn.getHeaderFields();
		ArrayList<String> e = new ArrayList<String>(1);
		e.add("HTTP/1.1 404 Not Found");
		assertEquals(e, m.get(null)); 
		assertTrue(m.size() > 2);
	}

	
	public void test_getHeaderFieldInt() throws Exception
	{
		HttpURLConnection conn = SeaCatClient.open(url);
		int value;
		
		value = conn.getHeaderFieldInt("content-length", -1);
		assertEquals((long)-1, (long)value);

		InputStream is = conn.getInputStream();
		is.read();

		value = conn.getHeaderFieldInt("content-length", -1);
		assertTrue(value > 0);
	}

	
	public void test_getHeaderFieldDate() throws Exception
	{
		HttpURLConnection conn = SeaCatClient.open(url);
		long value;
		
		value = conn.getHeaderFieldDate("date", -1);
		assertEquals((long)-1, value);

		InputStream is = conn.getInputStream();
		is.read();

		value = conn.getHeaderFieldDate("date", -1);
		assertTrue(value > 0);
	}

	
	public void test_getContentEncoding() throws Exception
	{
		HttpURLConnection conn = SeaCatClient.open(url);
		String enc = conn.getContentEncoding();
		assertNull(enc);
		InputStream is = conn.getInputStream();
		is.read();
		enc = conn.getContentEncoding();
		assertNull(enc); // Yeah, that is correct and documented behaviour  - see http://stackoverflow.com/questions/3934251/urlconnection-does-not-get-the-charset
	}


	public void test_getResponseCode() throws Exception
	{
		HttpURLConnection conn = SeaCatClient.open(url);
		long rc;
		InputStream is = conn.getInputStream();
		is.read();
		rc= conn.getResponseCode();
		assertEquals(404, rc);
	}

	
	public void test_getResponseMessage() throws Exception
	{
		HttpURLConnection conn = SeaCatClient.open(url);
		String rm;
		InputStream is = conn.getInputStream();
		is.read();
		rm = conn.getResponseMessage();
		assertEquals("Not Found", rm);
	}

	
	public void test_getExpiration() throws Exception
	{
		HttpURLConnection conn = SeaCatClient.open(url);
		long v;
		v = conn.getExpiration();
		assertEquals(0, v);
		InputStream is = conn.getInputStream();
		is.read();
		v = conn.getExpiration();
		assertEquals(0, v);
		//TODO: Test on a real value
	}

	
	public void test_getDate() throws Exception
	{
		HttpURLConnection conn = SeaCatClient.open(url);
		long v;
		v = conn.getDate();
		assertEquals(0, v);
		InputStream is = conn.getInputStream();
		is.read();
		v = conn.getDate();
		assertTrue(v > 0);
	}


	public void test_getLastModified() throws Exception
	{
		HttpURLConnection conn = SeaCatClient.open(url);
		long v;
		v = conn.getLastModified();
		assertEquals(0, v);
		InputStream is = conn.getInputStream();
		is.read();
		v = conn.getLastModified();
		assertEquals(0, v);
		//TODO: Test on a real value
	}

	
	public void test_getContentLength() throws Exception
	{
		HttpURLConnection conn = SeaCatClient.open(url);
		long v;
		v = conn.getContentLength();
		assertEquals(-1, v);
		InputStream is = conn.getInputStream();
		is.read();
		v = conn.getContentLength();
		assertTrue(v > 0);
	}

	
	public void test_getContentType() throws Exception
	{
		HttpURLConnection conn = SeaCatClient.open(url);
		String ct;
		ct = conn.getContentType();
		assertNull(ct);
		InputStream is = conn.getInputStream();
		is.read();
		ct = conn.getContentType();
		assertEquals("text/html", ct);
	}

	
	public void test_requestProperty() throws Exception
	{
		this.url = new URL("https://testhost.seacat/get/xheaders");
		HttpURLConnection conn = SeaCatClient.open(url);

		conn.addRequestProperty("X-Headers-1", "foo");
		conn.addRequestProperty("X-Headers-1", "bar");
		
		conn.setRequestProperty("X-Headers-2", "foo");
		conn.setRequestProperty("X-Headers-2", "bar");
		conn.setRequestProperty("X-Headers-3", "bar");
		
		BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String line = rd.readLine();

		assertEquals("3", line);
	}

	
	public void test_FileNameMap()
	{
		FileNameMap fnm = mobi.seacat.client.internal.HttpURLConnectionImpl.getFileNameMap();
		assertNotNull(fnm);
		mobi.seacat.client.internal.HttpURLConnectionImpl.setFileNameMap(fnm);
	}

	
	public void test_getPermission() throws IOException
	{
		HttpURLConnection conn = SeaCatClient.open(url);
		Permission p = conn.getPermission();
		assertNotNull(p);
	}

	
	public void test_ifModifiedSince() throws Exception
	{
		this.url = new URL("https://testhost.seacat/get/ifmodifiedsince");
		HttpURLConnection conn = SeaCatClient.open(url);

		conn.setIfModifiedSince(10000);
		long v = conn.getIfModifiedSince();
		assertEquals(10000, v);
		
		BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String line = rd.readLine();

		Date d = HttpDate.parse(line);
		assertEquals(10000, d.getTime());
	}


}
