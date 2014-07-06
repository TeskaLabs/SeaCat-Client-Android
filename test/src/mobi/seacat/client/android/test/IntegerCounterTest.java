package mobi.seacat.client.android.test;

import android.test.AndroidTestCase;

import mobi.seacat.client.internal.IntegerCounter;

public class IntegerCounterTest extends AndroidTestCase
{
	public void test()
	{
		final IntegerCounter c = new IntegerCounter(77);
		
		assertEquals(77, c.get());
		
		int i = c.getAndAdd(7);
		assertEquals(77, i);
		assertEquals(84, c.get());
	}

}
