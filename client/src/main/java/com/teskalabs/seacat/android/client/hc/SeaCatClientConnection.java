package com.teskalabs.seacat.android.client.hc;

import android.util.Log;

import com.teskalabs.seacat.android.client.core.Reactor;
import com.teskalabs.seacat.android.client.core.SPDY;
import com.teskalabs.seacat.android.client.http.Headers;
import com.teskalabs.seacat.android.client.http.InboundStream;
import com.teskalabs.seacat.android.client.http.OutboundStream;
import com.teskalabs.seacat.android.client.intf.IFrameProvider;
import com.teskalabs.seacat.android.client.intf.IStream;

import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpConnectionMetrics;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseFactory;
import org.apache.http.ProtocolVersion;
import org.apache.http.RequestLine;
import org.apache.http.conn.ClientConnectionRequest;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.conn.ManagedClientConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ContentLengthStrategy;
import org.apache.http.impl.entity.StrictContentLengthStrategy;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.net.ssl.SSLSession;

public class SeaCatClientConnection implements ClientConnectionRequest, ManagedClientConnection, IFrameProvider, IStream
{
    protected final Reactor reactor;

    private int streamId = -1;
    protected HttpRequest request = null;
    protected HttpContext context = null;
    protected boolean readyToSYN_STREAM = false;

    protected final HttpRoute route;
    protected final HttpResponseFactory responseFactory;
    protected Object state;

    protected InboundStream inboundStream = null;
    protected OutboundStream outboundStream = null;
    private final ContentLengthStrategy lenStrategy;
    protected long outboundStreamLength;

    private int priority = 3; //TODO: There is no way how this changed

    protected HttpResponse response = null;
    private boolean responseReady = false;
    private Lock responseReadyLock = new ReentrantLock();
    private Condition responseReadyCondition = responseReadyLock.newCondition();

    private int socketTimeout = 10000; //Value in milliseconds
    private boolean reusable = true; // Just to replicate original behaviour, no real meaning

    ///

    public SeaCatClientConnection(HttpRoute route, HttpResponseFactory responseFactory, Reactor reactor, Object state)
    {
        this.reactor = reactor;

        this.route = route;
        this.responseFactory = responseFactory;
        this.state = state;

        this.lenStrategy = new StrictContentLengthStrategy();
    }

    /// IFrameProvider

    @Override
    public Result buildFrame(Reactor reactor) throws IOException
    {
        //if (fixedContentLength > 0) addRequestProperty("Content-length", String.format("%d", fixedContentLength));

        // Add If-Modified-Since header
        //long ifModifiedSince = getIfModifiedSince();
        //if (ifModifiedSince != 0) addRequestProperty("If-Modified-Since", HttpDate.format(new Date(ifModifiedSince)));

        boolean fin_flag = (outboundStream == null);

        ByteBuffer frame = reactor.framePool.borrow("SeaCatClientConnection.buildFrame");

        streamId = reactor.streamFactory.registerStream(this);

        RequestLine requestLine = request.getRequestLine();
        Headers.Builder requestHeaders = new Headers.Builder();

        final String host = request.getFirstHeader("Host").getValue();

        final HeaderIterator iter = request.headerIterator();
        while (iter.hasNext())
        {
            final Header header = iter.nextHeader();
            requestHeaders.add(header.getName(), header.getValue());
        }
        requestHeaders.add("X-SC-OS", "and"); // For Android

        // Build SYN_STREAM frame
        SPDY.buildALX1SynStream(frame, streamId, host, requestLine.getMethod(), requestLine.getUri(), requestHeaders.build(), fin_flag, this.priority);

        // If there is an outbound stream, launch it
        if (outboundStream != null)
        {
            outboundStream.setStreamId(streamId);
            outboundStream.launch();
        }

        return new IFrameProvider.Result(frame, false);
    }

    @Override
    public int getFrameProviderPriority()
    {
        return this.priority;
    }

    /// ManagedClientConnection

    @Override
    public void open(HttpRoute httpRoute, HttpContext httpContext, HttpParams httpParams) throws IOException
    {
        this.context = httpContext;
    }

    @Override
    public void close() throws IOException
    {
        //TODO: This ...
    }

    @Override
    public void shutdown() throws IOException
    {
        //TODO: This ...
    }

    @Override
    public void sendRequestHeader(HttpRequest httpRequest) throws HttpException, IOException
    {
        Log.i("SeaCat", "SeaCatClientConnection / sendRequestHeader");

        this.request = httpRequest;
        this.readyToSYN_STREAM = true;
    }

    @Override
    public void sendRequestEntity(HttpEntityEnclosingRequest httpEntityEnclosingRequest) throws HttpException, IOException
    {
        HttpEntity e = httpEntityEnclosingRequest.getEntity();
        Log.i("SeaCat", "SeaCatClientConnection / sendRequestEntity");

        outboundStreamLength = this.lenStrategy.determineLength(httpEntityEnclosingRequest);
        if (outboundStreamLength == ContentLengthStrategy.CHUNKED) {
            Log.i("SeaCat", "SeaCatClientConnection / sendRequestEntity -> CHUNKED");
        } else if (outboundStreamLength == ContentLengthStrategy.IDENTITY) {
            Log.i("SeaCat", "SeaCatClientConnection / sendRequestEntity -> IDENTITY");
        } else {
            Log.i("SeaCat", "SeaCatClientConnection / sendRequestEntity -> Content-Lenght:" + outboundStreamLength);
        }

        outboundStream = new OutboundStream(this.reactor, this.priority);
        e.writeTo(outboundStream);
        outboundStream.close();

        this.readyToSYN_STREAM = true;
    }


    @Override
    public void flush() throws IOException
    {
        // This a trick how to send header and SYN_STREAM
        if (this.readyToSYN_STREAM)
        {
            reactor.registerFrameProvider(this, true);
            this.readyToSYN_STREAM = false;
        }
    }


    @Override
    public HttpResponse receiveResponseHeader() throws HttpException, IOException
    {
        long timeoutMillis = getSocketTimeout();
        if (timeoutMillis == 0) timeoutMillis = 1000*60*3; // 3 minutes timeout
        long cutOfTimeMillis = (System.nanoTime() / 1000000L) + timeoutMillis;

        HttpResponse resp = null;

        responseReadyLock.lock();
        try
        {
            while (this.response == null)
            {
                if (this.responseReady) throw new HttpException("Response already received");

                long awaitMillis = cutOfTimeMillis - (System.nanoTime() / 1000000L);
                if (awaitMillis <= 0) throw new SocketTimeoutException("Connect timeout: "+ awaitMillis);

                try {
                    responseReadyCondition.awaitNanos(awaitMillis * 1000000L);
                } catch (InterruptedException e) {
                }
            }

            resp = this.response;
            this.response = null;

        }
        finally {
            responseReadyLock.unlock();
        }

        return resp;
    }


    @Override
    public void receiveResponseEntity(HttpResponse response) throws HttpException, IOException
    {
        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(inboundStream);
        response.setEntity(entity);
    }


    @Override
    public InetAddress getLocalAddress()
    {
        return null;
    }

    @Override
    public int getLocalPort()
    {
        return 0;
    }

    @Override
    public InetAddress getRemoteAddress()
    {
        return null;
    }

    @Override
    public int getRemotePort()
    {
        return 0;
    }



    @Override
    public boolean isStale()
    {
        return false;
    }


    @Override
    public boolean isOpen()
    {
        return this.context != null;
    }


    /**
     * Sets the socket timeout value.
     *
     * @param timeout timeout value in milliseconds
     */
    @Override
    public void setSocketTimeout(int timeout)
    {
        this.socketTimeout = timeout;
    }


    /**
     * Returns the socket timeout value.
     *
     * @return positive value in milliseconds if a timeout is set,
     * <code>0</code> if timeout is disabled or <code>-1</code> if
     * timeout is undefined.
     */
    @Override
    public int getSocketTimeout()
    {
        return this.socketTimeout;
    }


    @Override
    public void tunnelTarget(boolean b, HttpParams httpParams) throws IOException
    {

    }

    @Override
    public void tunnelProxy(HttpHost httpHost, boolean b, HttpParams httpParams) throws IOException
    {

    }

    @Override
    public void layerProtocol(HttpContext httpContext, HttpParams httpParams) throws IOException
    {

    }

    @Override
    public void markReusable()
    {
        this.reusable = true;
    }

    @Override
    public void unmarkReusable()
    {
        this.reusable = false;
    }

    @Override
    public boolean isMarkedReusable()
    {
        return this.reusable;
    }

    @Override
    public void setState(Object o)
    {
        this.state = o;
    }

    @Override
    public Object getState()
    {
        return this.state;
    }


    @Override
    public void setIdleDuration(long l, TimeUnit timeUnit)
    {

    }

    @Override
    public void releaseConnection() throws IOException
    {
        // NOP
    }

    @Override
    public void abortConnection() throws IOException
    {

    }


    /**
     * Checks if response data is available from the connection. May wait for the specified time until some data becomes available.
     * Note that some implementations may completely ignore the timeout parameter.
     *
     * @param timeout timeout value in milliseconds
     */
    @Override
    public boolean isResponseAvailable(int timeout) throws IOException
    {
        if (timeout == 0) timeout = 1000*60*3; // 3 minutes timeout
        long cutOfTimeMillis = (System.nanoTime() / 1000000L) + timeout;

        responseReadyLock.lock();
        try
        {
            while (this.responseReady == false)
            {

                long awaitMillis = cutOfTimeMillis - (System.nanoTime() / 1000000L);
                if (awaitMillis <= 0) return false;

                try {
                    responseReadyCondition.awaitNanos(awaitMillis * 1000000L);
                } catch (InterruptedException e) {
                }
            }
        }
        finally {
            responseReadyLock.unlock();
        }
        return responseReady;
    }

    @Override
    public HttpConnectionMetrics getMetrics()
    {
        //TODO: This ...
        return null;
    }

    @Override
    public boolean isSecure()
    {
        return true;
    }

    @Override
    public HttpRoute getRoute()
    {
        return this.route;
    }

    @Override
    public SSLSession getSSLSession()
    {
        return null;
    }

    /// ClientConnectionRequest

    @Override
    public ManagedClientConnection getConnection(long l, TimeUnit timeUnit) throws InterruptedException, ConnectionPoolTimeoutException
    {
        return this;
    }

    @Override
    public void abortRequest()
    {

    }

    // IInboundStream

    @Override
    public void reset()
    {
        if (inboundStream != null) inboundStream.reset();
        if (outboundStream != null) outboundStream.reset();
    }

    @Override
    public boolean receivedALX1_SYN_REPLY(Reactor reactor, ByteBuffer frame, int frameLength, byte frameFlags)
    {
        Log.i("SeaCat", "SeaCatClientConnection / receivedALX1_SYN_REPLY");

        // Body
        inboundStream = new InboundStream(reactor, getSocketTimeout());
        inboundStream.setStreamId(streamId);

        // Status
        int responseCode = frame.getShort();
        final ProtocolVersion pver = new ProtocolVersion("HTTP", 1, 1);
        final HttpResponse resp = responseFactory.newHttpResponse(pver, responseCode, context);

        // Reserved (unused) 16 bits
        frame.getShort();

        // Read headers
        for(;frame.position() < frame.limit();)
        {
            String k = SPDY.parseVLEString(frame);
            String v = SPDY.parseVLEString(frame);
            resp.addHeader(k, v);
        }

        responseReadyLock.lock();
        try
        {
            responseReady = true;
            this.response = resp;
            responseReadyCondition.signalAll();
        }
        finally {
            responseReadyLock.unlock();
        }

        if ((frameFlags & SPDY.FLAG_FIN) == SPDY.FLAG_FIN) inboundStream.close();
        return true;
    }

    @Override
    public boolean receivedSPD3_RST_STREAM(Reactor reactor, ByteBuffer frame, int frameLength, byte frameFlags)
    {
        reset();
        return true;
    }

    @Override
    public boolean receivedDataFrame(Reactor reactor, ByteBuffer frame, int frameLength, byte frameFlags)
    {
        if (inboundStream == null) return true; //TODO: Add warning that data are discared

        boolean ret = inboundStream.inboundData(frame);
        if ((frameFlags & SPDY.FLAG_FIN) == SPDY.FLAG_FIN) inboundStream.close();
        return ret;
    }
}
