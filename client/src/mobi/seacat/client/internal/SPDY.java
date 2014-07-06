package mobi.seacat.client.internal;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Date;

import mobi.seacat.client.okhttp.Headers;
import mobi.seacat.client.okhttp.HttpDate;

public class SPDY
{
	final static public int HEADER_SIZE = 8;

	final static public short CNTL_FRAME_VERSION_SPD3 = 0x03;
	final static public short CNTL_FRAME_VERSION_ALX1 = 0xA1;

	final static public short CNTL_TYPE_SYN_STREAM = 1;
	final static public short CNTL_TYPE_SYN_REPLY = 2;
	final static public short CNTL_TYPE_RST_STREAM = 3;
	final static public short CNTL_TYPE_PING = 6;

	final static public short CNTL_TYPE_STATS_REQ = 0xA1;
	final static public short _CNTL_TYPE_STATS_REP = 0xA2;

	final static public short CNTL_TYPE_CSR = 0xC1;
	final static public short CNTL_TYPE_CERT_QUERY = 0xC2;
	final static public short CNTL_TYPE_CERT = 0xC3;
	
	public static final byte FLAG_FIN = (byte) 0x01;
	public static final byte FLAG_UNIDIRECTIONAL = (byte) 0x02;
	public static final byte FLAG_CSR_NOT_FOUND = (byte) 0x80;

	
	///
	
	public static void buildSPD3Ping(ByteBuffer frame, int pingId)
	{
		// It is SPDY v3 control frame 
		frame.putShort((short) (0x8000 | CNTL_FRAME_VERSION_SPD3));

		// Type
		frame.putShort(CNTL_TYPE_PING);

		// Flags and length
		frame.putInt(4);

		// Ping ID
		frame.putInt(pingId);
	}

	public static void buildALX1SynStream(ByteBuffer buffer, int streamId, HttpURLConnectionImpl connection, boolean fin_flag, int priority)
	{
		assert((streamId & 0x80000000) == 0);
		
		buffer.putShort((short) (0x8000 | CNTL_FRAME_VERSION_ALX1));
		buffer.putShort(CNTL_TYPE_SYN_STREAM); // Type
		buffer.putInt(0x04030201);                  // Flags and length (placeholder)
		buffer.putInt(streamId);                    // Stream ID
		buffer.putInt(0);                           // Associated-To-Stream-ID - not used
		buffer.put((byte)((priority & 0x07)<<5));   // Priority
		buffer.put((byte)0x00);                     // Slot (reserved)
		
		assert buffer.position() == 18;

		// Host (without .seacat)
		final URL url = connection.getURL();
		String host = url.getHost();
		final int lastPeriodPos = host.lastIndexOf('.');
		if (lastPeriodPos > 0) host = host.substring(0, lastPeriodPos);
		appendVLEString(buffer, host);

		appendVLEString(buffer, connection.getRequestMethod());
		appendVLEString(buffer, url.getPath());

		// Add headers
		appendVLEString(buffer, "X-Seacat-Client");
		//TODO: appendVLEString(buffer, isAndroid ? "android" : "java");
		appendVLEString(buffer, "java");

		// Add If-Modified-Since header
		long ifModifiedSince = connection.getIfModifiedSince();
		if (ifModifiedSince != 0)
		{
			appendVLEString(buffer, "If-Modified-Since");
			appendVLEString(buffer, HttpDate.format(new Date(ifModifiedSince)));
		}

		Headers headers = connection.getRequestHeaders();		
		for (int i = 0; i < headers.size(); i++)
		{
			String header = headers.name(i);
			if (header == null) continue;
			
			String value = headers.value(i);
			if (value == null) continue;

			//TODO: Do some filtering (if needed)
			appendVLEString(buffer, header);
			appendVLEString(buffer, value);
		}

		// Update length entry
		int flagLength = buffer.position() - HEADER_SIZE;
		assert flagLength < 0x01000000;
		flagLength |= (fin_flag ? FLAG_FIN : 0) << 24;
		buffer.putInt(4, flagLength); // Update length of frame
	}

	///

	private static void appendVLEString(ByteBuffer buffer, String text)
	{
		byte[] bytes;
		try
		{
			bytes = text.getBytes("UTF-8");
		}		
		catch (UnsupportedEncodingException e)
		{
			bytes = new byte[] {'?', '?', '?'};
		}

		assert bytes.length <= 0xFFFF;

		// Append length
		if (bytes.length >= 0xFA)
		{
			buffer.put((byte)0xFF);
			buffer.putShort((short) bytes.length);
		}
		else
		{
			buffer.put((byte) bytes.length);
		}

		buffer.put(bytes);
	}

	///

	static String parseVLEString(ByteBuffer buffer)
	{
		//TODO: Check and correctly handle signed values!
		int length = buffer.get();
		if (length == 0xFF) length = buffer.getShort();

		assert length >= 0;
		
		byte[] bytes = new byte[length];
		buffer.get(bytes, 0, length);
		
		try
		{
			return new String(bytes, "UTF-8");
		}
		
		catch (UnsupportedEncodingException e)
		{
			return "???";
		}
	}
	
}
