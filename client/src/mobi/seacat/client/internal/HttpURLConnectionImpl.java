package mobi.seacat.client.internal;

//TODO: Raise exception when setFixedLengthStreamingMode is set and more data are written to OutputStream
//TODO: Follow http://developer.android.com/reference/java/net/HttpURLConnection.html chapters to implement everything nicely
//TODO: Implement ChunkedOutputStream and ContentLengthOutputStream for more user friendly behaviour

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.TreeMap;
import java.util.List;
import java.util.Map;

import mobi.seacat.client.SeaCatClient;
import mobi.seacat.client.SeaCatIOException;
import mobi.seacat.client.okhttp.Headers;

public final class HttpURLConnectionImpl extends HttpURLConnection
{
	private OutboundState outboundState = OutboundState.INIT;
	private Reactor reactor = null;
	private HttpInputStream inboundStream = null;
	private HttpOutputStream outboundStream = null;
	private Headers responseHeaders = null;
	
	public HttpURLConnectionImpl(URL url)
	{
		super(url);
	}

	///
	
	@Override
	public void connect() throws IOException
	{
		if (reactor != null) return;
		reactor = SeaCatClient.connect();
		outboundState = OutboundState.CONNECTED;
	}

	@Override
	public void disconnect()
	{
		try
		{
			SeaCatClient.disconnect();
		}
		catch (IOException e) {}
	}


	/*
	 * Return a response body
	 * This is the place where request is usually executed
	 */
	@Override
	public final InputStream getInputStream() throws IOException
	{
		advance(OutboundState.FLAG_FIN_SENT);
		return inboundStream;
	}


	@Override
	public OutputStream getOutputStream() throws IOException
	{

		if (outboundStream == null)
		{
			if (outboundState.getCode() > OutboundState.CONNECTED.getCode()) throw new IOException("Too late to open OutputStream"); // TODO: Change that into some standard exception (check what java implementation raises here)
			advance(OutboundState.CONNECTED);
			outboundStream = new HttpOutputStream(this, reactor);
		}
		return outboundStream;
	}

	
	//TODO: public Object getContent() throws IOException
	//TODO: public Object getContent(Class[] types) throws IOException


	public void advance(OutboundState targetState) throws IOException
	{
		int counter = 0;
		while (targetState.getCode() > outboundState.getCode())
		{
			counter += 1;
			if (counter > 1000) throw new IOException("Stalled outbound state");

			switch (outboundState)
			{
				case INIT: // to CONNECTED
					// Make sure we are connected
					connect();
					break;

				case CONNECTED: // to SYN_STREAM_SENT or directly to FLAG_FIN_SENT
					sendSYN_STREAM();
					break;

				case SYN_STREAM_SENT: // to FLAG_FIN_SENT
					// This part is called only if there is outboundStream registered
					// If null exception is raised here, issue in CONNECTED step before happen
					outboundStream.close();
					break;

				case FLAG_FIN_SENT:
					throw new IOException("Incorrect state change requested");
			}
		}
	}

	public void setFIN_FLAG_SENT() throws IOException
	{
		if (outboundState.getCode() < OutboundState.SYN_STREAM_SENT.getCode()) throw new IOException("Cannot switch to FIN_FLAG prior SYN_STREAM");
		outboundState = OutboundState.FLAG_FIN_SENT;
	}
	
	private void sendSYN_STREAM() throws IOException
	{	
		if (fixedContentLength > 0) addRequestProperty("Content-length", String.format("%d", fixedContentLength));
		
		boolean flag_fin = (outboundStream == null);
		if (outboundState.getCode() > OutboundState.CONNECTED.getCode()) throw new IOException("Too late to open InputStream"); // TODO: Change that into some standard exception (check what java implementation raises here)
		inboundStream = reactor.sendSYN_STREAM(this, flag_fin, 0);
		if (outboundStream != null) outboundStream.setStreamId(inboundStream.getStreamId());
		outboundState = flag_fin ? OutboundState.FLAG_FIN_SENT : OutboundState.SYN_STREAM_SENT;
	}
	
	public void inboundSynReply(ByteBuffer frame)
	{
		// Status
		responseCode = frame.getShort();
		responseMessage = HttpStatus.getMessage(responseCode);

		// Reserved (unused) 16 bits
		frame.getShort(); 

		// Parse response headers
		Headers.Builder headerBuilder = new Headers.Builder();
		headerBuilder.add(null, "HTTP/1.1 " + responseCode + " " + responseMessage); // To mimic HttpURLConnection behaviour
		for(;frame.position() < frame.limit();)
		{
			String k = SPDY.parseVLEString(frame);
			String v = SPDY.parseVLEString(frame);
			headerBuilder.add(k, v);
		}
		responseHeaders = headerBuilder.build();
		
	}

	///

	@Override
	public String getHeaderField(int position)
	{
		if (responseHeaders == null) return null;
		return responseHeaders.value(position);
	}

	@Override
	public String getHeaderFieldKey(int position)
	{
		if (responseHeaders == null) return null;
		return responseHeaders.name(position);
	}

	@Override
	public String getHeaderField(String name)
	{
		if (responseHeaders == null) return null;
		return responseHeaders.get(name);
	}

	@Override
	public Map<String, List<String>> getHeaderFields()
	{
		if (responseHeaders == null) return null;

		TreeMap<String, List<String>> m = new TreeMap<String, List<String>>(Headers.FIELD_NAME_COMPARATOR);
		for(String name : responseHeaders.names())
			m.put(name, responseHeaders.values(name));
		return m;
	}

	///

	@Override
	public boolean usingProxy()
	{
		return true;
	}
	
	///

	public enum OutboundState
	{

		INIT(1),
		CONNECTED(2),
		SYN_STREAM_SENT(3),
		FLAG_FIN_SENT(4);
		
		private final int code;

		private OutboundState(int code)
		{
			this.code=code;
		}

		public int getCode()
		{
			return code;
		}
	}

}
