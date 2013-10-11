package nami.statistics;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
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
import nami.cli.annotation.CommandDoc;
import nami.cli.annotation.ParamCompleter;
import nami.cli.annotation.ParentCommand;
import nami.configuration.ApplicationDirectoryException;
import nami.configuration.ConfigFormatException;
import nami.configuration.Configuration;
import nami.connector.NamiConnector;
import nami.connector.NamiServer;
import nami.connector.credentials.NamiCredentials;
import nami.connector.exception.NamiApiException;
import nami.connector.namitypes.NamiGruppierung;
import nami.statistics.StatisticsDatabase.Run;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Liest Mitgliederzahlen aus NaMi aus und erstellt Statistiken daraus.
 * 
 * @author Fabian Lipp
 * 
 */
public final class NamiStatistics {
    private NamiStatistics() {
    }

    private static Logger log = Logger
            .getLogger(NamiStatistics.class.getName());

    private static final String DEFAULT_CONFIG_FILENAME = "namistatistics.xml";

    private static CliParser parser = new CliParser(NamiStatistics.class,
            "statistics", new Class<?>[] { NamiConnector.class,
                    PrintWriter.class, StatisticsConfig.class });

    private static File getDefaultConfigFile()
            throws ApplicationDirectoryException {
        return new File(Configuration.getApplicationDirectory(),
                DEFAULT_CONFIG_FILENAME);
    }

    @SuppressWarnings("static-access")
    private static Options createOptions() {
        Options options = new Options();
        Option configfile = OptionBuilder.hasArg().withArgName("filename")
                .withLongOpt("configfile").create('c');
        options.addOption(configfile);

        return options;
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
     * @throws ParseException
     *             Probleme beim Parsen der Kommandozeile
     */
    @CliCommand("statistics")
    @AlternateCommands("stats")
    @CommandDoc("Erstellt Statistiken und gibt diese im CSV-Format aus")
    @ParamCompleter(StatisticsCompleter.class)
    public static void statistics(String[] args, NamiConnector namicon,
            PrintWriter out) throws IOException, ConfigFormatException,
            ApplicationDirectoryException, ParseException {

        Options options = createOptions();
        CommandLineParser clParser = new GnuParser();
        CommandLine cl = clParser.parse(options, args);

        // Konfiguration einlesen
        File configFile;
        if (cl.hasOption('c')) {
            configFile = new File(cl.getOptionValue('c'));
        } else {
            configFile = getDefaultConfigFile();
        }
        StatisticsConfig config = new StatisticsConfig(configFile);

        // Logger-Konfiguration (zu DEBUG-Zwecken)
        Logger dbLogger = Logger.getLogger(StatisticsDatabase.class.getName());
        dbLogger.setLevel(Level.FINEST);
        Handler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        Logger.getLogger("").addHandler(handler);
        Logger.getLogger("").setLevel(Level.ALL);

        parser.callMethod(cl.getArgs(), out, namicon, out, config);
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
     * @param config
     *            Statistik-Konfiguration
     * @throws IOException
     *             IOException
     * @throws NamiApiException
     *             API-Fehler beim Zugriff auf NaMi
     */
    @CliCommand("collectData")
    @ParentCommand("statistics")
    @CommandDoc("Lädt die aktuellen Mitgliederzahlen aus NaMi")
    public static void collectData(String[] args, NamiConnector namicon,
            PrintWriter out, StatisticsConfig config) throws IOException,
            NamiApiException {

        namicon.namiLogin();
        NamiGruppierung rootGruppierung;
        if (config.getRootGrp() == null) {
            rootGruppierung = NamiGruppierung.getGruppierungen(namicon);
        } else {
            rootGruppierung = NamiGruppierung.getGruppierungen(namicon,
                    config.getRootGrp());
        }
        config.getDb().populateDatabase(rootGruppierung);

        long runId = config.getDb().writeNewStatisticRun();
        if (runId != -1) {
            writeAnzahlForGruppierungAndChildren(runId, rootGruppierung,
                    namicon, config.getDb(), config.getGruppen());
        } else {
            log.warning("Could not insert new run into local database");
        }

        log.info("Got data from NaMi");
    }

    // verwendet von collectData()
    // wird rekursiv für jede Gruppierung aufgerufen
    private static void writeAnzahlForGruppierungAndChildren(long runId,
            NamiGruppierung gruppierung, NamiConnector namicon,
            StatisticsDatabase db, Collection<Gruppe> gruppen)
            throws NamiApiException, IOException {
        for (Gruppe gruppe : gruppen) {
            int anzahl = gruppe.getAnzahl(namicon,
                    gruppierung.getGruppierungsnummer());
            db.writeAnzahl(gruppierung.getGruppierungsnummer(), gruppe.getId(),
                    runId, anzahl);
        }

        for (NamiGruppierung child : gruppierung.getChildren()) {
            writeAnzahlForGruppierungAndChildren(runId, child, namicon, db,
                    gruppen);
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
     * @param config
     *            Statistik-Konfiguration
     */
    @CliCommand("listRuns")
    @ParentCommand("statistics")
    @CommandDoc("Listet die durchgeführten Statistik-Läufe auf")
    public static void listRuns(String[] args, NamiConnector namicon,
            PrintWriter out, StatisticsConfig config) {
        List<Run> runs = config.getDb().getRuns();
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
     * @param config
     *            Statistik-Konfiguration
     * @throws IOException
     *             Fehler beim Schreiben in die Ausgabe
     */
    @CliCommand("statsAsCsv")
    @ParentCommand("statistics")
    @CommandDoc("Statistik für alle Gruppierungen")
    public static void statsAsCsv(String[] args, NamiConnector namicon,
            PrintWriter out, StatisticsConfig config) throws IOException {
        statsAsCsv(args, out, false, config);
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
     * @param config
     *            Statistik-Konfiguration
     * @throws IOException
     *             Fehler beim Schreiben in die Ausgabe
     */
    @CliCommand("statsAsCsvCum")
    @ParentCommand("statistics")
    @CommandDoc("Statistik für alle Gruppierungen (kumuliert)")
    public static void statsAsCsvCum(String[] args, NamiConnector namicon,
            PrintWriter out, StatisticsConfig config) throws IOException {
        statsAsCsv(args, out, true, config);
    }

    private static void statsAsCsv(String[] args, PrintWriter out,
            boolean cumulate, StatisticsConfig config) throws IOException {
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
            config.getDb().getStatsAllGruppierungen(runId, cumulate, csvWriter);
        } else {
            config.getDb().getStatsAllGruppierungen(cumulate, csvWriter);
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
     * @param config
     *            Statistik-Konfiguration
     * @throws IOException
     *             Fehler beim Schreiben in die Ausgabe
     */
    @CliCommand("historyAsCsv")
    @ParentCommand("statistics")
    @CommandDoc("Mitgliederzahlen einer Gruppierung im zeitlichen Verlauf")
    public static void historyAsCsv(String[] args, NamiConnector namicon,
            PrintWriter out, StatisticsConfig config) throws IOException {
        historyAsCsv(args, out, false, config);
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
     * @param config
     *            Statistik-Konfiguration
     * @throws IOException
     *             Fehler beim Schreiben in die Ausgabe
     */
    @CliCommand("historyAsCsvCum")
    @ParentCommand("statistics")
    @CommandDoc("Mitgliederzahlen einer Gruppierung im zeitlichen Verlauf (kumuliert)")
    public static void historyAsCsvCum(String[] args, NamiConnector namicon,
            PrintWriter out, StatisticsConfig config) throws IOException {
        historyAsCsv(args, out, true, config);
    }

    private static void historyAsCsv(String[] args, PrintWriter out,
            boolean cumulate, StatisticsConfig config) throws IOException {
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
        config.getDb().getHistory(gruppierungsnummer, cumulate, csvWriter);

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
