package nami.connector.credentials;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Fragt die Zugangsdaten für NaMi von der Konsole ab. Die Zugangsdaten werden
 * erst vom Benutzer abgefragt, wenn eine andere Klasse darauf zugreift.
 * 
 * @author Fabian Lipp
 * 
 */
public class NamiConsoleCredentials extends NamiCredentials {

    private boolean readCredentials = false;
    private String apiUser = null;
    private String apiPass = null;

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
        if (username != null && !username.isEmpty()) {
            this.apiUser = username;
        }
    }

    private String readLine(String format, Object... args) throws IOException {
        if (System.console() != null) {
            return System.console().readLine(format, args);
        }
        System.out.print(String.format(format, args));
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                System.in));
        return reader.readLine();
    }

    private char[] readPassword(String format, Object... args)
            throws IOException {
        if (System.console() != null) {
            return System.console().readPassword(format, args);
        }
        return this.readLine(format, args).toCharArray();
    }

    private void readFromConsole() {
        try {
            if (apiUser == null) {
                apiUser = readLine("Enter username: ");
            }
            char[] res = readPassword("Enter password: ");
            apiPass = String.valueOf(res);
            readCredentials = true;
        } catch (IOException e) {
            System.err.println("Reading username and password failed.");
        }
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
