package mobi.seacat.client.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import mobi.seacat.client.core.Reactor;
import mobi.seacat.client.core.SPDY;
import mobi.seacat.client.intf.*;

public class URLConnection extends HttpURLConnection implements IFrameProvider , IStream
{
	protected final Reactor reactor;

	protected final InboundStream inboundStream;	
	protected OutboundStream outboundStream = null;

	private Headers responseHeaders = null;
	private final Headers.Builder requestHeaders = new Headers.Builder();

	private int streamId = -1;
	private int priority;
	private Stage stage;

	///

	public enum Stage
	{

		INITIAL(1),
		HEADERS_READY(2),
		SENDING_BODY(3),
		FIN_SENT(4);
		
		private final int code;

		private Stage(int code)
		{
			this.code=code;
		}

		public int getCode()
		{
			return code;
		}
	}
	///
	
	public URLConnection(Reactor reactor, URL u, int priority)
	{
		super(u);
		this.reactor = reactor;
		this.priority = priority;
		this.stage = Stage.INITIAL;

		this.inboundStream = new InboundStream(this);
	}

	
	protected final void advance(Stage toStage)
	{
		if (this.stage.getCode() >= toStage.getCode()) return;
		
		else if ((this.stage == Stage.INITIAL) && (toStage == Stage.HEADERS_READY))
		{
			this.stage = toStage;
			try {
				reactor.registerFrameProvider(this, true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		else if ((this.stage == Stage.HEADERS_READY) && (toStage == Stage.FIN_SENT))
		{
			this.stage = toStage;
		}

		else if ((this.stage == Stage.HEADERS_READY) && (toStage == Stage.SENDING_BODY))
		{
			this.stage = toStage;
		}

		else System.err.println("Incorrect stage advance " + this.stage + " -> "+ toStage);
	}
	
	///

	/*
	 * Return a response body
	 * This is the place where request is usually executed
	 */
	@Override
	public final InputStream getInputStream() throws IOException
	{
		advance(Stage.HEADERS_READY);
		return this.inboundStream;
	}

	
	@Override
	synchronized public OutputStream getOutputStream() throws IOException
	{
        if (!doOutput) {
            throw new ProtocolException("cannot write to a URLConnection" + " if doOutput=false - call setDoOutput(true)");
        }
        if (method.equals("GET")) {
            method = "POST"; // Backward compatibility
        }

		if (outboundStream == null)
		{
			if (stage.code >= Stage.HEADERS_READY.code) throw new ProtocolException("Cannot write output after reading input.");
			outboundStream = new OutboundStream(this);
		}
		return outboundStream;	
	}

	//TODO: public Object getContent() throws IOException
	//TODO: public Object getContent(Class[] types) throws IOException

	
	///

	@Override
	synchronized public Result buildFrame(Reactor reactor) throws IOException
	{
		boolean keep = false;
		ByteBuffer frame = null;

		assert(this.reactor == reactor);
		
		switch (this.stage)
		{
			case INITIAL:
				System.out.println("buildFrame - INITIAL (not good)");
				break;


			case HEADERS_READY:
			{
				frame = buildSYN_STREAM();
				if (outboundStream != null)
				{
					assert((frame.getShort(4) & SPDY.FLAG_FIN) == 0);
					keep = !outboundStream.isQueueEmpty();
					advance(Stage.SENDING_BODY);
				}
				else
				{
					assert((frame.getShort(4) & SPDY.FLAG_FIN) == SPDY.FLAG_FIN);
					advance(Stage.FIN_SENT);
				}
				return new IFrameProvider.Result(frame, keep);
			}


			case SENDING_BODY:
			{
				assert(streamId > 0);
				assert(outboundStream != null);

				frame = outboundStream.pollFrame();
				if (frame != null)
				{
					frame.putInt(0, streamId);
					keep = !outboundStream.isQueueEmpty();				
					if ((frame.getShort(4) & SPDY.FLAG_FIN) == SPDY.FLAG_FIN)
					{
						assert(keep == false);
						advance(Stage.FIN_SENT);
					}
				}
				return new IFrameProvider.Result(frame, keep);
			}

			default:
				System.err.println("Nothing to build - incorrect stage: " + this.stage);
		}
		
		return new IFrameProvider.Result(null, false);
	}

	
	private ByteBuffer buildSYN_STREAM() throws IOException
	{	
		if (fixedContentLength > 0) addRequestProperty("Content-length", String.format("%d", fixedContentLength));

		//TODO: addRequestProperty("X-Seacat-Client", isAndroid ? "and" : "jav");
		addRequestProperty("X-Seacat-Client", "and");

		// Add If-Modified-Since header
		long ifModifiedSince = getIfModifiedSince();
		if (ifModifiedSince != 0) addRequestProperty("If-Modified-Since", HttpDate.format(new Date(ifModifiedSince)));
		
		boolean fin_flag = (outboundStream == null);

		ByteBuffer frame = reactor.framePool.borrow("URLConnection.buildSYN_STREAM");

		streamId = reactor.streamFactory.registerStream(this);
	
		// Build SYN_STREAM frame
		SPDY.buildALX1SynStream(frame, streamId, getURL(), getRequestMethod(), getRequestHeaders(), fin_flag, this.priority);
	
		return frame;
	}

	
	@Override
	public int getFrameProviderPriority()
	{
		return this.priority;
	}

	///
	
	@Override
	public void reset()
	{
		outboundStream.reset();
		inboundStream.reset();
	}

	
	@Override
	synchronized public boolean receivedALX1_SYN_REPLY(Reactor reactor, ByteBuffer frame, int frameLength, byte frameFlags)
	{
		//TODO: Check stage - should disregards frames that come prior proper state

		// Status
		responseCode = frame.getShort();
		responseMessage = HttpStatus.getMessage(responseCode);

//		System.out.println(String.format("ALX1_SYN_REPLY: %d %s %s", responseCode, responseMessage, ((frameFlags & SPDY.FLAG_FIN) == SPDY.FLAG_FIN) ? "FIN" : ""));
		
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

		if ((frameFlags & SPDY.FLAG_FIN) == SPDY.FLAG_FIN) inboundStream.close();
		return true;
	}

	@Override
	synchronized public boolean receivedSPD3_RST_STREAM(Reactor reactor, ByteBuffer frame, int frameLength, byte frameFlags)
	{
		inboundStream.reset();
		if (outboundStream != null)
		{
			try {
				outboundStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return true;		
	}

	@Override
	public boolean receivedDataFrame(Reactor reactor, ByteBuffer frame, int frameLength, byte frameFlags)
	{
		//TODO: Check stage - should disregards frames that come prior proper state
//		System.out.println(String.format("DATA %s", ((frameFlags & SPDY.FLAG_FIN) == SPDY.FLAG_FIN) ? "FIN" : ""));		
		
		boolean ret = inboundStream.inboundData(frame);
		if ((frameFlags & SPDY.FLAG_FIN) == SPDY.FLAG_FIN) inboundStream.close();
		return ret;		
	}

	/// Response headers

	@Override
	synchronized public String getHeaderField(int position)
	{
		if (responseHeaders == null) return null;
		return responseHeaders.value(position);
	}

	@Override
	synchronized public String getHeaderFieldKey(int position)
	{
		if (responseHeaders == null) return null;
		return responseHeaders.name(position);
	}

	@Override
	synchronized public String getHeaderField(String name)
	{
		if (responseHeaders == null) return null;
		return responseHeaders.get(name);
	}

	@Override
	synchronized public Map<String, List<String>> getHeaderFields()
	{
		if (responseHeaders == null) return null;

		TreeMap<String, List<String>> m = new TreeMap<String, List<String>>(Headers.FIELD_NAME_COMPARATOR);
		for(String name : responseHeaders.names())
			m.put(name, responseHeaders.values(name));
		return m;
	}

	/// Request Headers

	@Override
	synchronized public void setRequestProperty(String field, String newValue)
	{
		//TODO: Consider this: if (stage >= HEADER_SENT) throw new IllegalStateException("Cannot set request property after connection is made");
		if (field == null)
		{
			throw new NullPointerException("field == null");
		}

		if (newValue == null)
		{
			// Silently ignore null header values for backwards compatibility with older
			// android versions as well as with other URLConnection implementations.
			//
			// Some implementations send a malformed HTTP header when faced with
			// such requests, we respect the spec and ignore the header.
			return;
		}

		requestHeaders.set(field, newValue);
	}


	@Override
	synchronized public void addRequestProperty(String field, String newValue)
	{
		//TODO: Consider this: if (stage >= HEADER_SENT) throw new IllegalStateException("Cannot set request property after connection is made");

		if (field == null)
		{
			throw new NullPointerException("field == null");
		}

		if (newValue == null)
		{
			// Silently ignore null header values for backwards compatibility with older
			// android versions as well as with other URLConnection implementations.
			//
			// Some implementations send a malformed HTTP header when faced with
			// such requests, we respect the spec and ignore the header.
			return;
		}

		requestHeaders.add(field, newValue);
	}

	
	@Override
	synchronized public String getRequestProperty(String field)
	{
		if (field == null) return null;
	    return requestHeaders.get(field);
	}


	@Override
	synchronized public Map<String, List<String>> getRequestProperties()
	{
		//TODO: Consider following: if (stage >= HEADER_SENT) throw new IllegalStateException("Cannot access request header fields after connection is set");

		Headers rh = requestHeaders.build();
		TreeMap<String, List<String>> m = new TreeMap<String, List<String>>(Headers.FIELD_NAME_COMPARATOR);
		for(String name : rh.names())
			m.put(name, rh.values(name));
		return m;
	}

	
	synchronized public Headers getRequestHeaders()
	{
		return requestHeaders.build();
	}


	///
	
	@Override
	public void connect() throws IOException { /* NOP */ }

	@Override
	public void disconnect() { /* NOP */ }

	@Override
	public boolean usingProxy() { return true; }

	public int getStreamId() { return streamId; }
	protected Stage getStage() { return stage; }

	
}
