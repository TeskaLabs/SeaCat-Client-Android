package mobi.seacat.client;

import java.io.IOException;

public class SeaCatClosedException extends IOException
{
	private static final long serialVersionUID = 302766813126931539L;

	public SeaCatClosedException(int statusCode)
	{
		super("SeaCatClosedException: "+statusCode);
	}

}
