package com.teskalabs.seacat.android.AndroidDemoApp;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import java.io.IOException;

import com.teskalabs.seacat.android.client.CSR;

public class CSRDialog extends Dialog implements View.OnClickListener {

    public Activity activity;
    public Dialog dialog;
    public Button continueBtn;
    public EditText emailAddressTV;

    public CSRDialog(Activity activity)
    {
        super(activity);
        this.activity = activity;
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
                csr.setUniqueCommonName();
                csr.setEmailAddress(emailAddressTV.getText().toString());

                try {
                    csr.submit();
                    dismiss();
                } catch (IOException e) {
                    // TODO: Show error and try again.
                }
                break;

            default:
                break;
        }


    }

}
