package nami.beitrag;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import nami.beitrag.gui.MainWindow;
import nami.configuration.Configuration;
import nami.connector.Beitragsart;
import nami.connector.NamiConnector;
import nami.connector.NamiServer;
import nami.connector.credentials.NamiCredentials;
import nami.connector.exception.CredentialsInitiationException;

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

    // TODO: Aus Konfigurationsdatei lesen
    private static final String GRUPPIERUNGS_NUMMER = "220309";

    private static final String MYBATIS_CONFIGFILE = "db/mybatis-config.xml";

    /**
     * Haupt-Funktion zum Aufruf der GUI.
     * 
     * @param args
     *            Kommandozeilen-Argumente
     * @throws CredentialsInitiationException
     *             Problem beim Einlesen der NaMi-Zugangsdaten
     */
    public static void main(String[] args)
            throws CredentialsInitiationException {
        // TODO: Datenbank aus Konfigurationsdatei lesen
        final String dbDriver = "com.mysql.jdbc.Driver";
        final String dbUrl = "jdbc:mysql://localhost:3306/batistest";
        final String dbUsername = "batistest";
        final String dbPassword = "batistest";

        // TODO: Liquibase verwenden
        // Update Database Schema
        // SchemaUpdater updater = new SchemaUpdater(dbDriver, dbUrl,
        // dbUsername, dbPassword);
        // updater.update();

        // Initialise MyBatis
        Properties prop = new Properties();
        prop.setProperty("driver", dbDriver);
        prop.setProperty("url", dbUrl);
        prop.setProperty("username", dbUsername);
        prop.setProperty("password", dbPassword);
        InputStream is = NamiBeitrag.class
                .getResourceAsStream(MYBATIS_CONFIGFILE);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder()
                .build(is, prop);

        Properties p = Configuration.getGeneralProperties();

        NamiCredentials credentials = NamiCredentials
                .getCredentialsFromProperties(p);

        NamiConnector con;
        if (Boolean.parseBoolean(p.getProperty("nami.useApi"))) {
            con = new NamiConnector(NamiServer.LIVESERVER_WITH_API, credentials);
        } else {
            con = new NamiConnector(NamiServer.LIVESERVER, credentials);
        }

        // TODO: Aus Konfigurationsdatei auslesen
        Map<Beitragsart, BigDecimal> beitragssaetze = new HashMap<>();
        beitragssaetze.put(Beitragsart.VOLLER_BEITRAG, new BigDecimal("19.75"));
        beitragssaetze.put(Beitragsart.FAMILIEN_BEITRAG,
                new BigDecimal("13.20"));
        beitragssaetze.put(Beitragsart.SOZIALERMAESSIGUNG, new BigDecimal(
                "6.90"));
        beitragssaetze.put(Beitragsart.KEIN_BEITRAG, new BigDecimal("0.0"));

        // TODO: Logger-Konfiguration
        Handler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        Logger.getLogger("org.apache.ibatis").addHandler(handler);
        Logger.getLogger("org.apache.ibatis").setLevel(Level.ALL);
        Logger.getLogger("nami.beitrag.db.BeitragMapper")
                .addHandler(handler);
        Logger.getLogger("nami.beitrag.db.BeitragMapper")
                .setLevel(Level.ALL);
        Logger.getLogger("nami.beitrag.db.RechnungenMapper")
                .addHandler(handler);
        Logger.getLogger("nami.beitrag.db.RechnungenMapper")
                .setLevel(Level.ALL);
        Logger.getLogger("nami.beitrag.db.ReportsMapper")
                .addHandler(handler);
        Logger.getLogger("nami.beitrag.db.ReportsMapper")
                .setLevel(Level.ALL);

        // Aufruf der GUI
        NamiBeitrag namiBeitrag = new NamiBeitrag(sqlSessionFactory,
                GRUPPIERUNGS_NUMMER, beitragssaetze, con);
        MainWindow mainWindow = new MainWindow(namiBeitrag);
        mainWindow.setVisible(true);
    }
}
