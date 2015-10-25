package com.teskalabs.seacat.android.client.util;

import java.io.IOException;
import com.teskalabs.seacat.android.client.core.seacatcc;

public final class RC
{
	public static final void checkAndThrowIOException(String message, int rc) throws IOException
	{
		if (rc != seacatcc.RC_OK) throw new IOException(String.format("SeaCat return code %d in %s",rc ,message));
	}
}
