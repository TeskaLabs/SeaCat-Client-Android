package mobi.seacat.client;

public class CSR
{
    private String state = null;
    private String country = null;
    private String locality = null;
    private String organization = null;
    private String organization_unit = null;
    private String common_name = null;
    private String title = null;
    private String surname = null;
    private String given_name = null;
    private String email = null;
    private String san_email = null;
    private String san_uri = null;

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getLocality() {
        return locality;
    }

    public void setLocality(String locality) {
        this.locality = locality;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getOrganizationUnit() {
        return organization_unit;
    }

    public void setOrganizationUnit(String organization_unit) {
        this.organization_unit = organization_unit;
    }

    public String getCommonName() {
        return common_name;
    }

    public void setCommonName(String common_name) {
        this.common_name = common_name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getGiven_name() {
        return given_name;
    }

    public void setGiven_name(String given_name) {
        this.given_name = given_name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSan_email() {
        return san_email;
    }

    public void setSan_email(String san_email) {
        this.san_email = san_email;
    }

    public String getSan_uri() {
        return san_uri;
    }

    public void setSan_uri(String san_uri) {
        this.san_uri = san_uri;
    }

    public CSR()
    {

    }
}
