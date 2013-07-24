package nami.connector.credentials;

/**
 * Stellt die Zugangsdaten für NaMi bereit.
 * 
 * @author Fabian Lipp
 * 
 */
public class NamiCredentials {
    private String apiUser;
    private String apiPass;

    /**
     * Erzeugt ein neues Credentials-Objekt, wobei die Zugangsdaten für NaMi
     * fest vorgegeben werden.
     * 
     * @param apiUser
     *            Benutzername (Mitgliedsnummer) für NaMi
     * @param apiPass
     *            Passwort
     */
    public NamiCredentials(String apiUser, String apiPass) {
        this.apiUser = apiUser;
        this.apiPass = apiPass;
    }

    /**
     * Liefert den Benutzernamen.
     * 
     * @return Benutzername
     */
    public String getApiUser() {
        return apiUser;
    }

    /**
     * Liefert das Passwort.
     * 
     * @return Passwort
     */
    public String getApiPass() {
        return apiPass;
    }
}
