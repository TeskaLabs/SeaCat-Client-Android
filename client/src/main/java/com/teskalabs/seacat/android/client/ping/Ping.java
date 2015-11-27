package com.teskalabs.seacat.android.client.ping;

import com.teskalabs.seacat.android.client.core.seacatcc;

public class Ping
{
	protected int pingId = -1;
	final protected double deadline; //TODO: Add support for deadline (skip&cancel objects that are behind deadline)

	protected Ping()
	{
		deadline = seacatcc.time() + 60.0;
	}
	
	public void pong() {}
	public void cancel() { }

	final protected void setPingId(int pingId)
	{
		this.pingId = pingId;
	}
    final public int getPingId() { return pingId; }

	final public boolean isExpired(double now)
	{
		return now >= deadline;
	}

}
