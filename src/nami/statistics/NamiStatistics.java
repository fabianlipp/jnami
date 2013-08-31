package nami.statistics;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import nami.cli.AlternateCommands;
import nami.cli.CliCommand;
import nami.cli.CommandDoc;
import nami.configuration.ApplicationDirectoryException;
import nami.configuration.ConfigFormatException;
import nami.configuration.Configuration;
import nami.connector.NamiConnector;
import nami.connector.NamiServer;
import nami.connector.credentials.NamiCredentials;
import nami.connector.exception.CredentialsInitiationException;
import nami.connector.exception.NamiApiException;
import nami.connector.namitypes.NamiGruppierung;
import nami.connector.namitypes.NamiSearchedValues;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaderJDOMFactory;
import org.jdom2.input.sax.XMLReaderXSDFactory;

/**
 * Liest Mitgliederzahlen aus NaMi aus und erstellt Statistiken daraus.
 * 
 * @author Fabian Lipp
 * 
 */
public final class NamiStatistics {
    private NamiConnector namicon;
    private StatisticsDatabase db;
    private Collection<Gruppe> gruppen;
    private static Logger log = Logger.getLogger(NamiStatistics.class
            .getCanonicalName());

    private static final String CONFIG_FILENAME = "namistatistics.xml";

    private NamiStatistics(NamiConnector namicon, StatisticsDatabase db,
            Collection<Gruppe> gruppen) {
        this.namicon = namicon;
        this.db = db;
        this.gruppen = gruppen;
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

            main(args, con, new PrintWriter(System.out));
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
     * @throws CredentialsInitiationException
     *             Fehler beim Initiieren der Zugangsdaten zu NaMi, die in der
     *             Konfigurationsdatei angegeben sind
     * @throws SQLException
     *             Fehler bei einer SQL-Anfrage
     * @throws NamiApiException
     *             API-Fehler beim Zugriff auf NaMi
     * @throws ApplicationDirectoryException
     *             Probleme beim Zugriff auf das Konfigurationsverzeichnis
     */
    @CliCommand("statistics")
    @AlternateCommands("stats")
    @CommandDoc("Erstellt Statistiken und gibt diese im CSV-Format aus")
    public static void main(String[] args, NamiConnector namicon,
            PrintWriter out) throws IOException, ConfigFormatException,
            CredentialsInitiationException, SQLException, NamiApiException,
            ApplicationDirectoryException {

        String xsdFile = "namistatistics.xsd";

        Logger dbLogger = Logger.getLogger(StatisticsDatabase.class
                .getCanonicalName());
        dbLogger.setLevel(Level.FINEST);
        Handler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        Logger.getLogger("").addHandler(handler);

        // Lese Konfigurationsdatei ein (inkl. Validierung)
        // TODO: Fange Exception ab, wenn Dokument nicht valide
        Document doc;
        try {
            URL xsdfile = NamiStatistics.class.getResource(xsdFile);
            XMLReaderJDOMFactory schemafac = new XMLReaderXSDFactory(xsdfile);
            SAXBuilder builder = new SAXBuilder(schemafac);

            File configFile = new File(Configuration.getApplicationDirectory(),
                    CONFIG_FILENAME);
            log.info("Using statistics config file: "
                    + configFile.getAbsolutePath());
            if (!configFile.exists() || !configFile.canRead()) {
                throw new ConfigFormatException("Cannot read config file");
            }
            doc = builder.build(configFile);
        } catch (JDOMException e) {
            throw new ConfigFormatException("Could not parse config file", e);
        }

        // Parse Konfiguration aus XML
        Element namistatisticsEl = doc.getRootElement();
        if (namistatisticsEl.getName() != "namistatistics") {
            throw new ConfigFormatException("Wrong root element in config file");
        }

        // Datenbankverbindung aus XML lesen
        Element databaseEl = namistatisticsEl.getChild("database");
        String dbUrl = databaseEl.getChildText("url");
        String dbUsername = databaseEl.getChildText("username");
        String dbPassword = databaseEl.getChildText("password");

        // Gruppen aus XML einlesen
        List<Gruppe> gruppen = new LinkedList<>();
        for (Element gruppeEl : namistatisticsEl.getChildren("gruppe")) {
            int gruppeId = Integer.parseInt(gruppeEl.getAttributeValue("id"));
            String bezeichnung = gruppeEl.getAttributeValue("bezeichnung");
            List<NamiSearchedValues> searches = new LinkedList<>();
            for (Element namiSearchEl : gruppeEl.getChildren("namiSearch")) {
                searches.add(NamiSearchedValues.fromXml(namiSearchEl));
            }
            gruppen.add(new Gruppe(gruppeId, bezeichnung, searches));
        }

        // Verbindung zu Datenbank aufbauen
        Connection dbcon;
        if (dbUsername == null) {
            dbcon = DriverManager.getConnection(dbUrl);
        } else {
            if (dbPassword == null) {
                dbcon = DriverManager.getConnection(dbUrl, dbUsername, "");
            } else {
                dbcon = DriverManager.getConnection(dbUrl, dbUsername,
                        dbPassword);
            }
        }
        StatisticsDatabase db = new StatisticsDatabase(dbcon, gruppen);

        NamiStatistics stats = new NamiStatistics(namicon, db, gruppen);
        // Kommando auslesen
        if (args.length < 1) {
            dbcon.close();
            throw new IllegalArgumentException("No command given");
        }

        Writer csvOut = out;
        if (args.length > 1) {
            // args[1] is output-filename
            String filename = args[1];
            if (!filename.equals("-")) {
                csvOut = new FileWriter(new File(filename));
            }
        }

        switch (args[0]) {
        case "collectData":
            stats.collectData();
            out.println("Daten aus NaMi wurden abgefragt");
            break;
        case "listRuns":
            stats.listRuns();
            break;
        case "statsAsCsv":
            stats.statsAsCsv(args, csvOut, false);
            break;
        case "statsAsCsvCum":
            stats.statsAsCsv(args, csvOut, true);
            break;
        case "historyAsCsv":
            stats.historyAsCsv(args, csvOut, false);
            break;
        case "historyAsCsvCum":
            stats.historyAsCsv(args, csvOut, true);
            break;
        default:
            dbcon.close();
            throw new IllegalArgumentException("Invalid command: " + args[0]);
        }

        // Der CSV-Ausgabe-Stream wird nur dann geschlossen, wenn es nicht der
        // übergebene Ausgabestrom ist (denn dann wird dieser noch gebraucht)
        if (csvOut != out) {
            csvOut.close();
        }
        dbcon.close();
    }

    private void collectData() throws IOException, NamiApiException,
            SQLException {
        namicon.namiLogin();
        NamiGruppierung rootGruppierung = NamiGruppierung
                .getGruppierungen(namicon);
        db.createDatabase(rootGruppierung);

        int runId = db.writeNewStatisticRun();
        writeAnzahlForGruppierungAndChildren(runId, rootGruppierung);
    }

    // verwendet von collectData()
    private void writeAnzahlForGruppierungAndChildren(int runId,
            NamiGruppierung gruppierung) throws NamiApiException, IOException,
            SQLException {
        for (Gruppe gruppe : gruppen) {
            int anzahl = gruppe.getAnzahl(namicon, gruppierung.getId());
            db.writeAnzahl(gruppierung.getId(), gruppe.getId(), runId, anzahl);
        }

        for (NamiGruppierung child : gruppierung.getChildren()) {
            writeAnzahlForGruppierungAndChildren(runId, child);
        }
    }

    private void listRuns() throws SQLException {
        ResultSet rs = db.getRuns();
        while (rs.next()) {
            String str = String.format("%3d  %s", rs.getInt("runId"),
                    rs.getString("datum"));
            System.out.println(str);
        }
    }

    private void statsAsCsv(String[] args, Writer csvOut, boolean cumulate)
            throws SQLException, IOException {

        int runId = -1;
        if (args.length > 2) {
            // use args[2] as runId
            try {
                runId = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Invalid second parameter (runId)");
            }
        }

        CsvWriter csvWriter = new CsvWriter(csvOut);
        ResultSet rs;
        if (runId != -1) {
            rs = db.getStatsAllGruppierungen(runId, cumulate);
        } else {
            rs = db.getStatsAllGruppierungen(cumulate);
        }

        csvWriter.writeResultSet(rs);
    }

    private void historyAsCsv(String[] args, Writer csvOut, boolean cumulate)
            throws IOException, SQLException {
        String gruppierungsnummer;
        if (args.length > 2) {
            // use args[2] as gruppierungsnummer
            gruppierungsnummer = args[2];
        } else {
            throw new IllegalArgumentException(
                    "Missing second parameter (gruppierungsnummer)");
        }

        CsvWriter csvWriter = new CsvWriter(csvOut);
        ResultSet rs = db.getHistory(gruppierungsnummer, cumulate);
        csvWriter.writeResultSet(rs);
    }
}
