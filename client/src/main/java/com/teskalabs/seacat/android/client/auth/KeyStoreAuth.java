package com.teskalabs.seacat.android.client.auth;

import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.teskalabs.seacat.android.client.SeaCatClient;
import com.teskalabs.seacat.android.client.SeaCatInternals;
import com.teskalabs.seacat.android.client.core.Reactor;
import com.teskalabs.seacat.android.client.core.seacatcc;
import com.teskalabs.seacat.android.client.util.RSAKeyPair;

import java.security.GeneralSecurityException;
import java.util.Calendar;
import java.util.Date;

public class KeyStoreAuth
{

	private final RSAKeyPair keypair;

	public KeyStoreAuth()
	{
		keypair = new RSAKeyPair("SeaCatMasterKey");
	}


	public void startAuth(Reactor reactor) {

		boolean requireUserAuth = false;
		int masterKeyLength = 2048;

		Bundle bundle = null;
		try {
			ApplicationInfo ai = reactor.getPackageManager().getApplicationInfo(reactor.getPackageName(), PackageManager.GET_META_DATA);
			bundle = ai.metaData;
		} catch (PackageManager.NameNotFoundException|NullPointerException e) {

		}
		if (bundle != null) {
			requireUserAuth = bundle.getBoolean("seacat.require_user_auth", requireUserAuth);
			masterKeyLength = bundle.getInt("seacat.master_key_length", masterKeyLength);
		}

		if (!keypair.exists())
		{
			try {
				keypair.discard();
			} catch (GeneralSecurityException e) {
				// No-op
			}

			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.MILLISECOND, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			final Date valid_from = cal.getTime();
			cal.add(Calendar.YEAR, 200);
			final Date valid_to = cal.getTime();

			if (requireUserAuth) {
				if (!isScreenSecure(reactor)) {
					reactor.sendBroadcast(SeaCatInternals.createIntent(SeaCatClient.ACTION_SEACAT_SECURE_LOCK_NEEDED));
					return;
				}
			}

			try {
				keypair.generate(reactor, masterKeyLength, "CN=SeaCatAuthKey", valid_from, valid_to, 1, requireUserAuth);
			} catch (GeneralSecurityException e) {
				Log.e(SeaCatInternals.L, "Failed to generate SeaCat authorization key.", e);
				return;
			}

			// Do seacat client reset?
		}

		byte[] auth_key;
		try {
			auth_key = keypair.derive("SeaCatAuthKey", 32);
		} catch (SeaCatUserNotAuthenticatedException e) {
			Log.w(SeaCatInternals.L, "User is not authenticated");
			reactor.sendBroadcast(SeaCatInternals.createIntent(SeaCatClient.ACTION_SEACAT_USER_AUTH_NEEDED));
			return;
		} catch (GeneralSecurityException e) {
			Log.e(SeaCatInternals.L, "Failed to obtain SeaCat authorization key.", e);
			return;
		}
		if (auth_key == null)
		{
			Log.e(SeaCatInternals.L, "Failed to obtain SeaCat authorization key.");
			return;
		}
		seacatcc.secret_key_worker(auth_key);
	}


	public void deauth()
	{
		seacatcc.secret_key_worker(null);
	}

	public void reset()
	{
		try {
			keypair.discard();
		} catch (GeneralSecurityException e) {
			Log.e(SeaCatInternals.L, "Failed to discard SeaCat authorization key.");
		}
	}


	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private static boolean isScreenSecure(Context context)
	{
		KeyguardManager mKeyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
		return mKeyguardManager.isKeyguardSecure();
	}


}
