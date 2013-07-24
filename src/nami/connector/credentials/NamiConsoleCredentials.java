package nami.connector.credentials;

/**
 * Fragt die Zugangsdaten für NaMi von der Konsole ab. Die Zugangsdaten werden
 * erst vom Benutzer abgefragt, wenn eine andere Klasse darauf zugreift.
 * 
 * @author Fabian Lipp
 * 
 */
public class NamiConsoleCredentials extends NamiCredentials {

    private boolean readCredentials = false;
    private String apiUser;
    private String apiPass;

    /**
     * Erzeugt ein neues Credentials-Objekt. Die Zugangsdaten werden erst vom
     * Benutzer abgefragt, wenn eine andere Klasse darauf zugreift.
     */
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
