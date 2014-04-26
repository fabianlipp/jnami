package nami.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Liefert Pfade zu Verzeichnissen und Konfigurationsdateien.
 * 
 * @author Fabian Lipp
 * 
 */
public final class Configuration {
    private Configuration() {
    }

    private static Logger log = Logger.getLogger(Configuration.class.getName());
    private static Properties generalProperties = null;

    /**
     * Liefert das Verzeichnis, in dem alle veränderlichen Dateien gespeichert
     * werden. Dieses befindet sich unterhalb des Home-Verzeichnisses des Users.
     * 
     * Dateiname: .jnami im Home-Verzeichnis des Users
     * 
     * @return Konfigurations-Verzeichnis
     * @throws ApplicationDirectoryException
     *             wird geworfen, falls das Verzeichnis noch nicht existiert und
     *             nicht erzeugt werden kann oder wenn bereits eine Datei mit
     *             dem Namen existiert
     */
    public static File getApplicationDirectory()
            throws ApplicationDirectoryException {
        File f = new File(System.getProperty("user.home"), ".jnami");
        if (!f.exists()) {
            f.mkdir();
        }

        if (!f.exists() || !f.isDirectory()) {
            throw new ApplicationDirectoryException(
                    "Config directory does not exist and cannot be created", f);
        }

        return f;
    }

    /**
     * Liefert die Logfile, die verwendet wird.
     * 
     * Dateiname: jnami.log
     * 
     * @return Logfile
     * @throws ApplicationDirectoryException
     *             wird geworfen, falls das Verzeichnis noch nicht existiert und
     *             nicht erzeugt werden kann oder wenn bereits eine Datei mit
     *             dem Namen existiert
     */
    public static File getLogfile() throws ApplicationDirectoryException {
        File f = new File(getApplicationDirectory(), "jnami.log");
        return f;
    }

    /**
     * Liefert die allgemeine Konfigurationsdatei, in der beispielsweise
     * konfiguriert ist, wie die Zugangsdaten zu NaMi ermittelt werden.
     * 
     * Dateiname: jnami.properties
     * 
     * @return Konfigurations-Datei
     * @throws ApplicationDirectoryException
     *             wird geworfen, falls das Verzeichnis noch nicht existiert und
     *             nicht erzeugt werden kann oder wenn bereits eine Datei mit
     *             dem Namen existiert
     */
    public static File getGeneralConfigfile()
            throws ApplicationDirectoryException {
        File f = new File(getApplicationDirectory(), "jnami.properties");

        return f;
    }

    /**
     * Liefert ein <tt>Properties</tt>-Objekt, das die allgemeine Konfiguration
     * enthält. Dazu wird zunächst die Standard-Konfiguration verwendet, die mit
     * dem Programm ausgeliefert wird und diese dann mit der
     * Benutzerkonfiguration überschrieben.
     * 
     * @return <tt>Properties</tt>-Objekt, in das die Konfigurationsdatei
     *         eingelesen ist.
     */
    public static Properties getGeneralProperties() {
        if (generalProperties != null) {
            return generalProperties;
        }

        InputStream defPropStr = Configuration.class
                .getResourceAsStream("defaultJnami.properties");
        Properties defProp = new Properties();
        try {
            defProp.load(defPropStr);
        } catch (IOException e) {
            log.warning("Could not read default credentials properties file");
        }
        Properties prop = new Properties(defProp);

        try {
            File f = getGeneralConfigfile();
            if (f.exists()) {
                InputStream in = new FileInputStream(f);
                prop.load(in);
                log.info("Read credentials configuration from file: "
                        + f.getAbsolutePath());
            }
        } catch (ApplicationDirectoryException e) {
            log.log(Level.WARNING, "Could not find configuration file", e);
        } catch (IOException e) {
            log.log(Level.WARNING, "Could not read configuration from file", e);
        }

        generalProperties = prop;
        return prop;
    }
}
