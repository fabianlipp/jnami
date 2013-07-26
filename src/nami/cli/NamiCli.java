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

import jline.console.ConsoleReader;
import jline.console.completer.StringsCompleter;
import jline.console.history.FileHistory;
import nami.cli.commands.Gruppierungen;
import nami.connector.NamiConnector;
import nami.connector.NamiServer;
import nami.connector.credentials.NamiCredentials;
import nami.connector.credentials.NamiWalletCredentials;
import nami.connector.exception.NamiLoginException;

// TODO: "help" (list commands)
// TODO: Use commandline arguments instead of shell as alternative

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
            NamiCli.class };

    private static Map<String, Method> commands = new HashMap<>();
    private static List<String> commandNames = new LinkedList<>();
    private static List<String> allCommandNames = new LinkedList<>();

    public static void main(String[] args) throws IOException {

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

        // Initialis JLine2
        ConsoleReader reader = new ConsoleReader();
        reader.setPrompt("NamiCli> ");
        reader.addCompleter(new StringsCompleter(allCommandNames));
        FileHistory history = new FileHistory(new File(HISTORY_FILE));
        reader.setHistory(history);
        PrintWriter out = new PrintWriter(reader.getOutput());

        // !!! TODO !!! NaMi-Username in Code
        NamiCredentials credentials = new NamiWalletCredentials("214023");
        // NamiCredentials credentials = new NamiConsoleCredentials();
        NamiConnector con = new NamiConnector(NamiServer.TESTSERVER,
                credentials);
        try {
            con.namiLogin();
        } catch (NamiLoginException e) {
            out.println("Could not login into NaMi:");
            e.printStackTrace(out);
            System.exit(1);
        }

        String line;
        while ((line = reader.readLine()) != null) {
            String[] lineSplitted = line.trim().split("\\p{Space}+");
            String[] arguments = Arrays.copyOfRange(lineSplitted, 1,
                    lineSplitted.length);

            // Handle empty und exit commands
            String lineTrimmed = line.trim();
            if (lineTrimmed.isEmpty()) {
                continue;
            } else if (lineTrimmed.equalsIgnoreCase("quit")
                    || lineTrimmed.equalsIgnoreCase("exit")) {
                break;
            }

            // Call method for command
            Method mtd = commands.get(lineSplitted[0].toLowerCase());
            if (mtd == null) {
                out.println("Unknown Command");
            } else {
                try {
                    mtd.invoke(null, new Object[] { arguments, con, out });
                } catch (InvocationTargetException e) {
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
        /*
         * } catch (Throwable t) { t.printStackTrace(); System.exit(1); }
         */

        System.exit(0);
    }

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
}
