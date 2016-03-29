package com.teskalabs.seacat.android.client.util;

import android.util.Log;

import java.io.IOException;

import com.teskalabs.seacat.android.client.SeaCatInternals;
import com.teskalabs.seacat.android.client.core.seacatcc;

public final class RC
{
    public static final void checkAndThrowIOException(String message, int rc) throws IOException
    {
        if (rc != seacatcc.RC_OK) throw new IOException(String.format("SeaCat return code %d in %s",rc ,message));
    }

    public static final void checkAndLogError(String message, int rc)
    {
        if (rc != seacatcc.RC_OK) Log.e(SeaCatInternals.L, String.format("SeaCat return code %d in %s", rc, message));
    }

}
