package nami.cli;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import jline.console.ConsoleReader;
import jline.console.UserInterruptException;
import jline.console.history.FileHistory;
import nami.cli.commands.EnumListings;
import nami.cli.commands.Gruppierungen;
import nami.cli.commands.Mitglieder;
import nami.configuration.ApplicationDirectoryException;
import nami.configuration.Configuration;
import nami.connector.NamiConnector;
import nami.connector.NamiServer;
import nami.connector.credentials.NamiCredentials;
import nami.connector.exception.CredentialsInitiationException;
import nami.connector.exception.NamiLoginException;
import nami.statistics.NamiStatistics;

// TODO: Use commandline arguments instead of shell as alternative

/**
 * Stellt ein Kommandozeileninterface zu NaMi bereit.
 * 
 * @author Fabian Lipp
 * 
 */
public final class NamiCli {
    private NamiCli() {
    }

    /**
     * Dateiname im Konfigurationsverzeichnis, in dem die Kommandozeilen-History
     * gespeichert wird.
     */
    private static final String HISTORY_FILENAME = "namicli_history";
    // private static Logger log = Logger.getLogger(NamiCli.class.getName());

    /**
     * Klassen, in denen die verfügbaren Funktionen deklariert sind. Die
     * aufrufbaren Methoden sind mit der Annotation {@link CliCommand}
     * gekennzeichnet.
     */
    private static final Class<?>[] COMMAND_CLASSES = { Gruppierungen.class,
            Mitglieder.class, EnumListings.class, NamiStatistics.class };

    /**
     * Hauptfunktion des Programms.
     * 
     * @param args
     *            Übergebene Parameter
     * @throws IOException
     *             Ein-/Ausgabefehler
     */
    public static void main(String[] args) throws IOException {
        try {
            File logFile = Configuration.getLogfile();
            FileHandler fh = new FileHandler(logFile.getAbsolutePath(), true);
            fh.setLevel(Level.ALL);
            Logger.getLogger("").addHandler(fh);
            Logger.getLogger("").info("Running NamiCli");

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

            try {
                con.namiLogin();
            } catch (NamiLoginException e) {
                System.err.println("Could not login into NaMi:");
                e.printStackTrace();
                System.err.flush();
                System.exit(1);
            }

            CliParser parser = new CliParser(COMMAND_CLASSES, new Class<?>[] {
                    NamiConnector.class, PrintWriter.class });

            // Initialise JLine2
            ConsoleReader reader = new ConsoleReader();
            reader.setPrompt("NamiCli> ");
            // Completer Configuration
            reader.addCompleter(parser.getCompleter());
            // Commandline History
            File historyFile = new File(
                    Configuration.getApplicationDirectory(), HISTORY_FILENAME);
            FileHistory history = new FileHistory(historyFile);
            reader.setHistory(history);
            PrintWriter out = new PrintWriter(reader.getOutput());
            // needed to enable Ctrl+C
            reader.setHandleUserInterrupt(true);

            String line = null;
            while (true) {
                try {
                    line = reader.readLine();
                } catch (UserInterruptException e) {
                    out.println("^C");
                    line = "";
                }
                if (line == null) {
                    break;
                }

                // Handle empty und exit commands
                String lineTrimmed = line.trim();
                if (lineTrimmed.isEmpty()) {
                    continue;
                } else if (lineTrimmed.equalsIgnoreCase("quit")
                        || lineTrimmed.equalsIgnoreCase("exit")) {
                    break;
                }

                parser.callMethod(line, out, con, out);
            }

            out.println();
            out.flush();

            // Speichere History in Datei
            history.flush();

            System.exit(0);
        } catch (ApplicationDirectoryException e) {
            System.err.println("Cannot write to logfile");
            e.printStackTrace();
            System.exit(1);
        } catch (CredentialsInitiationException e) {
            System.err.println("Could not use credentials from config file: ");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
