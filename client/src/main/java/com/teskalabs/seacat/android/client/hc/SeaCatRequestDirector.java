package com.teskalabs.seacat.android.client.hc;

import android.util.Log;

import com.teskalabs.seacat.android.client.SeaCatClient;
import com.teskalabs.seacat.android.client.SeaCatInternals;

import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.AuthenticationHandler;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.NonRepeatableRequestException;
import org.apache.http.client.RedirectException;
import org.apache.http.client.RedirectHandler;
import org.apache.http.client.RequestDirector;
import org.apache.http.client.UserTokenHandler;
import org.apache.http.client.methods.AbortableHttpRequest;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.conn.BasicManagedEntity;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ClientConnectionRequest;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.ManagedClientConnection;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.client.EntityEnclosingRequestWrapper;
import org.apache.http.impl.client.RequestWrapper;
import org.apache.http.impl.client.RoutedRequest;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.ProtocolException;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;


public class SeaCatRequestDirector implements RequestDirector
{

    /** The connection manager. */
    protected final ClientConnectionManager connManager;

    /** The route planner. */
    protected final HttpRoutePlanner routePlanner;

    /** The connection re-use strategy. */
    protected final ConnectionReuseStrategy reuseStrategy;

    /** The keep-alive duration strategy. */
    protected final ConnectionKeepAliveStrategy keepAliveStrategy;

    /** The request executor. */
    protected final HttpRequestExecutor requestExec;

    /** The HTTP protocol processor. */
    protected final HttpProcessor httpProcessor;

    /** The request retry handler. */
    protected final HttpRequestRetryHandler retryHandler;

    /** The redirect handler. */
    protected final RedirectHandler redirectHandler;

    /** The target authentication handler. */
    private final AuthenticationHandler targetAuthHandler;

    /** The proxy authentication handler. */
    private final AuthenticationHandler proxyAuthHandler;

    /** The user token handler. */
    private final UserTokenHandler userTokenHandler;

    /** The HTTP parameters. */
    protected final HttpParams params;

    /** The currently allocated connection. */
    protected ManagedClientConnection managedConn;

    private int redirectCount;

    private int maxRedirects;


    public SeaCatRequestDirector(
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
            final UserTokenHandler userTokenHandler,
            final HttpParams params)
    {

        if (requestExec == null) {
            throw new IllegalArgumentException
                    ("Request executor may not be null.");
        }
        if (conman == null) {
            throw new IllegalArgumentException
                    ("Client connection manager may not be null.");
        }
        if (reustrat == null) {
            throw new IllegalArgumentException
                    ("Connection reuse strategy may not be null.");
        }
        if (kastrat == null) {
            throw new IllegalArgumentException
                    ("Connection keep alive strategy may not be null.");
        }
        if (rouplan == null) {
            throw new IllegalArgumentException
                    ("Route planner may not be null.");
        }
        if (httpProcessor == null) {
            throw new IllegalArgumentException
                    ("HTTP protocol processor may not be null.");
        }
        if (retryHandler == null) {
            throw new IllegalArgumentException
                    ("HTTP request retry handler may not be null.");
        }
        if (redirectHandler == null) {
            throw new IllegalArgumentException
                    ("Redirect handler may not be null.");
        }
        if (targetAuthHandler == null) {
            throw new IllegalArgumentException
                    ("Target authentication handler may not be null.");
        }
        if (proxyAuthHandler == null) {
            throw new IllegalArgumentException
                    ("Proxy authentication handler may not be null.");
        }
        if (userTokenHandler == null) {
            throw new IllegalArgumentException
                    ("User token handler may not be null.");
        }
        if (params == null) {
            throw new IllegalArgumentException
                    ("HTTP parameters may not be null");
        }
        this.requestExec       = requestExec;
        this.connManager       = conman;
        this.reuseStrategy     = reustrat;
        this.keepAliveStrategy = kastrat;
        this.routePlanner      = rouplan;
        this.httpProcessor     = httpProcessor;
        this.retryHandler      = retryHandler;
        this.redirectHandler   = redirectHandler;
        this.targetAuthHandler = targetAuthHandler;
        this.proxyAuthHandler  = proxyAuthHandler;
        this.userTokenHandler  = userTokenHandler;
        this.params            = params;

        this.redirectCount = 0;
        this.maxRedirects = this.params.getIntParameter(ClientPNames.MAX_REDIRECTS, 100);
    } // constructor


    @Override
    public HttpResponse execute(HttpHost target, HttpRequest request, HttpContext context) throws HttpException, IOException
    {
        Log.i(SeaCatInternals.L, "SeaCatRequestDirector:execute()");

        HttpRequest orig = request;
        RequestWrapper origWrapper = wrapRequest(orig);
        origWrapper.setParams(params);
        HttpRoute origRoute = determineRoute(target, origWrapper, context);

        RoutedRequest roureq = new RoutedRequest(origWrapper, origRoute);

        long timeout = ConnManagerParams.getTimeout(params);

        int execCount = 0;

        HttpResponse response = null;
        boolean done = false;
        try {
            while (!done)
            {
                // In this loop, the RoutedRequest may be replaced by a
                // followup request and route. The request and route passed
                // in the method arguments will be replaced. The original
                // request is still available in 'orig'.

                RequestWrapper wrapper = roureq.getRequest();
                HttpRoute route = roureq.getRoute();

                // See if we have a user token bound to the execution context
                Object userToken = context.getAttribute(ClientContext.USER_TOKEN);

                // Allocate connection if needed
                if (managedConn == null) {
                    ClientConnectionRequest connRequest = connManager.requestConnection(
                            route, userToken);
                    if (orig instanceof AbortableHttpRequest) {
                        ((AbortableHttpRequest) orig).setConnectionRequest(connRequest);
                    }

                    try {
                        managedConn = connRequest.getConnection(timeout, TimeUnit.MILLISECONDS);
                    } catch(InterruptedException interrupted) {
                        InterruptedIOException iox = new InterruptedIOException();
                        iox.initCause(interrupted);
                        throw iox;
                    }

                    if (HttpConnectionParams.isStaleCheckingEnabled(params))
                    {
                        // validate connection
                        Log.d(SeaCatInternals.L, "Stale connection check");
                        if (managedConn.isStale()) {
                            Log.d(SeaCatInternals.L, "Stale connection detected");
                            managedConn.close();
                        }
                    }
                }

/*
                if (orig instanceof AbortableHttpRequest) {
                    ((AbortableHttpRequest) orig).setReleaseTrigger(managedConn);
                }
*/

                // Reopen connection if needed
                if (!managedConn.isOpen()) {
                    managedConn.open(route, context, params);
                }

/*
                try {
                    establishRoute(route, context);
                } catch (TunnelRefusedException ex) {
                    if (this.log.isDebugEnabled()) {
                        this.log.debug(ex.getMessage());
                    }
                    response = ex.getResponse();
                    break;
                }
*/
                // Reset headers on the request wrapper
                wrapper.resetHeaders();

                // Re-write request URI if needed
                rewriteRequestURI(wrapper, route);

                // Use virtual host if set
                target = (HttpHost) wrapper.getParams().getParameter(ClientPNames.VIRTUAL_HOST);

                if (target == null) {
                    target = route.getTargetHost();
                }

                // Populate the execution context
                context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, target);

                //HttpHost proxy = route.getProxyHost();
                //context.setAttribute(ExecutionContext.HTTP_PROXY_HOST, proxy);
                context.setAttribute(ExecutionContext.HTTP_CONNECTION, managedConn);
                //context.setAttribute(ClientContext.TARGET_AUTH_STATE, targetAuthState);
                //context.setAttribute(ClientContext.PROXY_AUTH_STATE, proxyAuthState);
                context.setAttribute(ExecutionContext.HTTP_REQUEST, wrapper);

                // Run request protocol interceptors
                requestExec.preProcess(wrapper, httpProcessor, context);

                boolean retrying = true;
                while (retrying) {
                    // Increment total exec count (with redirects)
                    execCount++;
                    // Increment exec count for this particular request
                    wrapper.incrementExecCount();
                    if (wrapper.getExecCount() > 1 && !wrapper.isRepeatable()) {
                        throw new NonRepeatableRequestException("Cannot retry request " +
                                "with a non-repeatable request entity");
                    }

                    try {
                        Log.d(SeaCatInternals.L, "Attempt " + execCount + " to execute request");
                        response = requestExec.execute(wrapper, managedConn, context);
                        retrying = false;

                    } catch (IOException ex) {
                        Log.d(SeaCatInternals.L, "Closing the connection.");
                        managedConn.close();
                        if (retryHandler.retryRequest(ex, execCount, context)) {
                            Log.e(SeaCatInternals.L,"Exception caught when processing request", ex);
                            Log.i(SeaCatInternals.L,"Retrying request");
                        } else {
                            throw ex;
                        }

                        // If we have a direct route to the target host
                        // just re-open connection and re-try the request
                        if (route.getHopCount() == 1) {
                            Log.d(SeaCatInternals.L, "Reopening the direct connection.");
                            managedConn.open(route, context, params);
                        } else {
                            // otherwise give up
                            retrying = false;
                        }

                    }

                }

                // Run response protocol interceptors
                response.setParams(params);
                requestExec.postProcess(response, httpProcessor, context);


                RoutedRequest followup = handleResponse(roureq, response, context);
                if (followup == null) {
                    done = true;
                } else {
                    managedConn.close();
                    roureq = followup;
                }

                userToken = this.userTokenHandler.getUserToken(context);
                context.setAttribute(ClientContext.USER_TOKEN, userToken);
                if (managedConn != null) {
                    managedConn.setState(userToken);
                }

            } // while not done


            // check for entity, release connection if possible
            if ((response == null) || (response.getEntity() == null) ||
                    !response.getEntity().isStreaming()) {
//                releaseConnection();

            } else {
                // install an auto-release entity
                HttpEntity entity = response.getEntity();
                entity = new BasicManagedEntity(entity, managedConn, false);
                response.setEntity(entity);
            }

            return response;

        } catch (HttpException ex) {
            abortConnection();
            throw ex;
        } catch (IOException ex) {
            abortConnection();
            throw ex;
        } catch (RuntimeException ex) {
            abortConnection();
            throw ex;
        }
    }

    ///

    private RequestWrapper wrapRequest(
            final HttpRequest request) throws ProtocolException
    {
        if (request instanceof HttpEntityEnclosingRequest) {
            return new EntityEnclosingRequestWrapper(
                    (HttpEntityEnclosingRequest) request);
        } else {
            return new RequestWrapper(
                    request);
        }
    }

    /**
     * Determines the route for a request.
     * Called by {@link #execute}
     * to determine the route for either the original or a followup request.
     *
     * @param target    the target host for the request.
     *                  Implementations may accept <code>null</code>
     *                  if they can still determine a route, for example
     *                  to a default target or by inspecting the request.
     * @param request   the request to execute
     * @param context   the context to use for the execution,
     *                  never <code>null</code>
     *
     * @return  the route the request should take
     *
     * @throws HttpException    in case of a problem
     */
    protected HttpRoute determineRoute(HttpHost    target,
                                       HttpRequest request,
                                       HttpContext context)
            throws HttpException
    {

        if (target == null) {
            target = (HttpHost) request.getParams().getParameter(ClientPNames.DEFAULT_HOST);
        }
        if (target == null) {
            throw new IllegalStateException
                    ("Target host must not be null, or set in parameters.");
        }

        return this.routePlanner.determineRoute(target, request, context);
    }


    protected void rewriteRequestURI(
            final RequestWrapper request,
            final HttpRoute route) throws ProtocolException
    {
        try {

            URI uri = request.getURI();
            if (route.getProxyHost() != null && !route.isTunnelled()) {
                // Make sure the request URI is absolute
                if (!uri.isAbsolute()) {
                    HttpHost target = route.getTargetHost();
                    uri = URIUtils.rewriteURI(uri, target);
                    request.setURI(uri);
                }
            } else {
                // Make sure the request URI is relative
                if (uri.isAbsolute()) {
                    uri = URIUtils.rewriteURI(uri, null);
                    request.setURI(uri);
                }
            }

        } catch (URISyntaxException ex) {
            throw new ProtocolException("Invalid URI: " +
                    request.getRequestLine().getUri(), ex);
        }
    }

    /**
     * Analyzes a response to check need for a followup.
     *
     * @param roureq    the request and route.
     * @param response  the response to analayze
     * @param context   the context used for the current request execution
     *
     * @return  the followup request and route if there is a followup, or
     *          <code>null</code> if the response should be returned as is
     *
     * @throws HttpException    in case of a problem
     * @throws IOException      in case of an IO problem
     */
    protected RoutedRequest handleResponse(RoutedRequest roureq,
                                           HttpResponse response,
                                           HttpContext context)
            throws HttpException, IOException {

        HttpRoute route = roureq.getRoute();
        HttpHost proxy = route.getProxyHost();
        RequestWrapper request = roureq.getRequest();

        HttpParams params = request.getParams();
        if (HttpClientParams.isRedirecting(params) &&
                this.redirectHandler.isRedirectRequested(response, context)) {

            if (redirectCount >= maxRedirects) {
                throw new RedirectException("Maximum redirects ("
                        + maxRedirects + ") exceeded");
            }
            redirectCount++;

            URI uri = this.redirectHandler.getLocationURI(response, context);

            HttpHost newTarget = new HttpHost(
                    uri.getHost(),
                    uri.getPort(),
                    uri.getScheme());

            HttpGet redirect = new HttpGet(uri);

            HttpRequest orig = request.getOriginal();
            redirect.setHeaders(orig.getAllHeaders());

            RequestWrapper wrapper = new RequestWrapper(redirect);
            wrapper.setParams(params);

            HttpRoute newRoute = determineRoute(newTarget, wrapper, context);
            RoutedRequest newRequest = new RoutedRequest(wrapper, newRoute);

            Log.d(SeaCatInternals.L, "Redirecting to '" + uri + "' via " + newRoute);

            return newRequest;
        }

        CredentialsProvider credsProvider = (CredentialsProvider)
                context.getAttribute(ClientContext.CREDS_PROVIDER);

        if (credsProvider != null && HttpClientParams.isAuthenticating(params)) {

            if (this.targetAuthHandler.isAuthenticationRequested(response, context)) {

                HttpHost target = (HttpHost)
                        context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
                if (target == null) {
                    target = route.getTargetHost();
                }

                Log.d(SeaCatInternals.L, "Target requested authentication");
/*
                Map<String, Header> challenges = this.targetAuthHandler.getChallenges(
                        response, context);
                try {
                    processChallenges(challenges,
                            this.targetAuthState, this.targetAuthHandler,
                            response, context);
                } catch (AuthenticationException ex) {
                    Log.w(SeaCatClient.L, "Authentication error: " +  ex.getMessage());
                }
                updateAuthState(this.targetAuthState, target, credsProvider);
*/

/*
                if (this.targetAuthState.getCredentials() != null) {
                    // Re-try the same request via the same route
                    return roureq;
                } else {
                    return null;
                }
*/
            } else {
                // Reset target auth scope
//                this.targetAuthState.setAuthScope(null);
            }

/*
            if (this.proxyAuthHandler.isAuthenticationRequested(response, context)) {

                Log.d(SeaCatClient.L, "Proxy requested authentication");
                Map<String, Header> challenges = this.proxyAuthHandler.getChallenges(
                        response, context);
                try {
                    processChallenges(challenges,
                            this.proxyAuthState, this.proxyAuthHandler,
                            response, context);
                } catch (AuthenticationException ex) {
                    if (this.log.isWarnEnabled()) {
                        this.log.warn("Authentication error: " +  ex.getMessage());
                        return null;
                    }
                }
                updateAuthState(this.proxyAuthState, proxy, credsProvider);

                if (this.proxyAuthState.getCredentials() != null) {
                    // Re-try the same request via the same route
                    return roureq;
                } else {
                    return null;
                }
            } else {
                // Reset proxy auth scope
                this.proxyAuthState.setAuthScope(null);
            }
*/

        }
        return null;
    } // handleResponse

    /**
     * Shuts down the connection.
     * This method is called from a <code>catch</code> block in
     * {@link #execute execute} during exception handling.
     */
    private void abortConnection()
    {
    }
}
