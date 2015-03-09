package mobi.seacat.client.ping;

import mobi.seacat.client.core.seacatcc;

public class Ping
{
	protected int pingId = -1;
	final protected double deadline; //TODO: Add support for deadline (skip&cancel objects that are behind deadline)

	protected Ping()
	{
		deadline = seacatcc.time() + 5.0;
	}
	
	public void pong()
	{
	}

	public void cancel()
	{
	}

	protected void setPingId(int pingId)
	{
		this.pingId = pingId;
	}

	public boolean isExpired(double now)
	{
		return now >= deadline;
	}

}
