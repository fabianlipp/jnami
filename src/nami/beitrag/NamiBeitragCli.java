package nami.beitrag;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import jline.console.completer.Completer;
import nami.cli.CliParser;
import nami.cli.CompleterFactory;
import nami.cli.annotation.AlternateCommands;
import nami.cli.annotation.CliCommand;
import nami.cli.annotation.ParamCompleter;
import nami.cli.annotation.ParentCommand;
import nami.configuration.ApplicationDirectoryException;
import nami.configuration.ConfigFormatException;
import nami.configuration.Configuration;
import nami.connector.Beitragsart;
import nami.connector.NamiConnector;
import nami.connector.NamiServer;
import nami.connector.credentials.NamiCredentials;
import nami.connector.exception.NamiApiException;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

public class NamiBeitragCli {
    private static SqlSessionFactory sqlSessionFactory;

    // TODO: Aus Konfigurationsdatei lesen
    private static final String GRUPPIERUNGS_NUMMER = "220309";
    private static Map<Beitragsart, BigDecimal> beitragssaetze = null;

    private static Logger log = Logger
            .getLogger(NamiBeitragCli.class.getName());
    private static CliParser parser = new CliParser(NamiBeitragCli.class,
            "beitrag");

    private static final String MYBATIS_CONFIGFILE = "db/mybatis-config.xml";

    static {
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
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(is, prop);

        // TODO: Aus Konfigurationsdatei auslesen
        beitragssaetze = new HashMap<>();
        beitragssaetze.put(Beitragsart.VOLLER_BEITRAG, new BigDecimal("19.75"));
        beitragssaetze.put(Beitragsart.FAMILIEN_BEITRAG,
                new BigDecimal("13.20"));
        beitragssaetze.put(Beitragsart.SOZIALERMAESSIGUNG, new BigDecimal(
                "6.90"));
        beitragssaetze.put(Beitragsart.KEIN_BEITRAG, new BigDecimal("0.0"));
    }

    /**
     * Main-Funktion, wenn diese Klasse direkt von der Kommandozeile aufgerufen
     * wird.
     * 
     * @param args
     *            Kommandozeilen-Argumente
     * @throws Exception .
     */
    public static void main(String[] args) throws Exception {
        try {
            Properties p = Configuration.getGeneralProperties();

            NamiCredentials credentials = NamiCredentials
                    .getCredentialsFromProperties(p);

            NamiConnector con;
            if (Boolean.parseBoolean(p.getProperty("nami.useApi"))) {
                con = new NamiConnector(NamiServer.LIVESERVER_WITH_API,
                        credentials);
            } else {
                con = new NamiConnector(NamiServer.LIVESERVER, credentials);
            }

            PrintWriter out = new PrintWriter(System.out);
            beitrag(args, con, out);
            out.close();
            System.exit(0);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Haupt-Funktion, wenn das Statistik-Tool durch NamiCli aufgerufen wird.
     * 
     * @param args
     *            nicht verwendet
     * @param namicon
     *            Verbindung zum NaMi-Server
     * @param out
     *            Writer, auf dem die Ausgabe erfolgt
     * @throws IOException
     *             IOException
     * @throws ConfigFormatException
     *             Fehler in der Konfigurationsdatei
     * @throws SQLException
     *             Fehler bei einer SQL-Anfrage
     * @throws ApplicationDirectoryException
     *             Probleme beim Zugriff auf das Konfigurationsverzeichnis
     */
    @CliCommand("beitrag")
    @ParamCompleter(BeitragCompleter.class)
    public static void beitrag(String[] args, NamiConnector namicon,
            PrintWriter out) throws IOException, ConfigFormatException,
            SQLException, ApplicationDirectoryException {

        // Logger dbLogger =
        // Logger.getLogger(StatisticsDatabase.class.getName());
        // dbLogger.setLevel(Level.FINEST);
        Handler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        // Logger.getLogger("").addHandler(handler);
        // Logger.getLogger("").setLevel(Level.ALL);

        parser.callMethod(args, namicon, out);
    }

    /**
     * Liefert den Completer, der die Kommandos vervollständigt, die
     * NamiStatistics versteht.
     * 
     */
    public static class BeitragCompleter implements CompleterFactory {
        @Override
        public Completer getCompleter() {
            return parser.getCompleter();
        }
    }

    /**
     * Bringt die lokale Mitgliederdatenbank auf den aktuellen Stand. Dazu
     * werden folgende Schritte ausgeführt:
     * <ul>
     * <li>in NaMi neu angelegte Mitglieder in die lokale Datenbank einfügen</li>
     * <li>in NaMi geänderte Mitglieder (erkennbar an der Versionsnummer) in der
     * lokalen Datenbank aktualisieren</li>
     * <li>in NaMi gelöschte Mitglieder in der lokalen Datenbank als gelöscht
     * markieren</li>
     * </ul>
     * Es werden keine Daten aus der lokalen Datenbank nach NaMi übertragen.
     * 
     * @param args
     *            nicht verwendet
     * @param namicon
     *            Verbindung zum NaMi-Server
     * @param out
     *            Writer, auf dem die Ausgabe erfolgt
     * @throws IOException
     *             IOException
     * @throws NamiApiException
     *             Fehler bei einer Anfrage an NaMi
     */
    @CliCommand("syncMitglieder")
    @AlternateCommands("sync")
    @ParentCommand("beitrag")
    public static void syncMitglieder(String[] args, NamiConnector namicon,
            PrintWriter out) throws NamiApiException, IOException {

        NamiBeitrag namiBeitrag = new NamiBeitrag(sqlSessionFactory,
                GRUPPIERUNGS_NUMMER, beitragssaetze, namicon);
        namiBeitrag.syncMitglieder();
    }

    /**
     * Holt die Beitragszahlungen aller Mitglieder ab und fügt sie in die lokale
     * Datenbank ein, falls sie noch nicht vorhanden sind.
     * 
     * @param args
     *            nicht verwendet
     * @param namicon
     *            Verbindung zum NaMi-Server
     * @param out
     *            Writer, auf dem die Ausgabe erfolgt
     * @throws IOException
     *             IOException
     * @throws NamiApiException
     *             Fehler bei einer Anfrage an NaMi
     */
    @CliCommand("fetchBeitragszahlungen")
    @ParentCommand("beitrag")
    public static void fetchBeitragszahlungen(String[] args,
            NamiConnector namicon, PrintWriter out) throws NamiApiException,
            IOException {
        NamiBeitrag namiBeitrag = new NamiBeitrag(sqlSessionFactory,
                GRUPPIERUNGS_NUMMER, beitragssaetze, namicon);
        namiBeitrag.fetchBeitragszahlungen();
    }

}
