package com.teskalabs.seacat.android.client.core;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;

public class CACertWorker implements Runnable
{
	
	@Override
	public void run()
	{
		ByteArrayOutputStream cert = new ByteArrayOutputStream();

		try
		{
			URL url = new URL(seacatcc.cacert_url());

			BufferedInputStream in = new BufferedInputStream(url.openStream());

			byte contents[] = new byte[4096];
			int bytesRead=0;

			while( (bytesRead = in.read(contents)) != -1)
			{ 
				cert.write(contents, 0, bytesRead);               
			}
		}
		
		catch (Exception e)
		{
			e.printStackTrace();
		}

		seacatcc.cacert_worker(cert.toByteArray());
	}

}
