package com.teskalabs.seacat.android.client.okhttp3;

import com.teskalabs.seacat.android.client.SeaCatClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import okio.Buffer;

public class SeaCatInterceptor implements Interceptor {

	@Override
	public Response intercept(Chain chain) throws IOException {

		Request r = chain.request();

		HttpURLConnection conn = SeaCatClient.open(r.url().url());
		conn.setRequestMethod(r.method());

		conn.setConnectTimeout(3000); // 15 seconds is a default timeout

		// Copy request headers
		Headers rh = r.headers();
		for(int i = 0; i<rh.size(); i++)
		{
			conn.addRequestProperty(rh.name(i), rh.value(i));
		}

		// Send request body if we have one
		RequestBody rb = r.body();
		if (rb != null)
		{
			conn.setRequestProperty("Content-Type", rb.contentType().toString());
			long l = rb.contentLength();
			if (l > -1)
			{
				conn.setRequestProperty("Content-Length", Long.toString(l));
			}
			conn.setDoOutput(true);

			okio.Buffer buffer = new okio.Buffer();
			rb.writeTo(buffer);

			OutputStream os = conn.getOutputStream();
			buffer.copyTo(os);
			os.close();
		}

		// Now let's process the response ...
		InputStream is = conn.getInputStream();
		assert(is != null);

		okio.Buffer buffer = new okio.Buffer();
		buffer.readFrom(is);
		buffer.flush();

		Response.Builder builder = new Response.Builder();
		builder.request(r);
		builder.code(conn.getResponseCode());
		builder.protocol(Protocol.HTTP_1_1);
		builder.message(conn.getResponseMessage());

		// Copy response headers
		Map<String, List<String>> map = conn.getHeaderFields();
		if (map != null) {
			for (Map.Entry<String, List<String>> entry : map.entrySet()) {
				String key = entry.getKey();
				if (key == null) continue;
				for (String value : entry.getValue()) {
					builder.addHeader(entry.getKey(), value);
				}
			}
		}

		String content_type = conn.getContentType();
		if (content_type == null) {
			content_type = "text/plain";
		}

		MediaType mt = MediaType.parse(content_type);
		ResponseBody body = ResponseBody.create(mt, buffer.size(), buffer);
		builder.body(body);

		return builder.build();
	}
}
