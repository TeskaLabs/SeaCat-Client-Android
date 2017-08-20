package com.teskalabs.seacat.android.client.message;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;

/*
 * Example of use:
 *
 *	try {
 *		mHandler.post(new SeaCatJSONMessageTrigger("message-name") {
 *			@Override
 *			public void onPostExecute() {
 *				Log.i(TAG, "Trigger result:" + this.getResponseBody().toString());
 *			}
 *		}.put("foo", "bar"));
 *	} catch (Exception e) {
 *		Log.e(TAG, "Failed to trigger SeaCat message", e);
 *	}
 *
 */

public class JSONMessageTrigger extends MessageTrigger {

	private static final String TAG = MessageTrigger.class.getName();
	protected JSONObject json;


	public JSONMessageTrigger(String eventName) throws IOException {
		super(eventName);
		json = new JSONObject();
	}

	public JSONMessageTrigger(String URLBase, String eventName) throws IOException {
		super(URLBase, eventName);
		json = new JSONObject();
	}


	public JSONMessageTrigger put(String name, String value) throws JSONException {
		json.put(name, value);
		return this;
	}


	@Override
	protected String getMessageContentType() {
		return "application/json; charset=utf-8";
	}

	@Override
	protected void writeContent(DataOutputStream outputStream) throws IOException {
		outputStream.writeBytes(json.toString());
		outputStream.flush();
	}

}
