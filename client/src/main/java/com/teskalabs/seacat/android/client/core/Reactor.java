package com.teskalabs.seacat.android.client.core;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.teskalabs.seacat.android.client.SeaCatClient;
import com.teskalabs.seacat.android.client.SeaCatInternals;
import com.teskalabs.seacat.android.client.intf.ICntlFrameConsumer;
import com.teskalabs.seacat.android.client.intf.IFrameProvider;
import com.teskalabs.seacat.android.client.ping.PingFactory;
import com.teskalabs.seacat.android.client.util.RC;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

//TODO: Replace print by Android logging (done in this file, check others)

public class Reactor extends ContextWrapper
{
	final public FramePool framePool = new FramePool();
	final private Thread ccoreThread;

	final Lock eventLoopNotStartedlock = new ReentrantLock();
	final Condition eventLoopStartedCond = eventLoopNotStartedlock.newCondition();
	private boolean eventLoopStarted = false;

	final private Executor workerExecutor;

	final public PingFactory pingFactory;
	final public StreamFactory streamFactory;
	
	final private Map<Integer, ICntlFrameConsumer> cntlFrameConsumers = new HashMap<Integer, ICntlFrameConsumer>();
	final private BlockingQueue<IFrameProvider> frameProviders;

    private String lastState;

	private String clientId = "ANONYMOUS_CLIENT";
	private String clientTag = "[ANONYMOUS0CLIENT]";

	///
	
	public Reactor(Context context) throws IOException
	{
		super(context);

		this.ccoreThread = new Thread(new Runnable() { public void run() { Reactor._run(); }});
		this.ccoreThread.setName("SeaCatCCoreThread");
		this.ccoreThread.setDaemon(true);
		this.workerExecutor = new ThreadPoolExecutor(0, 1000, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

        java.io.File vardir = this.getDir("seacat", Context.MODE_PRIVATE);

		int rc = seacatcc.init(this.getPackageName(), SeaCatInternals.applicationIdSuffix, vardir.getAbsolutePath(), this);
		RC.checkAndThrowIOException("seacatcc.init", rc);

        lastState = seacatcc.state();

		// Setup frame provider priority queue
		Comparator<IFrameProvider> frameProvidersComparator = new Comparator<IFrameProvider>()
		{
			@Override
			public int compare(IFrameProvider p1, IFrameProvider p2)
			{
				int p1pri = p1.getFrameProviderPriority();
				int p2pri = p2.getFrameProviderPriority();
				if (p1pri < p2pri) return -1;
				else if (p1pri == p2pri) return 0;
				else return 1;
			}
		};
		frameProviders = new PriorityBlockingQueue<>(11, frameProvidersComparator);


		// Create and register stream factory as control frame consumer
		streamFactory = new StreamFactory();
		cntlFrameConsumers.put(SPDY.buildFrameVersionType(SPDY.CNTL_FRAME_VERSION_ALX1, SPDY.CNTL_TYPE_SYN_REPLY), streamFactory);
		cntlFrameConsumers.put(SPDY.buildFrameVersionType(SPDY.CNTL_FRAME_VERSION_SPD3, SPDY.CNTL_TYPE_RST_STREAM), streamFactory);
		
		
		// Create and register ping factory as control frame consumer
		pingFactory = new PingFactory();
		cntlFrameConsumers.put(SPDY.buildFrameVersionType(SPDY.CNTL_FRAME_VERSION_SPD3, SPDY.CNTL_TYPE_PING), pingFactory);

		// Start reactor thread
		ccoreThread.start();

		eventLoopNotStartedlock.lock();
		while (!eventLoopStarted)
		{
			eventLoopStartedCond.awaitUninterruptibly();
		}
		eventLoopNotStartedlock.unlock();
	}

	// TODO: 26/11/2016 This one is never used ... how and when to shutdown on Android?
	public void shutdown() throws IOException
	{
		int rc = seacatcc.shutdown();
		RC.checkAndThrowIOException("seacatcc.shutdown", rc);
		
		while (true)
		{
			try {
				ccoreThread.join(5000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				continue;
			}
			
			if (ccoreThread.isAlive())
			{
				throw new IOException(String.format("%s is still alive", this.ccoreThread.getName()));
			}
			
			break;
		}
	}


	private static void _run()
	{
		int rc = seacatcc.run();
		if (rc != seacatcc.RC_OK)
            Log.e(SeaCatInternals.L, String.format("return code %d in %s",rc ,"seacatcc.run"));
	}
	
	///


	public void registerFrameProvider(IFrameProvider provider, boolean single) throws IOException
	{
		synchronized (frameProviders)
		{
			if ((single) && (frameProviders.contains(provider))) return;

			frameProviders.add(provider);
		}

		// Yield to C-Core that we have frame to send
		int rc = seacatcc.yield('W');
		if ((rc > 7900) && (rc < 8000))
		{
			Log.w(SeaCatInternals.L, String.format("return code %d in %s",rc ,"seacatcc.yield"));
			rc = seacatcc.RC_OK;
		}
		RC.checkAndThrowIOException("seacatcc.yield", rc);
	}


	///
	
	protected ByteBuffer JNICallbackWriteReady()
	{	
		try
		{
			ByteBuffer frame =  null;
			Vector<IFrameProvider> providersToKeep = new Vector<>();
			
			synchronized (frameProviders)
			{
				while (frame == null)
				{
					IFrameProvider provider = frameProviders.poll();
					if (provider == null) break;
		
					IFrameProvider.Result res = provider.buildFrame(this);
					frame = res.frame;
		
					if (res.keep) providersToKeep.add(provider);
				}
				
				if (!providersToKeep.isEmpty()) frameProviders.addAll(providersToKeep);
			}
				
			if (frame != null) frame.flip();
			return frame;
		}
		
		catch (Exception e)
		{
            Log.e(SeaCatInternals.L, "JNICallbackWriteReady:", e);
			return null;
		}
	}


	protected ByteBuffer JNICallbackReadReady()
	{
		try
		{
			return framePool.borrow("Reactor.JNICallbackReadReady");
		}
		
		catch (Exception e)
		{
            Log.e(SeaCatInternals.L, "JNICallbackReadReady:", e);
			return null;
		}
	}

	
	protected void JNICallbackFrameReceived(ByteBuffer frame, int frame_len)
	{
		int pos = frame.position();
		frame.position(pos + frame_len);
		frame.flip();

		byte fb = frame.get(0);
		boolean giveBackFrame = true;

		try
		{
 			if ((fb & (1L << 7)) != 0)
 			{
				giveBackFrame = receivedControlFrame(frame);
 			}
			
			else
			{
				giveBackFrame = streamFactory.receivedDataFrame(this, frame);
			}
				
		}
		
		catch (Exception e)
		{
            Log.e(SeaCatInternals.L, "JNICallbackFrameReceived:", e);
			giveBackFrame = true;
		}
		
		finally
		{
			if (giveBackFrame) framePool.giveBack(frame);
		}
	}


	protected void JNICallbackFrameReturn(ByteBuffer frame)
	{
		framePool.giveBack(frame);
	}

	
	protected void JNICallbackWorkerRequest(char workerCode)
	{
		switch (workerCode)
		{
			case 'P':
				workerExecutor.execute(new Runnable()
				{
					public void run()
					{
						seacatcc.ppkgen_worker();
					}
				});
				break;


			case 'C': {
                Runnable CSRWorker = SeaCatInternals.getCSRWorker();
                if (CSRWorker != null) workerExecutor.execute(CSRWorker);

                Intent intent = SeaCatInternals.createIntent(SeaCatClient.ACTION_SEACAT_CSR_NEEDED);
                this.sendBroadcast(intent);

                break;
            }

			default:
                Log.w(SeaCatInternals.L, "Unknown worker requested: " + workerCode);
		}
	}

	protected double JNICallbackEvLoopHeartBeat(double now)
	{
		// This method is called periodically from event loop (period is fairly arbitrary)
		// Return value of this method represent the longest time when it should be called again
		// It will very likely be called in shorter period too (as a result of heart beat triggered by other events)

		pingFactory.heartBeat(now);
		framePool.heartBeat(now);

		// TODO: 26/11/2016 Find the best sleeping interval, can be much longer that 5 seconds, I guess
		return 5.0; // Seconds
	}

	protected void JNICallbackEvLoopStarted()
	{
		eventLoopNotStartedlock.lock();
		eventLoopStarted = true;
		eventLoopStartedCond.signalAll();
		eventLoopNotStartedlock.unlock();

        this.sendBroadcast(SeaCatInternals.createIntent(SeaCatClient.ACTION_SEACAT_EVLOOP_STARTED));
	}

	protected void JNICallbackGWConnConnected()
	{
        this.sendBroadcast(SeaCatInternals.createIntent(SeaCatClient.ACTION_SEACAT_GWCONN_CONNECTED));
	}

    protected void JNICallbackGWConnReset()
    {
        pingFactory.reset();
        streamFactory.reset();
        this.sendBroadcast(SeaCatInternals.createIntent(SeaCatClient.ACTION_SEACAT_GWCONN_RESET));
    }

    protected void JNICallbackStateChanged(String state)
    {
        if (SeaCatInternals.logDebug) Log.d(SeaCatInternals.L, "State changed to "+state);

        Intent intent = SeaCatInternals.createIntent(SeaCatClient.ACTION_SEACAT_STATE_CHANGED);
        intent.putExtra(SeaCatClient.EXTRA_STATE, state);
        intent.putExtra(SeaCatClient.EXTRA_PREV_STATE, lastState);
        this.sendBroadcast(intent);

        if ((lastState.charAt(0) != 'C') && (state.charAt(0) == 'C'))
        {
            configureProxyServer();
        }

        lastState = state;
    }

	protected void JNICallbackClientIdChanged(String clientId, String clientTag)
	{
		this.clientId = clientId;
		this.clientTag = clientTag;

		Intent intent = SeaCatInternals.createIntent(SeaCatClient.ACTION_SEACAT_CLIENTID_CHANGED);
		intent.putExtra(SeaCatClient.EXTRA_CLIENT_ID, this.clientId);
		intent.putExtra(SeaCatClient.EXTRA_CLIENT_TAG, this.clientTag);
		this.sendBroadcast(intent);
	}

    public void broadcastState()
    {
        Intent intent = SeaCatInternals.createIntent(SeaCatClient.ACTION_SEACAT_STATE_CHANGED);
        intent.putExtra(SeaCatClient.EXTRA_STATE, seacatcc.state());
        intent.putExtra(SeaCatClient.EXTRA_PREV_STATE, lastState);
        this.sendBroadcast(intent);
    }

    ///

	private boolean receivedControlFrame(ByteBuffer frame)
	{
		int frameVersionType = frame.getInt() & 0x7fffffff;
		
		int frameLength = frame.getInt();
		byte frameFlags = (byte)(frameLength >> 24);
		frameLength &= 0xffffff;

		if (frameLength + SPDY.HEADER_SIZE != frame.limit())
		{
            Log.w(SeaCatInternals.L, String.format("Incorrect frame received: %d %x %d %x - closing connection", frame.limit(), frameVersionType, frameLength, frameFlags));

			// Invalid frame received -> disconnect from a gateway
			seacatcc.yield('d');
			return true;
		}
		
		ICntlFrameConsumer consumer = cntlFrameConsumers.get(frameVersionType);
		if (consumer == null)
		{
            Log.w(SeaCatInternals.L, String.format("Unidentified Control frame received: %d %x %d %x", frame.limit(), frameVersionType, frameLength, frameFlags));
			return true;			
		}
		
		return consumer.receivedControlFrame(this, frame, frameVersionType, frameLength, frameFlags);
	}


    protected void configureProxyServer()
    {
        SharedPreferences sp = this.getSharedPreferences(SeaCatInternals.SeaCatPreferences, Context.MODE_PRIVATE);

        String proxy_host = sp.getString("HTTPSProxyHost", "");
        String proxy_port = sp.getString("HTTPSProxyPort", "");

        if (proxy_host.isEmpty())
        {
            proxy_host = System.getProperty("https.proxyHost", "");
            proxy_port = System.getProperty("https.proxyPort", "");
            //String proxy_user = System.getProperty("https.proxyUser", "");
            //String proxy_password = System.getProperty("https.proxyPassword", "");
        }

        int rc = seacatcc.set_proxy_server_worker(proxy_host, proxy_port);
        RC.checkAndLogError("seacatcc.set_proxy_server_worker", rc);
    }

	///

	public String getClientTag()
	{
		return this.clientTag;
	}

	public String getClientId()
	{
		return this.clientId;
	}

}
