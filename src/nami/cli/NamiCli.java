package nami.cli;

import java.io.PrintWriter;
import java.util.Arrays;

import jline.console.ConsoleReader;
import jline.console.completer.StringsCompleter;
import nami.cli.commands.Gruppierungen;
import nami.connector.NamiConnector;
import nami.connector.NamiServer;
import nami.connector.credentials.NamiConsoleCredentials;
import nami.connector.credentials.NamiCredentials;
import nami.connector.credentials.NamiWalletCredentials;

public class NamiCli {
    public static void usage() {
        System.out.println("Usage: java " + NamiCli.class.getName()
                + " [none/simple/files/dictionary [trigger mask]]");
        System.out.println("  none - no completors");
        System.out.println("  simple - a simple completor that comples "
                + "\"foo\", \"bar\", and \"baz\"");
        System.out
                .println("  files - a completor that comples " + "file names");
        System.out.println("  classes - a completor that comples "
                + "java class names");
        System.out
                .println("  trigger - a special word which causes it to assume "
                        + "the next line is a password");
        System.out.println("  mask - is the character to print in place of "
                + "the actual password character");
        System.out.println("  color - colored prompt and feedback");
        System.out.println("\n  E.g - java Example simple su '*'\n"
                + "will use the simple compleator with 'su' triggering\n"
                + "the use of '*' as a password mask.");
    }

    public static void main(String[] args) {
        try {
            Character mask = null;
            String trigger = null;

            ConsoleReader reader = new ConsoleReader();

            reader.setPrompt("NamiCli> ");
            reader.addCompleter(new StringsCompleter("listGruppierungen"));

            String line;
            PrintWriter out = new PrintWriter(reader.getOutput());

            // !!! TODO !!! NaMi-Username in Code
            NamiCredentials credentials = new NamiWalletCredentials("214023");
            //NamiCredentials credentials = new NamiConsoleCredentials();
            NamiConnector con = new NamiConnector(NamiServer.TESTSERVER, credentials);
            con.namiLogin();
            
            while ((line = reader.readLine()) != null) {
                String[] lineSplitted = line.trim().split("\\p{Space}+");

                out.println("command: " + lineSplitted[0]);
                for (int i = 1; i < lineSplitted.length; i++) {
                    out.println("argument: " + lineSplitted[i]);
                }
                out.flush();

                String[] arguments = Arrays.copyOfRange(lineSplitted, 1,
                        lineSplitted.length);
                // TODO: Verwende Reflection, um Funktionen aufzurufen?
                switch (lineSplitted[0]) {
                case "listGruppierungen":
                    Gruppierungen.listGruppierungen(args, con, out);
                    break;
                default:
                    out.println("unknown command");
                }

                // If we input the special word then we will mask
                // the next line.
                if ((trigger != null) && (line.compareTo(trigger) == 0)) {
                    line = reader.readLine("password> ", mask);
                }
                if (line.equalsIgnoreCase("quit")
                        || line.equalsIgnoreCase("exit")) {
                    break;
                }
            }

            out.println();
            out.flush();
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        } 
        
        System.exit(0);
    }
}
