package com.teskalabs.seacat.android.client;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SeaCatEvent implements Runnable {

	private static final String TAG = SeaCatEvent.class.getName();
	private final URL url;
	private final JSONObject json = new JSONObject();

	private static final String eventsAPIUrlBase = "https://api.seacat/event/trigger/";

	public SeaCatEvent(String eventName) throws IOException {
		url = new URL(eventsAPIUrlBase +  eventName);
	}

	public SeaCatEvent(String URLBase, String eventName) throws IOException {
		url = new URL(URLBase +  eventName);
	}

	public SeaCatEvent put(String name, String value) throws JSONException {
		json.put(name, value);
		return this;
	}

	@Override
	public void run() {
		try {
			HttpURLConnection conn = SeaCatClient.open(url);

			conn.setRequestMethod("PUT");
			conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
			conn.setDoOutput(true);

			DataOutputStream outputStream = new DataOutputStream(conn.getOutputStream());
			outputStream.writeBytes(json.toString());
			outputStream.flush();
			outputStream.close();

			InputStream is = conn.getInputStream();
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			int nRead;
			byte[] data = new byte[1024];
			while ((nRead = is.read(data, 0, data.length)) != -1) {
				buffer.write(data, 0, nRead);
			}
			buffer.flush();
			Log.i(TAG, "triggerEvent: " + buffer.toString("UTF-8") + "\n");

		} catch (IOException e) {
			Log.e(TAG, "Error during event trigger", e);
		}
	}

}