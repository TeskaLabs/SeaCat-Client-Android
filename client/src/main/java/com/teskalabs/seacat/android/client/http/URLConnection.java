package com.teskalabs.seacat.android.client.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.lang.System;

import com.teskalabs.seacat.android.client.core.Reactor;
import com.teskalabs.seacat.android.client.core.SPDY;
import com.teskalabs.seacat.android.client.intf.*;

public class URLConnection extends HttpURLConnection implements IFrameProvider , IStream
{
	protected final Reactor reactor;

	protected final InboundStream inboundStream;	
	protected OutboundStream outboundStream = null;

	private Headers responseHeaders = null;
	private final Headers.Builder requestHeaders = new Headers.Builder();

    private boolean launched = false;
	private int streamId = -1;
	private int priority;
	private OutboundNext outboundNext;

    private boolean responseReady = false;
    private Lock responseReadyLock = new ReentrantLock();
    private Condition responseReadyCondition = responseReadyLock.newCondition();

	///

	public enum OutboundNext
	{
		SYN_STREAM(1),
		DATA(2),
		FIN_FLAG(3);

		private final int code;

		private OutboundNext(int code)
		{
			this.code=code;
		}
	}

	///
	
	public URLConnection(Reactor reactor, URL u, int priority)
	{
		super(u);
		this.reactor = reactor;
		this.priority = priority;
		this.outboundNext = OutboundNext.SYN_STREAM;

        this.inboundStream = new InboundStream(this);
	}

    ///

	/*
	 * Triggers an actual request to the server ...
	 *
	 */
	final void launch() throws IOException
    {
        if (!launched)
        {
            if (outboundStream != null)
            {
                int contentLength = outboundStream.getContentLength();
                if ((contentLength > 0) && (getRequestProperty("Content-length") == null))
                {
                    // If there is an outboundStream with data, we can determine Content-Length
                    outboundStream.close();

                    setRequestProperty("Content-length", "" + contentLength);
                }
            }

            launched = true;
            reactor.registerFrameProvider(this, true);
        }
	}

    final boolean isLaunched()
    {
        return launched;
    }

    ///

	/*
	 * Return a response body
	 * This is the place where request is usually executed
	 */
	@Override
	public final InputStream getInputStream() throws IOException
	{
		launch();
		return this.inboundStream;
	}


    /*
     * Return a response status code
     * This method triggers and actual request (if needed) and waits for response code from server.
     *
     */
    @Override
    public int getResponseCode() throws IOException
    {
        launch();
        waitForResponse();
        return super.getResponseCode();
    }

    /*
     * Block and wait for response from server.
     * Timeouts using SocketTimeoutException after getConnectTimeout() + getReadTimeout()
     */
    protected void waitForResponse() throws SocketTimeoutException
    {
        long timeoutMillis = getConnectTimeout();
        if (timeoutMillis == 0) timeoutMillis = 1000*60*3; // 3 minutes timeout
        long cutOfTimeMillis = (System.nanoTime() / 1000000L) + timeoutMillis;

        responseReadyLock.lock();
        try
        {
            while (responseReady != true) {
                long awaitMillis = cutOfTimeMillis - (System.nanoTime() / 1000000L);
                if (awaitMillis <= 0) throw new SocketTimeoutException("Connect timeout");

                try {
                    responseReadyCondition.awaitNanos(awaitMillis * 1000000L);
                } catch (InterruptedException e) {
                }
            }
        }
        finally {
            responseReadyLock.unlock();
        }
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
			if (outboundNext.code > OutboundNext.SYN_STREAM.code) throw new ProtocolException("Cannot write output after reading input.");
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
		
		switch (outboundNext)
		{
			case SYN_STREAM:
			{
				frame = buildSYN_STREAM();
				if (outboundStream != null)
				{
					assert((frame.getShort(4) & SPDY.FLAG_FIN) == 0);
					keep = !outboundStream.isQueueEmpty();
                    outboundNext = OutboundNext.DATA;
				}
				else
				{
					assert((frame.getShort(4) & SPDY.FLAG_FIN) == SPDY.FLAG_FIN);
                    outboundNext = OutboundNext.FIN_FLAG;
				}
				return new IFrameProvider.Result(frame, keep);
			}


			case DATA:
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
                        outboundNext = OutboundNext.FIN_FLAG;
					}
				}
				return new IFrameProvider.Result(frame, keep);
			}

			default:
				System.err.println("Nothing to build - incorrect outboundNext: " + this.outboundNext);
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
		if (outboundStream != null) outboundStream.reset();
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

        responseReadyLock.lock();
        try
        {
            responseReady = true;
            responseReadyCondition.signalAll();
        }
        finally {
            responseReadyLock.unlock();
        }

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
	
}
