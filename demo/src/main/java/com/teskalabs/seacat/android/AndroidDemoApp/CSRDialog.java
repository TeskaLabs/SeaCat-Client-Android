package com.teskalabs.seacat.android.AndroidDemoApp;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import com.teskalabs.seacat.android.client.CSR;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class CSRDialog extends Dialog implements View.OnClickListener {

    public Button continueBtn;
    public EditText emailAddressTV;

    public CSRDialog(Context context)
    {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_csr);

        continueBtn = (Button) findViewById(R.id.dialog_button_ok);
        continueBtn.setOnClickListener(this);

        emailAddressTV = (EditText) findViewById(R.id.dialog_csr_emailaddress);
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {

            case R.id.dialog_button_ok:

                CSR csr = new CSR();
                csr.setUniqueIdentifier(this.getContext());
                csr.setEmailAddress(emailAddressTV.getText().toString());

                JSONObject object = new JSONObject();
                try {
                    object.put("name", "Jack Hack");
                    object.put("score", new Integer(200));
                    object.put("current", new Double(152.32));
                    object.put("nickname", "Hacker");
                } catch (JSONException e) {
                    Log.e("CSRDialog", "Error building JSON", e);
                    return;
                }
                csr.setJsonData(object);

                try {
                    csr.submit();
                } catch (IOException e) {
                    Log.e("CSRDialog", "Submitting CSR", e);
                }
                break;

            default:
                break;
        }


    }

}
