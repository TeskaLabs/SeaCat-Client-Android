package mobi.seacat.client.core;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.HashMap;
import java.util.Comparator;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import mobi.seacat.client.SeaCatClient;
import mobi.seacat.client.intf.*;
import mobi.seacat.client.ping.PingFactory;
import mobi.seacat.client.util.RC;

//TODO: Replace print by Android logging (done in this file, check others)

public class Reactor
{
	final public FramePool framePool = new FramePool();
	final private Thread sessionThread;
	
	final private Executor workerExecutor;
	final private BlockingQueue<Runnable> workerQueue = new LinkedBlockingQueue<Runnable>();
	
	final public PingFactory pingFactory;
	final public StreamFactory streamFactory;
	
	final private Map<Integer, ICntlFrameConsumer> cntlFrameConsumers = new HashMap<Integer, ICntlFrameConsumer>();
	final private BlockingQueue<IFrameProvider> frameProviders;

    final private Context context;

    final private Object stateLock = new Object();
    private String state = null; // Latch that contain last reported state of SeaCat C-Core

	///
	
	public Reactor(Context context) throws IOException
	{
        this.context = context;

		this.sessionThread = new Thread(new Runnable() { public void run() { Reactor.run(); }});
		this.sessionThread.setName("SeaCatReactorThread");
		this.sessionThread.setDaemon(true);
		this.workerExecutor = new ThreadPoolExecutor(0, 1000, 5, TimeUnit.SECONDS, workerQueue);

        java.io.File vardir = context.getDir("seacat", Context.MODE_PRIVATE);

		int rc = seacatcc.init(context.getPackageName(), vardir.getAbsolutePath(), this);
		RC.checkAndThrowIOException("seacatcc.init", rc);


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
		frameProviders = new PriorityBlockingQueue<IFrameProvider>(11, frameProvidersComparator);


		// Create and register stream factory as control frame consumer
		streamFactory = new StreamFactory();
		cntlFrameConsumers.put(SPDY.buildFrameVersionType(SPDY.CNTL_FRAME_VERSION_ALX1, SPDY.CNTL_TYPE_SYN_REPLY), streamFactory);
		cntlFrameConsumers.put(SPDY.buildFrameVersionType(SPDY.CNTL_FRAME_VERSION_SPD3, SPDY.CNTL_TYPE_RST_STREAM), streamFactory);
		
		
		// Create and register ping factory as control frame consumer
		pingFactory = new PingFactory();
		cntlFrameConsumers.put(SPDY.buildFrameVersionType(SPDY.CNTL_FRAME_VERSION_SPD3, SPDY.CNTL_TYPE_PING), pingFactory);
	}

	
	public void start()
	{
		this.sessionThread.start();
	}

    public boolean isStarted() { return this.sessionThread.isAlive(); }


	public void shutdown() throws IOException
	{
		int rc = seacatcc.shutdown();
		RC.checkAndThrowIOException("seacatcc.shutdown", rc);
		
		while (true)
		{
			try {
				this.sessionThread.join(5000);
			} catch (InterruptedException e) {
				continue;
			}
			
			if (this.sessionThread.isAlive())
			{
				throw new IOException(String.format("%s is still alive", this.sessionThread.getName()));
			}
			
			break;
		}
	}


	private static void run()
	{
		int rc = seacatcc.run();
		if (rc != seacatcc.RC_OK)
            Log.e(SeaCatClient.L, String.format("return code %d in %s",rc ,"seacatcc.run"));
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
            Log.w(SeaCatClient.L, String.format("return code %d in %s",rc ,"seacatcc.yield"));
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
			Vector<IFrameProvider> providersToKeep = new Vector<IFrameProvider>(); 
			
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
            Log.e(SeaCatClient.L, "JNICallbackWriteReady:", e);
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
            Log.e(SeaCatClient.L, "JNICallbackReadReady:", e);
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
            Log.e(SeaCatClient.L, "JNICallbackFrameReceived:", e);
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


			case 'C':
				workerExecutor.execute(new Runnable()
				{
					public void run()
					{
						//TODO: Real values
						String[] params = {
							"C", "CZ",
							"ST", "Czech-Republic",
							"L", "Prague",
							"O", "TeskaLabs",
							"OU", "SeaCat",
							"CN", "client-java.test.Main",
							"SN", "John",
							"GN", "Doe",
							"emailAddress", "john.doe@example.com"
						};
						seacatcc.csrgen_worker(params);
					}
				});
				break;

				
			case 'R':
				workerExecutor.execute(new CACertWorker());
				break;
				

			default:
                Log.w(SeaCatClient.L, "Unknown Worker Request: " + workerCode);
		}
	}

	protected double JNICallbackEvLoopHeartBeat(double now)
	{
		// This method is called periodically from event loop (period is fairly arbitrary)
		// Return value of this method represent the longest time when it should be called again
		// It will very likely be called in shorter period too (as a result of heart beat triggered by other events)

		pingFactory.heartBeat(now);
		
		return 5.0; // Seconds
	}

	protected void JNICallbackEvLoopStarted()
	{
        context.sendBroadcast(SeaCatClient.createIntent(SeaCatClient.ACTION_SEACAT_EVLOOP_STARTED));
	}

	protected void JNICallbackGWConnConnected()
	{
        context.sendBroadcast(SeaCatClient.createIntent(SeaCatClient.ACTION_SEACAT_GWCONN_CONNECTED));
	}

    protected void JNICallbackGWConnReset()
    {
        pingFactory.reset();
        streamFactory.reset();
        context.sendBroadcast(SeaCatClient.createIntent(SeaCatClient.ACTION_SEACAT_GWCONN_RESET));
    }

    protected void JNICallbackStateChanged(String state)
    {
        synchronized (stateLock) {
            this.state = state;
        }

        Intent intent = SeaCatClient.createIntent(SeaCatClient.ACTION_SEACAT_STATE_CHANGED);
        intent.putExtra(SeaCatClient.EXTRA_STATE, state);
//        Log.d(SeaCatClient.L, "Broadcasting: " + intent);
        context.sendBroadcast(intent);
    }

    public void broadcastState()
    {
        Intent intent = SeaCatClient.createIntent(SeaCatClient.ACTION_SEACAT_STATE_CHANGED);
        intent.putExtra(SeaCatClient.EXTRA_STATE, getState());
//        Log.d(SeaCatClient.L, "Broadcasting: " + intent);
        context.sendBroadcast(intent);
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
            Log.w(SeaCatClient.L, String.format("Incorrect frame received: %d %x %d %x - closing connection", frame.limit(), frameVersionType, frameLength, frameFlags));

			// Invalid frame received - shutdown a reactor (disconnect) ...
			try
			{
				shutdown();
			}
			catch (Exception e)
			{
                Log.e(SeaCatClient.L, "receivedControlFrame:", e);
			}
			return true;
		}
		
		ICntlFrameConsumer consumer = cntlFrameConsumers.get(frameVersionType);
		if (consumer == null)
		{
            Log.w(SeaCatClient.L, String.format("Unidentified Control frame received: %d %x %d %x", frame.limit(), frameVersionType, frameLength, frameFlags));
			return true;			
		}
		
		return consumer.receivedControlFrame(this, frame, frameVersionType, frameLength, frameFlags);
	}

    ///

    public String getState()
    {
        synchronized (stateLock) {
            return this.state;
        }
    }

}
