package com.teskalabs.seacat.android.client;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SeaCatEventTrigger implements Runnable {

	private static final String TAG = SeaCatEventTrigger.class.getName();
	private final URL url;
	private final JSONObject json = new JSONObject();

	protected int responseCode = -1;
	protected String responseMessage = null;
	protected final ByteArrayOutputStream responseBody;

	private static final String eventsAPIUrlBase = "https://api.seacat/event/trigger/";

	public SeaCatEventTrigger(String eventName) throws IOException {
		this(eventsAPIUrlBase, eventName);
	}

	public SeaCatEventTrigger(String URLBase, String eventName) throws IOException {
		url = new URL(URLBase +  eventName);
		responseBody = new ByteArrayOutputStream();
	}

	public SeaCatEventTrigger put(String name, String value) throws JSONException {
		json.put(name, value);
		return this;
	}

	@Override
	public void run() {
		onPreExecute();

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
			int nRead;
			byte[] data = new byte[1024];
			while ((nRead = is.read(data, 0, data.length)) != -1) {
				responseBody.write(data, 0, nRead);
			}
			responseBody.flush();

			responseCode = conn.getResponseCode();
			responseMessage = conn.getResponseMessage();

			onPostExecute();
		} catch (IOException e) {
			onError(e);
		}
	}


	/* Override me ! */
	public void onPreExecute() {
	}

	/* Override me ! */
	public void onPostExecute() {
	}

	/* Override me ! */
	public void onError(IOException e) {
	}


	public int getResponseCode() {
		return responseCode;
	}

	public String getResponseMessage() {
		return responseMessage;
	}

	public ByteArrayOutputStream getResponseBody() {
		return responseBody;
	}

}
