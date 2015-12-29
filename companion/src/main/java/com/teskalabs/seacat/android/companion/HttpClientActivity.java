package com.teskalabs.seacat.android.companion;

import android.os.Bundle;

public class HttpClientActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contentStub.setLayoutResource(R.layout.activity_http_client);
        contentStub.inflate();

    }

}
