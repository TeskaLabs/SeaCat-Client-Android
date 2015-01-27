package mobi.seacat.client.internal;

import java.util.Map;
import java.util.HashMap;
import java.net.HttpURLConnection;

public final class HttpStatus
{
	private static final Map<Integer, String> map = new HashMap<Integer, String>();

	public static String getMessage(int statusCode)
	{
		return map.get(statusCode);
	}
	
	static
	{
		//1xx codes
		map.put(100, "Continue");
		map.put(101, "Switching Protocols");
		map.put(102, "Processing");

		//2xx codes
		map.put(HttpURLConnection.HTTP_OK /*200*/, "OK");
		map.put(HttpURLConnection.HTTP_CREATED /*201*/, "Created");
		map.put(HttpURLConnection.HTTP_ACCEPTED /*202*/, "Accepted");
		map.put(203, "Non Authoritative Information");
		map.put(HttpURLConnection.HTTP_NO_CONTENT /*204*/, "No Content");
		map.put(205, "Reset Content");
		map.put(206, "Partial Content");
		map.put(207, "Multi-Status");

		//3xx codes
		map.put(300, "Multiple Choices");
		map.put(HttpURLConnection.HTTP_MOVED_PERM /*301*/, "Moved Permanently");
		map.put(HttpURLConnection.HTTP_MOVED_TEMP /*302*/, "Moved Temporarily");
		map.put(HttpURLConnection.HTTP_SEE_OTHER /*303*/, "See Other");
		map.put(HttpURLConnection.HTTP_NOT_MODIFIED /*304*/, "Not Modified");
		map.put(HttpURLConnection.HTTP_USE_PROXY /*305*/, "Use Proxy");
		
		map.put(307, "Temporary Redirect");
		
		//4xx codes
		map.put(HttpURLConnection.HTTP_BAD_REQUEST /*400*/, "Bad Request");
		map.put(HttpURLConnection.HTTP_UNAUTHORIZED /*401*/, "Unauthorized");
		map.put(HttpURLConnection.HTTP_PAYMENT_REQUIRED /*402*/, "Payment Required");
		map.put(HttpURLConnection.HTTP_FORBIDDEN /*403*/, "Forbidden");
		map.put(HttpURLConnection.HTTP_NOT_FOUND /*404*/, "Not Found");
		map.put(405, "Method Not Allowed");
		map.put(HttpURLConnection.HTTP_NOT_ACCEPTABLE /*406*/, "Not Acceptable");
		map.put(407, "Proxy Authentication Required");
		map.put(408, "Request Timeout");
		map.put(HttpURLConnection.HTTP_CONFLICT /*409*/, "Conflict");
		map.put(HttpURLConnection.HTTP_GONE /*410*/, "Gone");
		map.put(HttpURLConnection.HTTP_LENGTH_REQUIRED /*411*/, "Length Required");
		map.put(412, "Precondition Failed");
		map.put(413, "Request Too Long");
		map.put(414, "Request-URI Too Long");
		map.put(415, "Unsupported Media Type");
		map.put(416, "Requested Range Not Satisfiable");
		map.put(417, "Expectation Failed");
		map.put(418, "Unprocessable Entity");
		map.put(419, "Insufficient Space On Resource");
		map.put(420, "Method Failure");

		
		map.put(423, "Locked");
		map.put(424, "Failed Dependency");

		//5xx codes
		map.put(HttpURLConnection.HTTP_INTERNAL_ERROR /*500*/, "Internal Server Error");
		map.put(HttpURLConnection.HTTP_NOT_IMPLEMENTED /*501*/, "Not Implemented");
		map.put(HttpURLConnection.HTTP_BAD_GATEWAY /*502*/, "Bad Gateway");
		map.put(HttpURLConnection.HTTP_UNAVAILABLE /*503*/, "Service Unavailable");
		map.put(HttpURLConnection.HTTP_GATEWAY_TIMEOUT /*504*/, "Gateway Timeout");
		map.put(505, "Http Version Not Supported");

		map.put(507 , "Insufficient Storage");		
	}

}
