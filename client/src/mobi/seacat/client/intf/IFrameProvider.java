package mobi.seacat.client.intf;

import java.io.IOException;
import java.nio.ByteBuffer;

import mobi.seacat.client.core.Reactor;

public interface IFrameProvider
{
	class Result
	{
		public final ByteBuffer frame;
		public final boolean keep;

		public Result(ByteBuffer frame, boolean keep)
		{
			this.frame = frame;
			this.keep = keep;
		}
	};

	Result buildFrame(Reactor reactor) throws IOException;
	int getFrameProviderPriority();
}
