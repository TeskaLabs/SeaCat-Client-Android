package mobi.seacat.client.internal;

/*
 * Simple un-synchronized counter object
 */
public class IntegerCounter
{
	private int counter;

    public IntegerCounter(int start)
    {
        this.counter = start;
    }

    public String toString()
    {
        return "[Counter counter=" + counter + "]";
    }

    public int get()
    {
        return counter;
    }

    public int getAndAdd(int increment)
    {
    	int ret = counter;
        counter += increment;
        return ret;
    }

}
