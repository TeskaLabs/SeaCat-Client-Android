package com.teskalabs.seacat.android.companion;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import com.teskalabs.seacat.android.companion.Base.BaseActivity;

public class HttpClientActivity extends BaseActivity {
    Spinner methodsSpinner;
    ListView headersListView;
    String methods[] = {
        "GET",
        "PUT",
        "POST",
        "DELETE"
    };

    public class HttpHeader {
        String name;
        String value;
        public HttpHeader(String name, String value){
            this.name = name;
            this.value = value;
        }
    }


    public class HttpHeaderListAdapter extends ArrayAdapter<HttpHeader> {
        private Context mContext;
        private int layoutResourceId;
        private HttpHeader data[] = null;

        public HttpHeaderListAdapter(Context mContext, int layoutResourceId, HttpHeader[] data) {

            super(mContext, layoutResourceId, data);
            this.layoutResourceId = layoutResourceId;
            this.mContext = mContext;
            this.data = data;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View listItem = convertView;

            LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
            listItem = inflater.inflate(layoutResourceId, parent, false);

            return listItem;
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contentStub.setLayoutResource(R.layout.activity_http_client);
        contentStub.inflate();

        methodsSpinner = (Spinner) findViewById(R.id.spinnerMethod);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, methods);

        // Apply the adapter to the spinner
        methodsSpinner.setAdapter(adapter);

        // Headers test
        HttpHeader[] headers = new HttpHeader[3];
        headers[0] = new HttpHeader("Content-Type", "application/json");
//        headers[1] = new HttpHeader("Pragma", "no-cache");
//        headers[2] = new HttpHeader("Pragma", "no-cache");

        HttpHeaderListAdapter httpHeadersAdapter = new HttpHeaderListAdapter(this, R.layout.list_item_http_header, headers);
        ListView headersListView = (ListView) findViewById(R.id.http_headers_list);
        headersListView.setAdapter(httpHeadersAdapter);

    }

}
