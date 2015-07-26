package mobi.seacat.client.core;

public final class seacatcc
{
	static
	{
		System.loadLibrary("seacatjni");
	}
	
	static public native int init(String applicationId, String varDirectory, Reactor reactor);
	static public native int run();
	static public native int shutdown();
	static public native int yield(char what);
    static public native String state();

	static public native void ppkgen_worker();
	
	static public native int csrgen_worker(String[] params);

	static public native String cacert_url();
	static public native void cacert_worker(byte[] cacert);

	// This is thread-safe (but quite expensive) method to obtain current time in format used by SeaCatCC event loop
	static public native double time();
	
	///
	static public final int RC_OK = (0);
	static public final int RC_E_GENERIC = (-9999);

}
