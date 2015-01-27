package mobi.seacat.client.internal;

public class JNI
{
	static
	{
		System.loadLibrary("seacatjni");
	}

	static public native int seacat_reactor_init(Reactor obj);
	static public native int seacat_reactor_fini();

	static public native int seacat_reactor_run();
	static public native int seacat_reactor_shutdown();
	static public native int seacat_reactor_yield();	
}
