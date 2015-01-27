package mobi.seacat.client;

import java.io.IOException;

public class SeaCatIOException extends IOException
{
	private static final long serialVersionUID = -9032983990006025810L;

	protected SeaCatIOException(String detailMessage)
    {
        super(detailMessage);
    }

	static public SeaCatIOException create(int errorCode)
	{
		final String reason;
		
		if (errorCode > -9000)
		{
			reason = String.format("SeaCatIOException: not realy an error (%d)", errorCode);
		}

		else if ((errorCode >= -9399) && (errorCode <= -9000))
		{
			reason = String.format("SeaCatIOException: errno (%d)", -(errorCode+9000));
		}

		else if ((errorCode >= -9600) && (errorCode <= -9799))
		{
			reason = String.format("SeaCatIOException: eia error (%d)", -(errorCode+9600));
		}

		else if ((errorCode >= -9800) && (errorCode <= -9899))
		{
			reason = String.format("SeaCatIOException: ssl error (%d)", -(errorCode+9800));
		}

		else if (errorCode == 9901)
		{
			reason = "SeaCatIOException: not configured";
		}

		else if (errorCode == 9902)
		{
			reason = "SeaCatIOException: not connected";
		}

		else if (errorCode == 9903)
		{
			reason = "SeaCatIOException: frame too small";
		}

		else
		{
			reason = String.format("SeaCatIOException (%d)", errorCode);
		}

		return new SeaCatIOException(reason);
	}
	
}
