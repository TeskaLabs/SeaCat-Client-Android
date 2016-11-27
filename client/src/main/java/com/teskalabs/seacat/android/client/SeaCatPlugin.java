package com.teskalabs.seacat.android.client;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;

import com.teskalabs.seacat.android.client.core.seacatcc;
import com.teskalabs.seacat.android.client.util.RC;

import java.util.ArrayList;
import java.util.Properties;

import static android.provider.Settings.*;

public abstract class SeaCatPlugin {

	static private ArrayList<SeaCatPlugin> plugins;

	static {
		plugins = new ArrayList<>();
	}

	static void commitCapabilities(Context context)
	{
		ArrayList<String> caps = new ArrayList<>();
		for (SeaCatPlugin p : plugins) {
			final Properties pcaps = p.getCapabilities();
			if (pcaps == null) continue;
			for (String name : pcaps.stringPropertyNames())
			{
				caps.add(String.format("%s\037%s", name, pcaps.getProperty(name)));
			}
		}

		// Add platform capabilities
		caps.add(String.format("%s\037%s", "plv", Build.VERSION.RELEASE));
		caps.add(String.format("%s\037%s", "pls", Build.VERSION.SDK_INT));
		caps.add(String.format("%s\037%s", "pli", Build.VERSION.INCREMENTAL));
		caps.add(String.format("%s\037%s", "plb", Build.BOARD));
		caps.add(String.format("%s\037%s", "plB", Build.BRAND));
		caps.add(String.format("%s\037%s", "pld", Build.DEVICE));
		caps.add(String.format("%s\037%s", "plf", Build.FINGERPRINT));
		caps.add(String.format("%s\037%s", "plI", Build.ID));
		caps.add(String.format("%s\037%s", "plm", Build.MANUFACTURER));
		caps.add(String.format("%s\037%s", "plM", Build.MODEL));
		caps.add(String.format("%s\037%s", "plp", Build.PRODUCT));
		caps.add(String.format("%s\037%s", "plS", Build.SERIAL));
		caps.add(String.format("%s\037%s", "plt", Build.TAGS));
		caps.add(String.format("%s\037%s", "plT", Build.TYPE));

		DisplayMetrics dm = Resources.getSystem().getDisplayMetrics();
		caps.add(String.format("%s\037%sx%s", "dpr", dm.widthPixels, dm.heightPixels));
		caps.add(String.format("%s\037%s", "dpi", dm.densityDpi));
		caps.add(String.format("%s\037%s", "dpden", dm.density));
		caps.add(String.format("%s\037%s", "dpxdpi", dm.xdpi));
		caps.add(String.format("%s\037%s", "dpydpi", dm.ydpi));

		caps.add(String.format("%s\037%s", "uid",
			Secure.getString(context.getContentResolver(), Secure.ANDROID_ID)
		));

		caps.add(null);

		String[] caparr = new String[caps.size()];
		caparr = caps.toArray(caparr);
		int rc = seacatcc.capabilities_store(caparr);
		RC.checkAndLogError("seacatcc.capabilities_store", rc);
	}

	///

	public SeaCatPlugin()
	{
		plugins.add(this);
	}

	abstract public Properties getCapabilities();

}
