package com.teskalabs.seacat.android.client.hc;

import org.apache.http.HttpResponseFactory;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ClientConnectionRequest;
import org.apache.http.conn.ManagedClientConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.DefaultHttpResponseFactory;

import java.util.concurrent.TimeUnit;

public class ClientConnManager  implements ClientConnectionManager
{
    final SchemeRegistry registry;
    HttpResponseFactory responseFactory = new DefaultHttpResponseFactory();

    public ClientConnManager()
    {
        registry = new SchemeRegistry();
        registry.register(
                new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        registry.register(
                new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));



    }

    @Override
    public SchemeRegistry getSchemeRegistry()
    {
        return registry;
    }

    @Override
    public ClientConnectionRequest requestConnection(HttpRoute httpRoute, Object state)
    {
        return new SeaCatClientConnection(httpRoute, responseFactory, state);
    }

    @Override
    public void releaseConnection(ManagedClientConnection managedClientConnection, long l, TimeUnit timeUnit)
    {

    }

    @Override
    public void closeIdleConnections(long l, TimeUnit timeUnit)
    {

    }

    @Override
    public void closeExpiredConnections()
    {

    }

    @Override
    public void shutdown()
    {

    }
}
