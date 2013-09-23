package nami.statistics;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import jline.console.completer.Completer;
import nami.cli.CliParser;
import nami.cli.CompleterFactory;
import nami.cli.annotation.AlternateCommands;
import nami.cli.annotation.CliCommand;
import nami.cli.annotation.CommandDoc;
import nami.cli.annotation.ParamCompleter;
import nami.cli.annotation.ParentCommand;
import nami.configuration.ApplicationDirectoryException;
import nami.configuration.ConfigFormatException;
import nami.configuration.Configuration;
import nami.connector.Geschlecht;
import nami.connector.NamiConnector;
import nami.connector.NamiServer;
import nami.connector.credentials.NamiCredentials;
import nami.connector.exception.NamiApiException;
import nami.connector.namitypes.NamiGruppierung;
import nami.connector.namitypes.NamiSearchedValues;
import nami.db.schema.SchemaUpdater;
import nami.statistics.StatisticsDatabase.Run;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
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
    private NamiStatistics() {
    }

    private static SqlSessionFactory sqlSessionFactory;

    private static StatisticsDatabase db = null;
    private static Collection<Gruppe> gruppen = null;
    /**
     * Wurzel-Gruppierung, für die die Statistik erstellt wird. Wenn die
     * Wurzel-Gruppierung <tt>null</tt> ist, wird der gesamte verfügbare
     * Gruppierungsbaum ausgewertet.
     */
    private static String rootGrp = null;

    private static Logger log = Logger
            .getLogger(NamiStatistics.class.getName());
    private static CliParser parser = new CliParser(NamiStatistics.class,
            "statistics");

    private static final String CONFIG_FILENAME = "namistatistics.xml";
    private static final URL XSDFILE = NamiStatistics.class
            .getResource("namistatistics.xsd");
    private static final String MYBATIS_CONFIGFILE = "mybatis-config.xml";

    private static void readConfig() throws ApplicationDirectoryException,
            ConfigFormatException, IOException {
        // check if config already parsed
        if (db != null && gruppen != null) {
            return;
        }

        // Lese Konfigurationsdatei ein (inkl. Validierung)
        Document doc;
        try {
            XMLReaderJDOMFactory schemafac = new XMLReaderXSDFactory(XSDFILE);
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
        rootGrp = namistatisticsEl.getAttributeValue("root");

        // Datenbankverbindung aus XML lesen
        Element databaseEl = namistatisticsEl.getChild("database");
        String dbDriver = databaseEl.getChildText("driver");
        String dbUrl = databaseEl.getChildText("url");
        String dbUsername = databaseEl.getChildText("username");
        String dbPassword = databaseEl.getChildText("password");

        // Gruppen aus XML einlesen
        gruppen = new LinkedList<>();
        TreeSet<Integer> gruppenIds = new TreeSet<>();
        for (Element gruppeEl : namistatisticsEl.getChildren("gruppe")) {
            // ID der Gruppe
            int gruppeId = Integer.parseInt(gruppeEl.getAttributeValue("id"));
            if (gruppenIds.contains(gruppeId)) {
                throw new ConfigFormatException("Duplicate ID in config file: "
                        + gruppeId);
            }
            gruppenIds.add(gruppeId);

            // Gruppenbezeichnung
            String bezeichnung = gruppeEl.getAttributeValue("bezeichnung");

            // Suchausdrücke
            List<NamiSearchedValues> searches = new LinkedList<>();
            for (Element namiSearchEl : gruppeEl.getChildren("namiSearch")) {
                searches.add(NamiSearchedValues.fromXml(namiSearchEl));
            }

            // Filter
            Element filterEl;
            List<Filter> filters = new LinkedList<>();
            filterEl = gruppeEl.getChild("geschlechtFilter");
            if (filterEl != null) {
                String value = filterEl.getAttributeValue("value");
                filters.add(new GeschlechtFilter(Geschlecht.fromString(value)));
            }

            gruppen.add(new Gruppe(gruppeId, bezeichnung, searches, filters));
        }

        // Update Database Schema
        SchemaUpdater updater = new SchemaUpdater(dbDriver, dbUrl, dbUsername,
                dbPassword);
        updater.update();

        // Initialise MyBatis
        Properties prop = new Properties();
        prop.setProperty("driver", dbDriver);
        prop.setProperty("url", dbUrl);
        prop.setProperty("username", dbUsername);
        prop.setProperty("password", dbPassword);
        InputStream is = NamiStatistics.class
                .getResourceAsStream(MYBATIS_CONFIGFILE);
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(is, prop);

        db = new StatisticsDatabase(gruppen, sqlSessionFactory);
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
            statistics(args, con, out);
            out.close();
            System.exit(0);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Liefert den Completer, der die Kommandos vervollständigt, die
     * NamiStatistics versteht.
     * 
     */
    public static class StatisticsCompleter implements CompleterFactory {
        @Override
        public Completer getCompleter() {
            return parser.getCompleter();
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
     * @throws ApplicationDirectoryException
     *             Probleme beim Zugriff auf das Konfigurationsverzeichnis
     */
    @CliCommand("statistics")
    @AlternateCommands("stats")
    @CommandDoc("Erstellt Statistiken und gibt diese im CSV-Format aus")
    @ParamCompleter(StatisticsCompleter.class)
    public static void statistics(String[] args, NamiConnector namicon,
            PrintWriter out) throws IOException, ConfigFormatException,
            ApplicationDirectoryException {

        readConfig();

        Logger dbLogger = Logger.getLogger(StatisticsDatabase.class.getName());
        dbLogger.setLevel(Level.FINEST);
        Handler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        Logger.getLogger("").addHandler(handler);
        Logger.getLogger("").setLevel(Level.ALL);

        parser.callMethod(args, namicon, out);
    }

    /**
     * Ermittelt die aktuelle Größe der Gruppen in NaMi und speichert sie in die
     * Datenbank.
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
     *             API-Fehler beim Zugriff auf NaMi
     */
    @CliCommand("collectData")
    @ParentCommand("statistics")
    @CommandDoc("Lädt die aktuellen Mitgliederzahlen aus NaMi")
    public static void collectData(String[] args, NamiConnector namicon,
            PrintWriter out) throws IOException, NamiApiException {

        namicon.namiLogin();
        NamiGruppierung rootGruppierung;
        if (rootGrp == null) {
            rootGruppierung = NamiGruppierung.getGruppierungen(namicon);
        } else {
            rootGruppierung = NamiGruppierung
                    .getGruppierungen(namicon, rootGrp);
        }
        db.populateDatabase(rootGruppierung);

        long runId = db.writeNewStatisticRun();
        if (runId != -1) {
            writeAnzahlForGruppierungAndChildren(runId, rootGruppierung,
                    namicon);
        } else {
            log.warning("Could not insert new run into local database");
        }

        log.info("Got data from NaMi");
    }

    // verwendet von collectData()
    // wird rekursiv für jede Gruppierung aufgerufen
    private static void writeAnzahlForGruppierungAndChildren(long runId,
            NamiGruppierung gruppierung, NamiConnector namicon)
            throws NamiApiException, IOException {
        for (Gruppe gruppe : gruppen) {
            int anzahl = gruppe.getAnzahl(namicon, gruppierung.getId());
            db.writeAnzahl(gruppierung.getId(), gruppe.getId(), runId, anzahl);
        }

        for (NamiGruppierung child : gruppierung.getChildren()) {
            writeAnzahlForGruppierungAndChildren(runId, child, namicon);
        }
    }

    /**
     * Listet die durchgeführten Statistik-Läufe mit ID und Datum auf.
     * 
     * @param args
     *            nicht verwendet
     * @param namicon
     *            Verbindung zum NaMi-Server
     * @param out
     *            Writer, auf dem die Ausgabe erfolgt
     */
    @CliCommand("listRuns")
    @ParentCommand("statistics")
    @CommandDoc("Listet die durchgeführten Statistik-Läufe auf")
    public static void listRuns(String[] args, NamiConnector namicon,
            PrintWriter out) {
        List<Run> runs = db.getRuns();
        for (Run run : runs) {
            String str = String.format("%3d  %s", run.getRunId(),
                    run.getDatum());
            out.println(str);
        }
    }

    /**
     * Liefert die Statistik für alle Gruppierungen zu einem bestimmten
     * Zeitpunkt aus der Datenbank.
     * 
     * @param args
     *            Parameter:
     *            <ul>
     *            <li>1: Dateiname für die CSV-Ausgabe (bei '-' erfolgt die
     *            Ausgabe in <tt>out</tt>)</li>
     *            <li>2 (optional): RunId, die den Zeitpunkt angibt, zu dem die
     *            Statistik ermittelt werden soll</li>
     *            </ul>
     * @param namicon
     *            Verbindung zum NaMi-Server
     * @param out
     *            Writer, auf dem die Ausgabe erfolgt
     * @throws IOException
     *             Fehler beim Schreiben in die Ausgabe
     */
    @CliCommand("statsAsCsv")
    @ParentCommand("statistics")
    @CommandDoc("Statistik für alle Gruppierungen")
    public static void statsAsCsv(String[] args, NamiConnector namicon,
            PrintWriter out) throws IOException {
        statsAsCsv(args, out, false);
    }

    /**
     * Liefert die Statistik für alle Gruppierungen zu einem bestimmten
     * Zeitpunkt aus der Datenbank. Dabei werden bei den Zahlen der
     * Gruppierungen alle untergeordneten Gruppierungen mitgezählt.
     * 
     * @param args
     *            Parameter:
     *            <ul>
     *            <li>1: Dateiname für die CSV-Ausgabe (bei '-' erfolgt die
     *            Ausgabe in <tt>out</tt>)</li>
     *            <li>2 (optional): RunId, die den Zeitpunkt angibt, zu dem die
     *            Statistik ermittelt werden soll</li>
     *            </ul>
     * @param namicon
     *            Verbindung zum NaMi-Server
     * @param out
     *            Writer, auf dem die Ausgabe erfolgt
     * @throws IOException
     *             Fehler beim Schreiben in die Ausgabe
     */
    @CliCommand("statsAsCsvCum")
    @ParentCommand("statistics")
    @CommandDoc("Statistik für alle Gruppierungen (kumuliert)")
    public static void statsAsCsvCum(String[] args, NamiConnector namicon,
            PrintWriter out) throws IOException {
        statsAsCsv(args, out, true);
    }

    private static void statsAsCsv(String[] args, PrintWriter out,
            boolean cumulate) throws IOException {
        Writer csvOut = getOutputWriter(args, out);

        int runId = -1;
        if (args.length > 1) {
            // use args[1] as runId
            try {
                runId = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Invalid second parameter (runId)");
            }
        }

        CsvWriter csvWriter = new CsvWriter(csvOut);
        if (runId != -1) {
            db.getStatsAllGruppierungen(runId, cumulate, csvWriter);
        } else {
            db.getStatsAllGruppierungen(cumulate, csvWriter);
        }

        // Der CSV-Ausgabe-Stream wird nur dann geschlossen, wenn es nicht der
        // übergebene Ausgabestrom ist (denn dann wird dieser noch gebraucht)
        if (csvOut != out) {
            csvOut.close();
        }
    }

    /**
     * Liefert die Mitgliederzahlen einer bestimmten Gruppierung im zeitlichen
     * Verlauf aus der Datenbank. Dabei werden bei den Zahlen der Gruppierungen
     * alle untergeordneten Gruppierungen mitgezählt.
     * 
     * @param args
     *            Parameter:
     *            <ul>
     *            <li>1: Dateiname für die CSV-Ausgabe (bei '-' erfolgt die
     *            Ausgabe in <tt>out</tt>)</li>
     *            <li>2: Gruppierung</li>
     *            </ul>
     * @param namicon
     *            Verbindung zum NaMi-Server
     * @param out
     *            Writer, auf dem die Ausgabe erfolgt
     * @throws IOException
     *             Fehler beim Schreiben in die Ausgabe
     */
    @CliCommand("historyAsCsv")
    @ParentCommand("statistics")
    @CommandDoc("Mitgliederzahlen einer Gruppierung im zeitlichen Verlauf")
    public static void historyAsCsv(String[] args, NamiConnector namicon,
            PrintWriter out) throws IOException {
        historyAsCsv(args, out, false);
    }

    /**
     * Liefert die Mitgliederzahlen einer bestimmten Gruppierung im zeitlichen
     * Verlauf aus der Datenbank.
     * 
     * @param args
     *            Parameter:
     *            <ul>
     *            <li>1: Dateiname für die CSV-Ausgabe (bei '-' erfolgt die
     *            Ausgabe in <tt>out</tt>)</li>
     *            <li>2: Gruppierung</li>
     *            </ul>
     * @param namicon
     *            Verbindung zum NaMi-Server
     * @param out
     *            Writer, auf dem die Ausgabe erfolgt
     * @throws IOException
     *             Fehler beim Schreiben in die Ausgabe
     */
    @CliCommand("historyAsCsvCum")
    @ParentCommand("statistics")
    @CommandDoc("Mitgliederzahlen einer Gruppierung im zeitlichen Verlauf (kumuliert)")
    public static void historyAsCsvCum(String[] args, NamiConnector namicon,
            PrintWriter out) throws IOException {
        historyAsCsv(args, out, true);
    }

    private static void historyAsCsv(String[] args, PrintWriter out,
            boolean cumulate) throws IOException {
        Writer csvOut = getOutputWriter(args, out);

        String gruppierungsnummer;
        if (args.length > 1) {
            // use args[1] as gruppierungsnummer
            gruppierungsnummer = args[1];
        } else {
            throw new IllegalArgumentException(
                    "Missing second parameter (gruppierungsnummer)");
        }

        CsvWriter csvWriter = new CsvWriter(csvOut);
        db.getHistory(gruppierungsnummer, cumulate, csvWriter);

        // Der CSV-Ausgabe-Stream wird nur dann geschlossen, wenn es nicht der
        // übergebene Ausgabestrom ist (denn dann wird dieser noch gebraucht)
        if (csvOut != out) {
            csvOut.close();
        }
    }

    /**
     * Lese die Ausgabedatei bzw. -strom aus der Eingabezeile und öffne einen
     * passenden Writer.
     * 
     * @param args
     *            Kommandozeile
     * @return Writer, der args[0] entspricht
     * @throws IOException
     */
    private static Writer getOutputWriter(String[] args, PrintWriter out)
            throws IOException {
        Writer csvOut = out;
        if (args.length > 0) {
            // args[0] is output-filename
            String filename = args[0];
            if (!filename.equals("-")) {
                csvOut = new FileWriter(new File(filename));
            }
        }
        return csvOut;
    }
}
