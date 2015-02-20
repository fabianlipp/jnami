package nami.connector.credentials;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Fragt die Zugangsdaten für NaMi in einem Swing-Frame ab. Die Zugangsdaten
 * werden erst vom Benutzer abgefragt, wenn eine andere Klasse darauf zugreift.
 * 
 * @author Fabian Lipp
 * 
 */
public class NamiSwingCredentials extends NamiCredentials {

    private boolean readCredentials = false;
    private String apiUser = null;
    private String apiPass = null;

    private CredentialsFrame frame;
    private final Object lock = new Object();

    /**
     * Erzeugt ein neues Credentials-Objekt. Die Zugangsdaten werden erst vom
     * Benutzer abgefragt, wenn eine andere Klasse darauf zugreift.
     */
    public NamiSwingCredentials() {
        this(null);
    }

    /**
     * Erzeugt ein neues Credentials-Objekt. Die Zugangsdaten werden erst vom
     * Benutzer abgefragt, wenn eine andere Klasse darauf zugreift.
     * 
     * Wenn ein Benutzername (ungleich <tt>null</tt>) als Parameter übergeben
     * wird, wird dieser als Vorgabewert ins Formular eingetragen. Außerdem
     * erhält das Passwort-Feld beim Anzeigen des Formulars den Fokus. Der
     * Benutzername kann im Formular trotzdem geändert werden.
     * 
     * @param username
     *            Benutzername für NaMi
     */
    public NamiSwingCredentials(String username) {
        super(null, null);
        frame = new CredentialsFrame(username);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                synchronized (lock) {
                    lock.notify();
                }
            }
        });
    }

    private boolean readWithSwing() {

        frame.setVisible(true);

        synchronized (lock) {
            while (frame.isVisible()) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        if (!frame.isComplete()) {
            return false;
        } else {
            readCredentials = true;
            apiUser = frame.getUsername();
            apiPass = new String(frame.getPassword());
            return true;
        }
    }

    @Override
    public String getApiUser() {
        if (!readCredentials) {
            readWithSwing();
        }

        return apiUser;
    }

    @Override
    public String getApiPass() {
        if (!readCredentials) {
            readWithSwing();
        }

        return apiPass;
    }

}
