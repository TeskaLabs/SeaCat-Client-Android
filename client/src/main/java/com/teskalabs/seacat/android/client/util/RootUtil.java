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

	public static String isRooted(Context context) {
		boolean isEmulator = isEmulator(context);
		if (isEmulator) return "emulator";

		String buildTags = Build.TAGS;
		if(buildTags != null && buildTags.contains("test-keys")) {
			return "test-keys";
		}

		File file = new File("/system/app/Superuser.apk");
		if (file.exists()) {
			return "Superuser.apk";
		}

		file = new File("/system/xbin/su");
		if (file.exists()) {
			return "xbin/su";
		}

		return null;
	}
}
