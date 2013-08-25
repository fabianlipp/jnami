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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jline.console.ConsoleReader;
import jline.console.UserInterruptException;
import jline.console.completer.StringsCompleter;
import jline.console.history.FileHistory;
import nami.cli.commands.EnumListings;
import nami.cli.commands.Gruppierungen;
import nami.cli.commands.Mitglieder;
import nami.connector.NamiConnector;
import nami.connector.NamiServer;
import nami.connector.credentials.NamiCredentials;
import nami.connector.credentials.NamiWalletCredentials;
import nami.connector.exception.NamiLoginException;

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

    /**
     * Klassen, in denen die verfügbaren Funktionen deklariert sind. Die
     * aufrufbaren Methoden sind mit der Annotation {@link CliCommand}
     * gekennzeichnet.
     */
    private static final Class<?>[] COMMAND_CLASSES = { Gruppierungen.class,
            Mitglieder.class, EnumListings.class, NamiCli.class };

    private static Map<String, Method> commands = new HashMap<>();
    private static List<String> commandNames = new LinkedList<>();
    private static List<String> allCommandNames = new LinkedList<>();

    static {
        // Read available commands from annotations in COMMAND_CLASSES
        for (Class<?> cls : COMMAND_CLASSES) {
            for (Method mtd : cls.getMethods()) {
                if (mtd.isAnnotationPresent(CliCommand.class)) {
                    CliCommand anno = mtd.getAnnotation(CliCommand.class);
                    commands.put(anno.value().toLowerCase(), mtd);
                    commandNames.add(anno.value());
                    allCommandNames.add(anno.value());

                    AlternateCommands alt = mtd
                            .getAnnotation(AlternateCommands.class);
                    if (alt != null) {
                        for (String str : alt.value()) {
                            commands.put(str.toLowerCase(), mtd);
                            allCommandNames.add(str);
                        }
                    }
                }
            }
        }
        Collections.sort(commandNames);
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
        // !!! TODO !!! NaMi-Username in Code -> in Konfigurationsdatei
        // auslagern
        // NamiCredentials credentials = new NamiWalletCredentials("214023");
        NamiCredentials credentials = new NamiWalletCredentials("");
        // NamiCredentials credentials = new NamiConsoleCredentials();
        // NamiConnector con = new NamiConnector(NamiServer.TESTSERVER,
        // credentials);
        NamiConnector con = new NamiConnector(NamiServer.LIVESERVER,
                credentials);
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
        reader.addCompleter(new StringsCompleter(allCommandNames));
        FileHistory history = new FileHistory(new File(HISTORY_FILE));
        reader.setHistory(history);
        PrintWriter out = new PrintWriter(reader.getOutput());
        reader.setHandleUserInterrupt(true);

        String line = null;
        while (true) {
            try {
                line = reader.readLine();
            } catch (UserInterruptException e) {
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

    private static String getShortDescription(String command) {
        Method mtd = commands.get(command.toLowerCase());
        if (mtd == null) {
            return "";
        } else {
            CommandDoc anno = mtd.getAnnotation(CommandDoc.class);
            if (anno != null) {
                return anno.value();
            } else {
                return "";
            }
        }
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
