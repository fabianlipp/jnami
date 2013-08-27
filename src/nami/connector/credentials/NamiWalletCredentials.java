package nami.connector.credentials;

import org.evolvis.libwallet.WalletManager;
import org.evolvis.libwallet.impl.WalletException;

/**
 * Holt das Passwort für NaMi aus der KDE Wallet. Das Passwort wird erst
 * ausgelesen, wenn es von einer anderen Klasse abgefragt wird.
 * 
 * Das Passwort steht im Verzeichnis "jnami" im Schlüssel <<Benutzername>>.
 * Der Schlüssel ist vom Typ Passwort.
 * 
 * @author Fabian Lipp
 * 
 */
public class NamiWalletCredentials extends NamiCredentials {

    private boolean readCredentials = false;
    private String apiPass;

    /**
     * Erzeugt ein neues Credentials-Objekt, wobei das Passwort aus der KDE
     * Wallet gelesen wird.
     * 
     * @param apiUser
     *            Benutzername (Mitgliedsnummer) für NaMi
     */
    public NamiWalletCredentials(String apiUser) {
        super(apiUser, null);
    }

    private void readFromWallet() throws WalletException {
        WalletManager wallet = WalletManager.getInstance();
        apiPass = wallet.readPassword("jnami", "jnami", getApiUser());
        readCredentials = true;
    }

    @Override
    public String getApiPass() {
        if (!readCredentials) {
            try {
                readFromWallet();
            } catch (WalletException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        return apiPass;
    }

}
