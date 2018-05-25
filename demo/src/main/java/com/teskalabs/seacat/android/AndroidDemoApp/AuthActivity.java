package com.teskalabs.seacat.android.AndroidDemoApp;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.Toast;

import com.teskalabs.seacat.android.client.SeaCatClient;
import com.teskalabs.seacat.android.client.SeaCatInternals;

public class AuthActivity extends ActionBarActivity {

	private static final int REQUEST_CODE_USER_AUTHORIZED = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_auth);
	}


	@Override
	protected void onResume() {
		super.onResume();

		// Make sure that credentials are configured
		KeyguardManager mKeyguardManager = (KeyguardManager) this.getSystemService(Context.KEYGUARD_SERVICE);
		if (!mKeyguardManager.isKeyguardSecure())
		{
			try {
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
					startActivity(new Intent("android.credentials.UNLOCK"));
				} else {
					startActivity(new Intent("com.android.credentials.UNLOCK"));
				}
			} catch (ActivityNotFoundException e) {
				Log.e(SeaCatInternals.L, "No UNLOCK activity: " + e.getMessage(), e);
			}
		}

		// Authenticate!
		showAuthenticationScreen();
	}


	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private void showAuthenticationScreen()
	{
		KeyguardManager mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);

		// Create the Confirm Credentials screen. You can customize the title and description. Or
		// we will provide a generic one for you if you leave it null
		Intent intent = mKeyguardManager.createConfirmDeviceCredentialIntent(null, null);
		if (intent != null) {
			startActivityForResult(intent, REQUEST_CODE_USER_AUTHORIZED);
		}
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (requestCode == REQUEST_CODE_USER_AUTHORIZED)
		{
			if (resultCode == RESULT_OK) {
				SeaCatClient.startAuth();
				finish();
			} else {
				// The user canceled or didn’t complete the lock screen
				// operation. Go to error/cancellation flow.
				Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show();
			}
		}
	}


	@Override
	public void onBackPressed() {
		// super.onBackPressed(); commented this line in order to disable back press
	}

}
