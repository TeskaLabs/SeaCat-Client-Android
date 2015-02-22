package mobi.seacat.client.intf;

import java.nio.ByteBuffer;

import mobi.seacat.client.core.Reactor;

public interface ICntlFrameConsumer
{
	boolean receivedControlFrame(Reactor reactor, ByteBuffer frame, int frameVersionType, int frameLength, byte frameFlags);
}
