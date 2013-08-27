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

    /**
     * Erzeugt ein neues Credentials-Objekt. Die Zugangsdaten werden erst vom
     * Benutzer abgefragt, wenn eine andere Klasse darauf zugreift.
     * 
     * Wenn ein Benutzername als Parameter übergeben wird, wird dieser verwendet
     * und nicht mehr auf der Konsole abgefragt.
     * 
     * @param username
     *            Benutzername für NaMi
     */
    public NamiConsoleCredentials(String username) {
        super(null, null);
        this.apiUser = username;
    }

    private void readFromConsole() {
        if (apiUser == null) {
            apiUser = System.console().readLine("Enter username: ");
        }
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
