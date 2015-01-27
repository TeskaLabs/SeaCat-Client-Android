package mobi.seacat.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

import mobi.seacat.client.SeaCatClient;

public class Main implements mobi.seacat.client.intf.IDelegate
{
	
	@Override
	public void pong(int pingId)
	{
		System.out.println("PONG: "+ pingId);
	}

	
	private void foreverPing() throws IOException
	{
		while (true)
		{
			SeaCatClient.ping();
			
			try {
			    Thread.sleep(3000);
			} catch(InterruptedException ex) {
			    Thread.currentThread().interrupt();
			}
		}
	}

	///

	private void simpleGet() throws IOException, IOException
	{

		URL url = new URL("https://testhost.seacat/get/hello_world");
		HttpURLConnection conn = SeaCatClient.open(url);
	
		InputStream is = conn.getInputStream();
		assert(is != null);
	
	
		String line;
		String result = "";
		BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		while ((line = rd.readLine()) != null)
		{
			result += line;
		}
		rd.close();
		is.close();
	
		System.out.println(String.format("Received %d: ", conn.getResponseCode()) + result);
	}

	///

	private void simplePost() throws IOException
	{
		String urlParameters  = "param1=a&param2=b&param3=c";
		byte[] postData       = urlParameters.getBytes( Charset.forName( "UTF-8" ));
		int    postDataLength = postData.length;
		
		URL url = new URL("https://testhost.seacat/post/replay");
		HttpURLConnection conn = SeaCatClient.open(url);
	
		conn.setDoOutput( true );
		conn.setDoInput ( true );
		conn.setInstanceFollowRedirects( false );
		conn.setRequestMethod( "POST" );
		conn.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded"); 
		conn.setRequestProperty( "charset", "utf-8");
		conn.setRequestProperty( "Content-Length", Integer.toString( postDataLength ));
		conn.setUseCaches( false );

		OutputStream os = conn.getOutputStream();
		os.write(postData);
		os.close();

		InputStream is = conn.getInputStream();
		assert(is != null);
	
		String line;
		String result = "";
		BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		while ((line = rd.readLine()) != null)
		{
			result += line;
		}
		
		rd.close();
		is.close();
	
		System.out.println(String.format("Received %d: ", conn.getResponseCode()) + result);
	}

	///
	
	public void run() throws IOException, InterruptedException
	{
		SeaCatClient.configure(this);

		while (true)
		{
			SeaCatClient.ping();
			simplePost();
			SeaCatClient.ping();
			simpleGet();
//			Thread.sleep(10);
		}
		//foreverPing();
	}


	public static void main(String[] args) throws Exception
	{
		Main m = new Main();
		m.run();
	}
}
