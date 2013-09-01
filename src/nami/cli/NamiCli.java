package nami.cli;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jline.console.ConsoleReader;
import jline.console.UserInterruptException;
import jline.console.completer.Completer;
import jline.console.completer.NullCompleter;
import jline.console.completer.StringsCompleter;
import jline.console.history.FileHistory;
import nami.cli.annotation.AlternateCommands;
import nami.cli.annotation.CliCommand;
import nami.cli.annotation.CommandDoc;
import nami.cli.annotation.ParamCompleter;
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

    private static final String HISTORY_FILE = System.getProperty("user.home")
            + "/.namicli_history";
    private static Logger log = Logger.getLogger(NamiCli.class
            .getCanonicalName());

    /**
     * Klassen, in denen die verfügbaren Funktionen deklariert sind. Die
     * aufrufbaren Methoden sind mit der Annotation {@link CliCommand}
     * gekennzeichnet.
     */
    private static final Class<?>[] COMMAND_CLASSES = { Gruppierungen.class,
            Mitglieder.class, EnumListings.class, NamiCli.class,
            NamiStatistics.class };

    /**
     * Enthält die Kommandos, die NamiCli versteht, und die zugeordneten
     * Methoden, die dann aufgerufen werden.
     */
    private static Map<String, Method> commands = new HashMap<>();
    /**
     * Enthält die primären Namen der Kommandos. Hier werden also nur die
     * Kommandos eingefügt, die als Attribut einer {@link CliCommand}-Annotation
     * angegeben sind, nicht die die nur als Attribut einer
     * {@link AlternateCommands}-Annotation angegeben sind.
     * 
     * Diese Liste wird beispielsweise verwendet, um die Hilfe mit der
     * Kurzbeschreibung der Kommandos anzuzeigen.
     */
    private static List<String> commandNames = new LinkedList<>();
    /**
     * Enthält alle Kommandos, also auch die aus {@link AlternateCommands}.
     */
    private static List<String> allCommandNames = new LinkedList<>();
    /**
     * Enthält die Second-Level-Completer für jedes der unterstützten Kommandos.
     */
    private static Map<String, Completer> completersMap = new HashMap<>();

    static {
        // Read available commands from annotations in COMMAND_CLASSES
        for (Class<?> cls : COMMAND_CLASSES) {
            for (Method mtd : cls.getMethods()) {
                CliCommand anno = mtd.getAnnotation(CliCommand.class);
                if (anno != null) {
                    Completer completer = getCompleterForMethod(mtd);
                    addToCommands(anno.value(), mtd, completer, true);

                    AlternateCommands alt = mtd
                            .getAnnotation(AlternateCommands.class);
                    if (alt != null) {
                        for (String str : alt.value()) {
                            addToCommands(str, mtd, completer, false);
                        }
                    }
                }
            }
        }
        Collections.sort(commandNames);
    }

    /**
     * Erzeugt einen Completer für eine CliCommand-Methode. Dazu wird die
     * entsprechende Annotation ausgelesen und mit der dort angegeben
     * Factory-Klasse der Completer erzeugt.
     * 
     * @param mtd
     *            Methode
     * @return Der Completer, falls die Annotation vorhanden war und dieser
     *         erzeugt werden konnte; andernfalls wird ein
     *         <tt>NullCompleter</tt> zurückgegeben.
     */
    private static Completer getCompleterForMethod(Method mtd) {
        Completer completer = NullCompleter.INSTANCE;
        if (mtd.isAnnotationPresent(ParamCompleter.class)) {
            try {
                ParamCompleter anno = mtd.getAnnotation(ParamCompleter.class);
                Class<? extends CompleterFactory> complFacCls = anno.value();
                CompleterFactory complFac = complFacCls.newInstance();
                completer = complFac.getCompleter();
            } catch (InstantiationException | IllegalAccessException e) {
                log.log(Level.WARNING,
                        "Could not initialise completer for command", e);
            }
        }
        return completer;
    }

    /**
     * Fügt ein Kommado in die entsprechenden Listen dieser Klasse ein.
     * 
     * @param commandName
     *            Kommando
     * @param mtd
     *            aufzurufende Methode
     * @param completer
     *            Completer für die Parameter des Kommandos
     * @param primary
     *            <tt>true</tt>, falls dies ein primäres Kommando ist, also als
     *            Attribut von {@link CliCommand} angegeben ist
     */
    private static void addToCommands(String commandName, Method mtd,
            Completer completer, boolean primary) {
        commands.put(commandName.toLowerCase(), mtd);
        allCommandNames.add(commandName);
        if (primary) {
            commandNames.add(commandName);
        }
        completersMap.put(commandName.toLowerCase(), completer);
    }

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

            // Initialise JLine2
            ConsoleReader reader = new ConsoleReader();
            reader.setPrompt("NamiCli> ");
            // Completer Configuration
            Completer completer = new TwoLevelCompleter(new StringsCompleter(
                    allCommandNames), completersMap);
            reader.addCompleter(completer);
            // Commandline History
            FileHistory history = new FileHistory(new File(HISTORY_FILE));
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

                // split line at spaces
                String[] lineSplitted = splitCommandline(line);
                String[] arguments = Arrays.copyOfRange(lineSplitted, 1,
                        lineSplitted.length);

                // Call method for command
                Method mtd = commands.get(lineSplitted[0].toLowerCase());
                if (mtd == null) {
                    out.println("Unknown Command");
                } else {
                    try {
                        mtd.invoke(null, new Object[] { arguments, con, out });
                    } catch (InvocationTargetException e) {
                        // called method throws an exception
                        e.getTargetException().printStackTrace(out);
                    } catch (Exception e) {
                        e.printStackTrace(out);
                    }
                }

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

    /**
     * Gibt die verfügbaren Befehle und eine kurze Zusammenfassung dieser aus.
     * 
     * @param args
     *            nicht verwendet
     * @param con
     *            Verbindung zum NaMi-Server
     * @param out
     *            Writer, auf dem die Ausgabe erfolgt
     */
    @CliCommand("help")
    @CommandDoc("Erklärt die verfügbaren Befehle")
    public static void getHelp(String[] args, NamiConnector con, PrintWriter out) {
        // TODO Werte Argumente aus und zeige ggf. Detail zu Befehl an

        int longestCommand = 0;
        for (String command : commandNames) {
            if (command.length() > longestCommand) {
                longestCommand = command.length();
            }
        }

        out.println("Verfügbare Befehle:");
        out.println();
        String formatString = "  %-" + longestCommand + "s  %s";
        for (String command : commandNames) {
            String description = getShortDescription(command);
            out.println(String.format(formatString, command, description));
        }
    }

    /**
     * Liefert die Beschreibung eines Kommandos aus der {@link CommandDoc}
     * -Annotation.
     * 
     * @param command
     *            Kommando
     * @return der Beschreibungstexts
     */
    private static String getShortDescription(String command) {
        Method mtd = commands.get(command.toLowerCase());
        if (mtd != null) {
            CommandDoc anno = mtd.getAnnotation(CommandDoc.class);
            if (anno != null) {
                return anno.value();
            }
        }

        return "";
    }

    /**
     * Teilt den übergebenen String an den Leerzeichen auf. Leerzeichen die
     * innerhalb von Anführungszeichen stehen, werden ignoriert. Das Ergebnis
     * entspricht quasi dem <tt>args</tt>-Array, das der main-Funktion übergeben
     * wird.
     * 
     * @param line
     *            aufzuteilende Zeile
     * @return an den Leerzeichen aufgeteilte Zeile
     */
    private static String[] splitCommandline(String line) {
        // Quelle:
        // http://stackoverflow.com/questions/366202/regex-for-splitting-
        // a-string-using-space-when-not-surrounded-by-single-or-double
        List<String> matchList = new LinkedList<>();
        Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
        Matcher regexMatcher = regex.matcher(line);
        while (regexMatcher.find()) {
            if (regexMatcher.group(1) != null) {
                // Add double-quoted string without the quotes
                matchList.add(regexMatcher.group(1));
            } else if (regexMatcher.group(2) != null) {
                // Add single-quoted string without the quotes
                matchList.add(regexMatcher.group(2));
            } else {
                // Add unquoted word
                matchList.add(regexMatcher.group());
            }
        }
        return matchList.toArray(new String[0]);
    }
}
