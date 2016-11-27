package com.teskalabs.seacat.android.client.core;

public final class seacatcc
{
    static
    {
        System.loadLibrary("seacatjni");
    }

    static public native int init(String applicationId, String applicationIdSuffix, String varDirectory, Reactor reactor);
    static public native int run();
    static public native int shutdown();
    static public native int yield(char what);
    static public native String state();

    static public native void ppkgen_worker();

    static public native int csrgen_worker(String[] params);

    static public native int set_proxy_server_worker(String proxy_host, String proxy_port);

    // This is thread-safe (but quite expensive) method to obtain current time in format used by SeaCatCC event loop
    static public native double time();

    static public native int log_set_mask(long bitmask);

    static public native int socket_configure_worker(int port, char domain, char type, int protocol, String peer_address, String peer_port);

    static public native String client_id();
    static public native String client_tag();

    static public native int capabilities_store(String[] capabilities);
    ///
    static public final int RC_OK = (0);
    static public final int RC_E_GENERIC = (-9999);

}
