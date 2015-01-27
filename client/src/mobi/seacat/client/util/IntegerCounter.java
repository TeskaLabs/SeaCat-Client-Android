package mobi.seacat.client.util;

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

    public void set(int value)
    {
    	counter = value;
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
