package com.teskalabs.seacat.android.client.okhttp3;

import com.teskalabs.seacat.android.client.SeaCatClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class SeaCatInterceptor implements Interceptor {

	@Override
	public Response intercept(Chain chain) throws IOException {

		Request r = chain.request();

		HttpURLConnection conn = SeaCatClient.open(r.url().url());
		conn.setRequestMethod(r.method());

		InputStream is = conn.getInputStream();
		assert(is != null);

		ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		int nRead;
		byte[] data = new byte[16384];
		while ((nRead = is.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, nRead);
		}
		buffer.flush();

		Response.Builder builder = new Response.Builder();
		builder.request(r);
		builder.code(conn.getResponseCode());
		builder.protocol(Protocol.HTTP_1_1);
		builder.message(conn.getResponseMessage());

		//TODO: Set headers

		MediaType mt = MediaType.parse(conn.getContentType());

		//TODO: Consider using Stream ...

		ResponseBody body = ResponseBody.create(mt, buffer.toByteArray());
		builder.body(body);

		return builder.build();
	}
}
