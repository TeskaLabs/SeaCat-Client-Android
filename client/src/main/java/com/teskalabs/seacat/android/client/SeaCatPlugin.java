package com.teskalabs.seacat.android.client;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;

import com.teskalabs.seacat.android.client.core.seacatcc;
import com.teskalabs.seacat.android.client.util.RC;
import com.teskalabs.seacat.android.client.util.RootUtil;

import java.util.ArrayList;
import java.util.Properties;

import static android.provider.Settings.*;

public abstract class SeaCatPlugin {

	static private ArrayList<SeaCatPlugin> plugins;
	static private boolean characteristicsCommited;

	static {
		plugins = new ArrayList<>();
		characteristicsCommited = false;
	}

	static synchronized void commitCharacteristics(Context context)
	{
		if (characteristicsCommited) throw new RuntimeException("SeaCat characteristics are already comited!");

		ArrayList<String> chrs = new ArrayList<>();
		for (SeaCatPlugin p : plugins) {
			final Properties pchrs = p.getCharacteristics();
			if (pchrs == null) continue;
			for (String name : pchrs.stringPropertyNames())
			{
				chrs.add(String.format("%s\037%s", name, pchrs.getProperty(name)));
			}
		}

		// Add platform characteristics
		chrs.add(String.format("%s\037%s", "plv", Build.VERSION.RELEASE));
		chrs.add(String.format("%s\037%s", "pls", Build.VERSION.SDK_INT));
		chrs.add(String.format("%s\037%s", "pli", Build.VERSION.INCREMENTAL));
		chrs.add(String.format("%s\037%s", "plB", Build.BRAND));
		chrs.add(String.format("%s\037%s", "plf", Build.FINGERPRINT));
		chrs.add(String.format("%s\037%s", "plI", Build.ID));
		chrs.add(String.format("%s\037%s", "plm", Build.MANUFACTURER));
		chrs.add(String.format("%s\037%s", "plM", Build.MODEL));
		chrs.add(String.format("%s\037%s", "plp", Build.PRODUCT));
		chrs.add(String.format("%s\037%s", "plt", Build.TAGS));
		chrs.add(String.format("%s\037%s", "plT", Build.TYPE));
		chrs.add(String.format("%s\037%s", "plU",
			Secure.getString(context.getContentResolver(), Secure.ANDROID_ID)
		));

		// Add hardware characteristics
		chrs.add(String.format("%s\037%s", "hwb", Build.BOARD));
		chrs.add(String.format("%s\037%s", "hwd", Build.DEVICE));
		chrs.add(String.format("%s\037%s", "hwS", Build.SERIAL));

		DisplayMetrics dm = Resources.getSystem().getDisplayMetrics();
		chrs.add(String.format("%s\037%sx%s", "dpr", dm.widthPixels, dm.heightPixels));
		chrs.add(String.format("%s\037%s", "dpi", dm.densityDpi));
		chrs.add(String.format("%s\037%s", "dpden", dm.density));
		chrs.add(String.format("%s\037%s", "dpxdpi", dm.xdpi));
		chrs.add(String.format("%s\037%s", "dpydpi", dm.ydpi));

		String rooted = RootUtil.isRooted(context);
		if (rooted != null)
		{
			chrs.add(String.format("%s\037%s", "ROOTED", rooted));
		}

		//TODO: How to obtain and add version of the SeaCat client for Android?

		try {
			PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			chrs.add(String.format("%s\037%s", "apN", pInfo.versionName));
			chrs.add(String.format("%s\037%s", "apV", pInfo.versionCode));
		} catch (PackageManager.NameNotFoundException e) {
			Log.e(SeaCatInternals.L, "Cannot get package info of the application");
		}

		chrs.add(null);

		String[]chrsarr = new String[chrs.size()];
		chrsarr = chrs.toArray(chrsarr);
		int rc = seacatcc.characteristics_store(chrsarr);
		RC.checkAndLogError("seacatcc.characteristics_store", rc);
		if (rc == 0) characteristicsCommited = true;
	}

	///

	public SeaCatPlugin()
	{
		plugins.add(this);
	}

	abstract public Properties getCharacteristics();

}
