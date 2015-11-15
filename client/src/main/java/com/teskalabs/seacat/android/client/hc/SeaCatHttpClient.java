package com.teskalabs.seacat.android.client.hc;

import com.teskalabs.seacat.android.client.core.Reactor;

import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.client.AuthenticationHandler;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.RedirectHandler;
import org.apache.http.client.RequestDirector;
import org.apache.http.client.UserTokenHandler;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestExecutor;

/*
 * Based o the Apache HttpComponents ( https://hc.apache.org )
 *
 * A quite complicated history on Android:
 * https://hc.apache.org/httpcomponents-client-4.5.x/android-port.html
 *
 * Google Android 1.0 was released with a pre-BETA snapshot of Apache HttpClient.
 * To coincide with the first Android release Apache HttpClient 4.0 APIs had to be frozen prematurely,
 * while many of interfaces and internal structures were still not fully worked out.
 * As Apache HttpClient 4.0 was maturing the project was expecting Google to incorporate the latest code improvements into their code tree.
 * Unfortunately it did not happen. Version of Apache HttpClient shipped with Android has effectively become a fork.
 * Eventually Google decided to discontinue further development of their fork while refusing to upgrade
 * to the stock version of Apache HttpClient citing compatibility concerns as a reason for such decision.
 * As a result those Android developers who would like to continue using Apache HttpClient APIs on Android cannot take advantage of newer features,
 * performance improvements and bug fixes.
 *
 * Given that as of Android API 23 Google's fork of HttpClient has been removed this project has been discontinued.
 *
 * This is a source code of Apache HttpComponents that is very likely used in Android:
 * http://grepcode.com/snapshot/repo1.maven.org/maven2/org.apache.httpcomponents/httpcore/4.0-beta3/
 *
 */


public class SeaCatHttpClient extends DefaultHttpClient implements HttpClient
{
    final protected Reactor reactor;

    /**
     * Creates a new HTTP client from parameters and a connection manager.
     *
     * @param params    the parameters
     * @param conman    the connection manager
     */
    public SeaCatHttpClient(
            final ClientConnectionManager conman,
            final HttpParams params,
            Reactor reactor
    )
    {
        super(conman, params);
        this.reactor = reactor;
    }


    public SeaCatHttpClient(final HttpParams params, Reactor reactor)
    {
        super(null, params);
        this.reactor = reactor;
    }


    public SeaCatHttpClient(Reactor reactor)
    {
        super(null, null);
        this.reactor = reactor;
    }

    ///

    @Override
    protected HttpRequestExecutor createRequestExecutor()
    {
        return new SeaCatHttpRequestExecutor();
    }

    @Override
    protected RequestDirector createClientRequestDirector(
            final HttpRequestExecutor requestExec,
            final ClientConnectionManager conman,
            final ConnectionReuseStrategy reustrat,
            final ConnectionKeepAliveStrategy kastrat,
            final HttpRoutePlanner rouplan,
            final HttpProcessor httpProcessor,
            final HttpRequestRetryHandler retryHandler,
            final RedirectHandler redirectHandler,
            final AuthenticationHandler targetAuthHandler,
            final AuthenticationHandler proxyAuthHandler,
            final UserTokenHandler stateHandler,
            final HttpParams params)
    {
        return new SeaCatRequestDirector(
                requestExec,
                conman,
                reustrat,
                kastrat,
                rouplan,
                httpProcessor,
                retryHandler,
                redirectHandler,
                targetAuthHandler,
                proxyAuthHandler,
                stateHandler,
                params);
    }


    @Override
    protected ClientConnectionManager createClientConnectionManager()
    {
        return new SeaCatClientConnManager(reactor);
    }

}
