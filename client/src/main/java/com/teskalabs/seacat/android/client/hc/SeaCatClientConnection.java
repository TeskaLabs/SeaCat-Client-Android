package com.teskalabs.seacat.android.client.hc;

import org.apache.http.HttpConnectionMetrics;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.conn.ClientConnectionRequest;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.conn.ManagedClientConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSession;

public class SeaCatClientConnection implements ClientConnectionRequest, ManagedClientConnection
{
    protected final HttpRoute route;
    protected final Object state;

    public SeaCatClientConnection(HttpRoute route, Object state)
    {
        this.route = route;
        this.state = state;
    }

    @Override
    public ManagedClientConnection getConnection(long l, TimeUnit timeUnit) throws InterruptedException, ConnectionPoolTimeoutException
    {
        return this;
    }

    @Override
    public void abortRequest()
    {

    }

    @Override
    public boolean isSecure()
    {
        return true;
    }

    @Override
    public HttpRoute getRoute()
    {
        return null;
    }

    @Override
    public SSLSession getSSLSession()
    {
        return null;
    }

    @Override
    public void open(HttpRoute httpRoute, HttpContext httpContext, HttpParams httpParams) throws IOException
    {

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
        return null;
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

    @Override
    public void sendRequestHeader(HttpRequest httpRequest) throws HttpException, IOException
    {

    }

    @Override
    public void sendRequestEntity(HttpEntityEnclosingRequest httpEntityEnclosingRequest) throws HttpException, IOException
    {

    }

    @Override
    public HttpResponse receiveResponseHeader() throws HttpException, IOException
    {
        return null;
    }

    @Override
    public void receiveResponseEntity(HttpResponse httpResponse) throws HttpException, IOException
    {

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
}
