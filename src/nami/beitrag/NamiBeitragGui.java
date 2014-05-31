package nami.beitrag;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import nami.beitrag.gui.MainWindow;
import nami.beitrag.letters.LetterDirectory;
import nami.configuration.ApplicationDirectoryException;
import nami.configuration.ConfigFormatException;
import nami.configuration.Configuration;
import nami.connector.NamiConnector;
import nami.connector.NamiServer;
import nami.connector.credentials.NamiCredentials;
import nami.connector.exception.CredentialsInitiationException;
import nami.db.schema.SchemaUpdater;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

/**
 * Main-Klasse zum Aufruf der GUI für das Beitragstool.
 * 
 * @author Fabian Lipp
 * 
 */
public final class NamiBeitragGui {
    private NamiBeitragGui() {
    }

    private static final String CONFIG_FILENAME = "namibeitrag.xml";
    private static final String MYBATIS_CONFIGFILE = "db/mybatis-config.xml";

    /**
     * Haupt-Funktion zum Aufruf der GUI.
     * 
     * @param args
     *            Kommandozeilen-Argumente
     * @throws CredentialsInitiationException
     *             Problem beim Einlesen der NaMi-Zugangsdaten
     * @throws ApplicationDirectoryException
     *             Fehler beim Zugriff auf das Konfigurationsverzeichnis
     * @throws IOException
     *             Fehler beim Dateizugriff
     * @throws ConfigFormatException
     *             Fehler beim Parsen der Konfigurationsdatei
     */
    public static void main(String[] args)
            throws CredentialsInitiationException,
            ApplicationDirectoryException, ConfigFormatException, IOException {
        File configFile = new File(Configuration.getApplicationDirectory(),
                CONFIG_FILENAME);
        if (!configFile.exists()) {
            throw new IllegalArgumentException(
                    "Erwarte Konfigurationsdatei in " + configFile);
        }
        NamiBeitragConfiguration conf = new NamiBeitragConfiguration(configFile);

        // Update Database Schema
        Properties dbConfig = conf.getDatabaseConfig();
        SchemaUpdater updater = new SchemaUpdater(
                dbConfig.getProperty("driver"), dbConfig.getProperty("url"),
                dbConfig.getProperty("user"), dbConfig.getProperty("password"));
        updater.update("beitrag");

        // Initialise MyBatis
        InputStream is = NamiBeitragGui.class
                .getResourceAsStream(MYBATIS_CONFIGFILE);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder()
                .build(is, dbConfig);

        // Connect to NaMi
        Properties p = Configuration.getGeneralProperties();
        NamiCredentials credentials = NamiCredentials
                .getCredentialsFromProperties(p);
        NamiConnector con;
        if (Boolean.parseBoolean(p.getProperty("nami.useApi"))) {
            con = new NamiConnector(NamiServer.LIVESERVER_WITH_API, credentials);
        } else {
            con = new NamiConnector(NamiServer.LIVESERVER, credentials);
        }

        LetterDirectory dir = new LetterDirectory(new File(
                conf.getLetterOutputPath()));

        // TODO: Logger-Konfiguration
        Handler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        Logger.getLogger("org.apache.ibatis").addHandler(handler);
        Logger.getLogger("org.apache.ibatis").setLevel(Level.ALL);
        Logger.getLogger("nami.beitrag.db.BeitragMapper").addHandler(handler);
        Logger.getLogger("nami.beitrag.db.BeitragMapper").setLevel(Level.ALL);
        Logger.getLogger("nami.beitrag.db.BriefeMapper").addHandler(handler);
        Logger.getLogger("nami.beitrag.db.BriefeMapper").setLevel(Level.ALL);
        Logger.getLogger("nami.beitrag.db.RechnungenMapper")
                .addHandler(handler);
        Logger.getLogger("nami.beitrag.db.RechnungenMapper")
                .setLevel(Level.ALL);
        Logger.getLogger("nami.beitrag.db.ReportsMapper").addHandler(handler);
        Logger.getLogger("nami.beitrag.db.ReportsMapper").setLevel(Level.ALL);
        Logger.getLogger("nami.beitrag.db.LastschriftenMapper").addHandler(
                handler);
        Logger.getLogger("nami.beitrag.db.LastschriftenMapper").setLevel(
                Level.ALL);
        Logger.getLogger("nami.beitrag.db.MandateMapper").addHandler(handler);
        Logger.getLogger("nami.beitrag.db.MandateMapper").setLevel(Level.ALL);

        // Aufruf der GUI
        NamiBeitrag namiBeitrag = new NamiBeitrag(sqlSessionFactory, conf, con);
        MainWindow mainWindow = new MainWindow(namiBeitrag, dir, conf);
        mainWindow.setVisible(true);
    }
}
