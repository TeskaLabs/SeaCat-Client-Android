package com.teskalabs.seacat.android.client.hc;

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

    protected HttpRequest request = null;
    protected HttpContext context = null;

    private int streamId = -1;
    private Lock streamIdLock = new ReentrantLock();
    private Condition streamIdCondition = streamIdLock.newCondition();

    protected final HttpRoute route;
    protected final HttpResponseFactory responseFactory;
    protected Object state;

    protected InboundStream inboundStream = null;
    protected OutboundStream outboundStream = null;

    private boolean alive = true;
    private int priority = 3; //TODO: There is no way how this changed

    protected HttpResponse response = null;
    private Lock responseReadyLock = new ReentrantLock();
    private Condition responseReadyCondition = responseReadyLock.newCondition();

    private int socketTimeout = 5*60*1000; //Value in milliseconds
    private boolean reusable = true; // Just to replicate original behaviour, no real meaning

    ///

    public SeaCatClientConnection(HttpRoute route, HttpResponseFactory responseFactory, Reactor reactor, Object state)
    {
        this.reactor = reactor;

        this.route = route;
        this.responseFactory = responseFactory;
        this.state = state;
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

        streamIdLock.lock();
        try
        {
            streamId = reactor.streamFactory.registerStream(this);
            streamIdCondition.signalAll();
        }
        finally {
            streamIdLock.unlock();
        }

        RequestLine requestLine = request.getRequestLine();
        Headers.Builder requestHeaders = new Headers.Builder();

        final String host = request.getFirstHeader("Host").getValue();

        final HeaderIterator iter = request.headerIterator();
        while (iter.hasNext())
        {
            final Header header = iter.nextHeader();
            requestHeaders.add(header.getName(), header.getValue());
        }
        requestHeaders.add("X-SC-SDK", "and"); // For Android

        // Build SYN_STREAM frame
        SPDY.buildALX1SynStream(frame, streamId, host, requestLine.getMethod(), requestLine.getUri(), requestHeaders.build(), fin_flag, this.priority);

        return new IFrameProvider.Result(frame, false);
    }


    public void waitForStreamId() throws IOException
    {
        long timeoutMillis = getSocketTimeout();
        if (timeoutMillis == 0) timeoutMillis = 1000*60*3; // 3 minutes timeout
        long cutOfTimeMillis = (System.nanoTime() / 1000000L) + timeoutMillis;

        streamIdLock.lock();
        try
        {
            while (this.streamId == -1)
            {
                long awaitMillis = cutOfTimeMillis - (System.nanoTime() / 1000000L);
                if (awaitMillis <= 0) throw new SocketTimeoutException(String.format("Connect timeout: %d", timeoutMillis));

                try {
                    streamIdCondition.awaitNanos(awaitMillis * 1000000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

        }
        finally {
            streamIdLock.unlock();
        }
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
        this.request = httpRequest;

        // Very important step, it detects if there will be a body (or RequestEntity)
        // This is also used in FIN_FLAG detection in buildFrame() method
        if (httpRequest instanceof HttpEntityEnclosingRequest) {
            outboundStream = new OutboundStream(this.reactor, this.priority);
        }

        reactor.registerFrameProvider(this, true);
    }

    @Override
    public void sendRequestEntity(HttpEntityEnclosingRequest httpEntityEnclosingRequest) throws HttpException, IOException
    {
        HttpEntity e = httpEntityEnclosingRequest.getEntity();

/*
        outboundStreamLength = this.lenStrategy.determineLength(httpEntityEnclosingRequest);
        if (outboundStreamLength == ContentLengthStrategy.CHUNKED) {
            Log.i("SeaCat", "SeaCatClientConnection / sendRequestEntity -> CHUNKED");
        } else if (outboundStreamLength == ContentLengthStrategy.IDENTITY) {
            Log.i("SeaCat", "SeaCatClientConnection / sendRequestEntity -> IDENTITY");
        } else {
            Log.i("SeaCat", "SeaCatClientConnection / sendRequestEntity -> Content-Lenght:" + outboundStreamLength);
        }
*/
        waitForStreamId();
        outboundStream.launch(streamId);
        e.writeTo(outboundStream);
        outboundStream.close();
    }


    @Override
    public void flush() throws IOException
    {
        //TODO: What to do ?
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
                if (!this.alive) throw new HttpException("Request has been canceled.");

                long awaitMillis = cutOfTimeMillis - (System.nanoTime() / 1000000L);
                if (awaitMillis <= 0) throw new SocketTimeoutException(String.format("Connect timeout: %d", timeoutMillis));

                try {
                    responseReadyCondition.awaitNanos(awaitMillis * 1000000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
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
            while (this.response == null)
            {
                if (!this.alive) return false;

                long awaitMillis = cutOfTimeMillis - (System.nanoTime() / 1000000L);
                if (awaitMillis <= 0) return false;

                try {
                    responseReadyCondition.awaitNanos(awaitMillis * 1000000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        finally {
            responseReadyLock.unlock();
        }

        return (this.response != null);
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

        responseReadyLock.lock();
        try
        {
            alive = false;
            responseReadyCondition.signalAll();
        }
        finally {
            responseReadyLock.unlock();
        }

        if (inboundStream != null) inboundStream.reset();
        if (outboundStream != null) outboundStream.reset();
    }

    @Override
    public boolean receivedALX1_SYN_REPLY(Reactor reactor, ByteBuffer frame, int frameLength, byte frameFlags)
    {
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
