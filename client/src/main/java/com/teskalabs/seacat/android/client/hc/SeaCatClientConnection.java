package com.teskalabs.seacat.android.client.hc;

import android.util.Log;

import com.teskalabs.seacat.android.client.core.Reactor;
import com.teskalabs.seacat.android.client.core.SPDY;
import com.teskalabs.seacat.android.client.http.Headers;
import com.teskalabs.seacat.android.client.http.HttpStatus;
import com.teskalabs.seacat.android.client.http.InboundStream;
import com.teskalabs.seacat.android.client.intf.IFrameProvider;
import com.teskalabs.seacat.android.client.intf.IStream;

import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpConnectionMetrics;
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

    private int streamId = -1;
    protected HttpRequest request = null;
    protected HttpResponse response = null;
    protected HttpContext context = null;

    protected final HttpRoute route;
    protected final HttpResponseFactory responseFactory;
    protected final Object state;

    protected InboundStream inboundStream = null;

    private int priority = 3; //TODO: There is no way how this changed

    private boolean responseReady = false;
    private Lock responseReadyLock = new ReentrantLock();
    private Condition responseReadyCondition = responseReadyLock.newCondition();

    ///

    public SeaCatClientConnection(HttpRoute route, HttpResponseFactory responseFactory, Object state)
    {
        this.reactor = com.teskalabs.seacat.android.client.SeaCatClient.getReactor(); // TODO: Better way how to get this

        this.route = route;
        this.responseFactory = responseFactory;
        this.state = state;
    }

    /// IFrameProvider

    @Override
    public Result buildFrame(Reactor reactor) throws IOException
    {
        boolean keep = false;
        ByteBuffer frame = null;

        frame = buildSYN_STREAM();
        return new IFrameProvider.Result(frame, keep);
    }

    private ByteBuffer buildSYN_STREAM() throws IOException
    {
        //if (fixedContentLength > 0) addRequestProperty("Content-length", String.format("%d", fixedContentLength));

        // Add If-Modified-Since header
        //long ifModifiedSince = getIfModifiedSince();
        //if (ifModifiedSince != 0) addRequestProperty("If-Modified-Since", HttpDate.format(new Date(ifModifiedSince)));

        boolean fin_flag = true; //(outboundStream == null);

        ByteBuffer frame = reactor.framePool.borrow("SeaCatClientConnection.buildSYN_STREAM");

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
        requestHeaders.add("X-SC-OS", "and");

        // Build SYN_STREAM frame
        SPDY.buildALX1SynStream(frame, streamId, host, requestLine.getMethod(), requestLine.getUri(), requestHeaders.build(), fin_flag, this.priority);

        return frame;
    }

    @Override
    public int getFrameProviderPriority()
    {
        return this.priority;
    }

    /// ManagedClientConnection

    @Override
    public void sendRequestHeader(HttpRequest httpRequest) throws HttpException, IOException
    {
        Log.i("SeaCat", "SeaCatClientConnection / sendRequestHeader  "+ httpRequest);
        this.request = httpRequest;
        reactor.registerFrameProvider(this, true);
    }

    @Override
    public void sendRequestEntity(HttpEntityEnclosingRequest httpEntityEnclosingRequest) throws HttpException, IOException
    {
        Log.i("SeaCat", "SeaCatClientConnection / sendRequestEntity");
    }

    @Override
    public HttpResponse receiveResponseHeader() throws HttpException, IOException
    {
        Log.i("SeaCat", "SeaCatClientConnection / receiveResponseHeader");

        long timeoutMillis = 3000; //TODO: getConnectTimeout();
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

        return this.response;
    }

    @Override
    public void receiveResponseEntity(HttpResponse httpResponse) throws HttpException, IOException
    {
        Log.i("SeaCat", "SeaCatClientConnection / receiveResponseEntity");
    }

    @Override
    public void flush() throws IOException
    {

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
    public void close() throws IOException
    {

    }

    @Override
    public boolean isOpen()
    {
        return false;
    }

    @Override
    public boolean isStale()
    {
        return false;
    }

    @Override
    public void setSocketTimeout(int i)
    {

    }

    @Override
    public int getSocketTimeout()
    {
        return 0;
    }

    @Override
    public void shutdown() throws IOException
    {

    }

    @Override
    public HttpConnectionMetrics getMetrics()
    {
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

    @Override
    public void open(HttpRoute httpRoute, HttpContext httpContext, HttpParams httpParams) throws IOException
    {
        this.context = context;
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

    }

    @Override
    public void unmarkReusable()
    {

    }

    @Override
    public boolean isMarkedReusable()
    {
        return false;
    }

    @Override
    public void setState(Object o)
    {
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

    }

    @Override
    public void abortConnection() throws IOException
    {

    }

    @Override
    public boolean isResponseAvailable(int i) throws IOException
    {
        return false;
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
        inboundStream.reset();
    }

    @Override
    public boolean receivedALX1_SYN_REPLY(Reactor reactor, ByteBuffer frame, int frameLength, byte frameFlags)
    {
        Log.i("SeaCat", "SeaCatClientConnection / receivedALX1_SYN_REPLY");

        // Body
        inboundStream = new InboundStream(reactor, 3000); //TODO: Real timeout
        inboundStream.setStreamId(streamId);
        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(inboundStream);

        // Status
        int responseCode = frame.getShort();
        ProtocolVersion pver = new ProtocolVersion("HTTP", 1, 1);
        this.response = responseFactory.newHttpResponse(pver, responseCode, context);
        this.response.setEntity(entity);

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
    public boolean receivedSPD3_RST_STREAM(Reactor reactor, ByteBuffer frame, int frameLength, byte frameFlags)
    {
        if (inboundStream != null) inboundStream.reset();
        return true;
    }

    @Override
    public boolean receivedDataFrame(Reactor reactor, ByteBuffer frame, int frameLength, byte frameFlags)
    {
        if (inboundStream == null) return true; //TODO: Add warning that data are discarted

        boolean ret = inboundStream.inboundData(frame);
        if ((frameFlags & SPDY.FLAG_FIN) == SPDY.FLAG_FIN) inboundStream.close();
        return ret;
    }
}
