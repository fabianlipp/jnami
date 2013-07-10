package nami.connector.credentials;


public class NamiConsoleCredentials extends NamiCredentials {

    private boolean readCredentials = false;
    private String apiUser;
    private String apiPass;
    
    public NamiConsoleCredentials() {
        super(null, null);
    }
    
    private void readFromConsole() {
        apiUser = System.console().readLine("Enter username: ");
        char[] res = System.console().readPassword("Enter password: ");
        apiPass = String.valueOf(res);
        readCredentials = true;
    }
    
    @Override
    public String getApiUser() {
        if (!readCredentials) {
            readFromConsole();
        }
        
        return apiUser;
    }
    
    @Override
    public String getApiPass() {
        if (!readCredentials) {
            readFromConsole();
        }
        
        return apiPass;
    }

}
