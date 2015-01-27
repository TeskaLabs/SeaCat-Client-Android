package mobi.seacat.client.internal;

/**
 * This is simple "mutable" integer holder.
 * Used e.g. to allow transfer of C Core client return codes from Session thread.
 *
 */
public class Status
{

	private int status;

    public Status(int status) 
    {
         this.status = status;
    }

    public int getStatus()
    {
        return status;
    }

    public void setStatus(int status)
    {
        this.status = status;
    }
	
}
