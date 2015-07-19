package mobi.seacat.client;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

public class CSR
{
    private Map<String, String> paramMap = new HashMap<String, String>();

    ///

    CSR()
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

}
