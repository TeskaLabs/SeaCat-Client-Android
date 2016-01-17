package com.teskalabs.seacat.android.client;

import android.provider.Settings;
import android.util.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

import com.teskalabs.seacat.android.client.util.RC;
import com.teskalabs.seacat.android.client.core.seacatcc;

import org.json.JSONObject;

public class CSR
{
    private Map<String, String> paramMap = new HashMap<String, String>();

    ///

    public CSR()
    {
    }

    ///

    public void set(String name, String value)
    {
        paramMap.put(name, value);
    }

    public String get(String name)
    {
        return paramMap.get(name);
    }

    ///

    public String[] toStringArray() {
        int cnt = paramMap.size();
        String[] arr = new String[cnt*2];

        int pos = 0;
        for (Map.Entry<String, String> entry : paramMap.entrySet())
        {
            arr[pos++] = entry.getKey();
            arr[pos++] = entry.getValue();
        }

        return arr;
    }

    ///

    public String getCountry() {
        return paramMap.get("C");
    }

    public void setCountry(String country) {
        paramMap.put("C", country);
    }


    public String getState() {
        return paramMap.get("ST");
    }

    public void setState(String state) {
        paramMap.put("ST", state);
    }


    public String getLocality() {
        return paramMap.get("L");
    }

    public void setLocality(String locality) {
        paramMap.put("L", locality);
    }


    public String getOrganization() {
        return paramMap.get("O");
    }

    public void setOrganization(String organization) {
        paramMap.put("O", organization);
    }


    public String getOrganizationUnit() {
        return paramMap.get("OU");
    }

    public void setOrganizationUnit(String organization_unit) {
        paramMap.put("OU", organization_unit);
    }


    public String getCommonName() {
        return paramMap.get("CN");
    }

    public void setCommonName(String common_name) {
        paramMap.put("CN", common_name);
    }


    public String getSurname() {
        return paramMap.get("SN");
    }

    public void setSurname(String surname) {
        paramMap.put("SN", surname);
    }


    public String getGivenName() {
        return paramMap.get("GN");
    }

    public void setGivenName(String given_name) {
        paramMap.put("GN", given_name);
    }


    public String getEmailAddress() {
        return paramMap.get("emailAddress");
    }

    public void setEmailAddress(String emailAddress) {
        paramMap.put("emailAddress", emailAddress);
    }



    public String getUniqueIdentifier() {
        return paramMap.get("UID");
    }

    public void setUniqueIdentifier(String uniqueIdentifier) {
        paramMap.put("UID", uniqueIdentifier);
    }


    ///

    public void setUniqueIdentifier()
    {
        UUID deviceUuid = null;
        final String androidId = Settings.Secure.getString(
                SeaCatService.instance.getApplicationContext().getContentResolver(),
                Settings.Secure.ANDROID_ID
        );

        if ((androidId != null) && (!"9774d56d682e549c".equals(androidId)))
        {
            try {
                deviceUuid = UUID.nameUUIDFromBytes(androidId.getBytes("utf8"));
            } catch (UnsupportedEncodingException e)
            {   }
        }

        if (deviceUuid == null)
        {
            Log.w(SeaCatInternals.L, "Settings.Secure.ANDROID_ID get failed, using random UUID");
            deviceUuid = UUID.randomUUID();
        }

        this.setUniqueIdentifier(deviceUuid.toString().replaceAll("-", ""));
    }

    ///

    public void setData(String data)
    {
        this.set("description", data);
    }

    public void setJsonData(JSONObject jsonData)
    {
        this.setData(jsonData.toString());
    }

    ///

    public void submit() throws IOException
    {
        int rc = seacatcc.csrgen_worker(this.toStringArray());
        RC.checkAndThrowIOException("seacatcc.csrgen_worker", rc);
    }

    ///

    static public Runnable createDefault()
    {
        return new Runnable()
        {
            public void run()
            {

                CSR csr = new CSR();

                csr.setUniqueIdentifier();

                try {
                    csr.submit();
                } catch (IOException e) {
                    Log.e(SeaCatInternals.L, "Exception in CSR.createDefault:", e);
                }
            }
        };
    }

}
