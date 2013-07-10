package nami.connector.credentials;

public class NamiCredentials {
    private String apiUser;
    private String apiPass;
    
    public NamiCredentials(String apiUser, String apiPass) {
        this.apiUser = apiUser;
        this.apiPass = apiPass;
    }
    
    public String getApiUser() {
        return apiUser;
    }
    
    public String getApiPass() {
        return apiPass;
    }
}
