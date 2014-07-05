package mobi.seacat.client;

import java.io.IOException;

public class SeaCatIOException extends IOException
{
	private static final long serialVersionUID = -9032983990006025810L;

	public SeaCatIOException(int errorCode)
    {
        super(String.format("SeaCat IOException %d", errorCode));
    }
}
