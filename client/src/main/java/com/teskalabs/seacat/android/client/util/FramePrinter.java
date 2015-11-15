package com.teskalabs.seacat.android.client.util;

import com.teskalabs.seacat.android.client.core.SPDY;

import java.nio.ByteBuffer;

public class FramePrinter
{
    static public String frameToString(ByteBuffer frame)
    {
        if (frame == null) return "[null]";

        byte fb = frame.get(0);
        if ((fb & (1L << 7)) != 0)
        {
            return controlFrameToString(frame);
        }

        else
        {
            return dataFrameToString(frame);
        }
    }

    private static String controlFrameToString(ByteBuffer frame) {
        //TODO: This ...
        return "[C??]";
    }


    private static String dataFrameToString(ByteBuffer frame) {
        int streamId = frame.getInt(0);
        int frameLength = frame.getInt(4);
        byte frameFlags = (byte)(frameLength >> 24);
        frameLength &= 0xffffff;

        return String.format("[D %d %d%s]",
                streamId,
                frameLength,
                ((frameFlags & SPDY.FLAG_FIN) == SPDY.FLAG_FIN) ? " FIN": ""
        );
    }

}
