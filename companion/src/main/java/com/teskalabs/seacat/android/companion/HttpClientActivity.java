package com.teskalabs.seacat.android.companion;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.teskalabs.seacat.android.companion.Base.BaseActivity;

public class HttpClientActivity extends BaseActivity {
    Spinner methodsSpinner;
    Spinner headersSpinner;
    String methods[] = {
        "GET",
        "PUT",
        "POST",
        "DELETE"
    };

    private class HttpHeader {
        String name;
        String value;
        public void HttpHeader(String name, String value){
            this.name = name;
            this.value = value;
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
    }

}
