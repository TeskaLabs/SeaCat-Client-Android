package com.teskalabs.seacat.android.okhttp;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.teskalabs.seacat.android.client.okhttp3.SeaCatInterceptor;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	public void onClick(View v) {
		new LongOperation().execute("xxxx");
	}

	private class LongOperation extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... strings) {

			OkHttpClient client = new OkHttpClient.Builder()
				.addInterceptor(new SeaCatInterceptor())
				.build();

			Request request = new Request.Builder()
				.url("https://jsontest.seacat/")
				.build();

			Response response = null;
			try {
				response = client.newCall(request).execute();
				Log.i("NiceTag", response.body().string());
			} catch (IOException e) {
				e.printStackTrace();
			}

			return "OK";
		}
	}

}
