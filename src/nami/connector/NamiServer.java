package nami.connector;

/**
 * Beschreibt einen NaMi-Server.
 * 
 * @author Fabian Lipp
 * 
 */
public class NamiServer {

    /**
     * Daten des NaMi-Testservers.
     */
    public static final NamiServer TESTSERVER = new NamiServer(
            "namitest.dpsg.de", false, "ica");

    /**
     * Daten des Produktiv-Servers.
     */
    public static final NamiServer LIVESERVER = new NamiServer("nami.dpsg.de",
            true, "ica");

    private String namiServer = "namitest.dpsg.de";
    private boolean useSsl = false;
    private String namiDeploy = "ica";

    /**
     * Erstellt die Beschreibung eines NaMi-Servers.
     * 
     * @param namiServer
     *            Hostname des Servers
     * @param useSsl
     *            legt fest, ob SSL für die Verbindung zum Server genutzt wird
     * @param namiDeploy
     *            Installationsverzeichnis auf dem Server
     */
    public NamiServer(String namiServer, boolean useSsl, String namiDeploy) {
        this.namiServer = namiServer;
        this.useSsl = useSsl;
        this.namiDeploy = namiDeploy;
    }

    /**
     * Liefert den Hostname des Servers.
     * 
     * @return Hostname
     */
    public String getNamiServer() {
        return namiServer;
    }

    /**
     * Gibt an, ob die Verbindung SSL nutzen soll.
     * 
     * @return <code>true</code>, falls SSL aktiv ist
     */
    public boolean getUseSsl() {
        return useSsl;
    }

    /**
     * Gibt das Installationsverzeichnis auf dem Server an.
     * 
     * @return Installationsverzeichnis
     */
    public String getNamiDeploy() {
        return namiDeploy;
    }
}
