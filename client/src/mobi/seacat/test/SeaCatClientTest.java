package mobi.seacat.test;

import static org.junit.Assert.*;

import mobi.seacat.client.SeaCatClient;

import org.junit.BeforeClass;
import org.junit.Test;

public class SeaCatClientTest {

    @BeforeClass
    public static void oneTimeSetUp() throws Exception
    {
    	SeaCatClient.configure();
    }

	@Test
	public void testConfigure() throws Exception
	{
		assertTrue(SeaCatClient.isConfigured());
	}

}
