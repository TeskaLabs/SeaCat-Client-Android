package com.teskalabs.seacat.android.client.intf;

import java.nio.ByteBuffer;

import com.teskalabs.seacat.android.client.core.Reactor;

public interface ICntlFrameConsumer
{
	boolean receivedControlFrame(Reactor reactor, ByteBuffer frame, int frameVersionType, int frameLength, byte frameFlags);
}
