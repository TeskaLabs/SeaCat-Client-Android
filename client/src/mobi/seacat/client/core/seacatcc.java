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
	static public native int reset();
	
	static public native void ppkgen_worker();
	
	static public native void csrgen_worker(
		String country, String state, String locality, String organization, String organization_unit,
		String common_name,
		String surname, String given_name,
		String email,
		String san_email, String san_uri
	);

	static public native String cacert_url();
	static public native void cacert_worker(byte[] cacert);

	///
	static public final int RC_OK = (0);
	static public final int RC_E_GENERIC = (-9999);

}
