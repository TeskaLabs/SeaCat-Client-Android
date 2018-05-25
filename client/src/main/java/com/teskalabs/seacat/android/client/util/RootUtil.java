package com.teskalabs.seacat.android.client.util;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;

import java.io.File;

public class RootUtil
{

	public static boolean isEmulator(Context context) {
		return "sdk".equals(Build.PRODUCT) || "google_sdk".equals(Build.PRODUCT) || Settings.Secure.getString(context.getContentResolver(), "android_id") == null;
	}

	public static boolean isRooted(Context context) {
		boolean isEmulator = isEmulator(context);
		String buildTags = Build.TAGS;

		if(!isEmulator && buildTags != null && buildTags.contains("test-keys")) {
			return true;
		}

		else
		{
			File file = new File("/system/app/Superuser.apk");
			if(file.exists()) {
				return true;
			} else {
				file = new File("/system/xbin/su");
				return !isEmulator && file.exists();
			}
		}
	}
}
