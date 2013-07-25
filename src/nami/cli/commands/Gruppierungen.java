package nami.cli.commands;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

import nami.connector.NamiConnector;
import nami.connector.exception.NamiApiException;
import nami.connector.namitypes.NamiGruppierung;

public class Gruppierungen {
    public static void listGruppierungen(String[] args, NamiConnector con,
            PrintWriter out) throws NamiApiException, IOException {
        // TODO: Argument mit einbeziehen
        NamiGruppierung rootGruppierung = NamiGruppierung.getGruppierungen(con);
        printGruppierungenTeilbaum(out, rootGruppierung, 0, true);
    }

    private static void printGruppierungenTeilbaum(PrintWriter out,
            NamiGruppierung root, int depth, boolean lastChild) {
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
            printGruppierungenTeilbaum(out, grp, depth + 1, !iter.hasNext());
        }
    }
}
