package nami.cli.commands;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

import nami.cli.AlternateCommands;
import nami.cli.CliCommand;
import nami.cli.CommandDoc;
import nami.connector.NamiConnector;
import nami.connector.exception.NamiApiException;
import nami.connector.namitypes.NamiGruppierung;

/**
 * Funktionen zum Anzeigen von Informationen zu Gruppierungen.
 * 
 * @author Fabian Lipp
 * 
 */
public final class Gruppierungen {
    private Gruppierungen() {
    }

    /**
     * Listet die verfügbaren Gruppierungen baumförmig auf.
     * 
     * @param args
     *            nicht verwendet
     * @param con
     *            Verbindung zum NaMi-Server
     * @param out
     *            Writer, auf dem die Ausgabe erfolgt
     * @throws NamiApiException
     *             API-Fehler beim Zugriff auf NaMi
     * @throws IOException
     *             IOException
     */
    @CliCommand("listGruppierungen")
    @AlternateCommands("lg")
    @CommandDoc("Listet die verfügbaren Gruppierungen auf")
    public static void listGruppierungen(String[] args, NamiConnector con,
            PrintWriter out) throws NamiApiException, IOException {
        NamiGruppierung rootGruppierung = NamiGruppierung.getGruppierungen(con);
        printGruppierungenTeilbaum(out, rootGruppierung, 0);
    }

    /**
     * Gibt einen Teilbaum des Gruppierungsbaums aus.
     * 
     * @param out
     *            Ausgabe-Writer
     * @param root
     *            Wurzel des Teilbaums
     * @param depth
     *            Tiefe der Wurzel
     */
    private static void printGruppierungenTeilbaum(PrintWriter out,
            NamiGruppierung root, int depth) {
        StringBuffer line = new StringBuffer();
        for (int i = 1; i <= depth; i++) {
            line.append("  ");
        }
        line.append(root.getDescriptor()).append(" [");
        line.append(root.getId()).append("]");
        out.println(line);

        Iterator<NamiGruppierung> iter = root.getChildren().iterator();
        while (iter.hasNext()) {
            NamiGruppierung grp = iter.next();
            printGruppierungenTeilbaum(out, grp, depth + 1);
        }
    }
}
