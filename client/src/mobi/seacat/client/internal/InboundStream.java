package mobi.seacat.client.internal;

import java.io.Closeable;
import java.nio.ByteBuffer;

/*
 * This is interface that can be registered to Reactor as target for incoming (inbound) stream.
 */
public abstract interface InboundStream extends Closeable
{
	public abstract void inboundSynReply(ByteBuffer frame);
	public abstract boolean inboundData(ByteBuffer frame);
	public abstract void inboundRstStream(byte flags, int statusCode);
	public abstract void inboundFIN();

}
