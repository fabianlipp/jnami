package nami.cli.commands;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.cli.ParseException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import nami.cli.annotation.CliCommand;
import nami.cli.annotation.CommandDoc;
import nami.connector.Ebene;
import nami.connector.NamiConnector;
import nami.connector.exception.NamiApiException;
import nami.connector.namitypes.NamiGruppierung;
import nami.connector.namitypes.NamiMitgliedListElement;
import nami.connector.namitypes.NamiSearchedValues;

/**
 * Funktionen zum Suchen und Anzeigen von Mitgliedern.
 * 
 * @author Fabian Lipp
 */
public final class Vorstandsmailadressen {
    private Vorstandsmailadressen() {
    }

    /**
     * Objekt, das eine Gruppierung mit den zugehörigen Vorstandsmitgliedern
     * verbindet. Wird für den JSON-Export verwendet.
     */
    @SuppressWarnings("unused")
    private static class Stamm {
        private NamiGruppierung grp;
        private List<NamiMitgliedListElement> vorstand;
    }

    private static List<Stamm> processGruppierung(NamiGruppierung grp,
            NamiConnector con) throws NamiApiException, IOException {
        List<Stamm> staemme = new LinkedList<>();
        if (grp.getEbene() == Ebene.STAMM) {
            NamiSearchedValues search = new NamiSearchedValues();
            search.setTaetigkeitId(13); // Vorsitzender
            search.setUntergliederungId(5); // Vorstand
            search.setGruppierungsnummer(grp.getGruppierungsnummer());
            search.setMitAllenTaetigkeiten(true);

            Collection<NamiMitgliedListElement> mitglieder =
                    search.getAllResults(con);
            Stamm stamm = new Stamm();
            stamm.grp = grp;
            List<NamiMitgliedListElement> vorstand = new LinkedList<>();
            for (NamiMitgliedListElement mgl : mitglieder) {
                vorstand.add(mgl);
            }

            search.setTaetigkeitId(11); // Kurat(in)
            mitglieder = search.getAllResults(con);
            for (NamiMitgliedListElement mgl : mitglieder) {
                vorstand.add(mgl);
            }
            stamm.vorstand = vorstand;
            staemme.add(stamm);
        }
        for (NamiGruppierung childGrp : grp.getChildren()) {
            staemme.addAll(processGruppierung(childGrp, con));
        }
        return staemme;
    }

    /**
     * Liest die Daten der Vorstände für alle sichtbaren Stämme aus.
     * 
     * @param args
     *            ein Parameter: Ausgabedatei, in die das Ergebnis als JSON
     *            geschrieben wird
     * @param con
     *            Verbindung zum NaMi-Server
     * @param out
     *            Writer, auf dem die Ausgabe erfolgt
     * @throws NamiApiException
     *             API-Fehler beim Zugriff auf NaMi
     * @throws IOException
     *             IOException
     * @throws ParseException
     *             Fehler beim Parsen der Kommandozeile
     */
    @CliCommand("vorstandsMailadressen")
    @CommandDoc("Liefert Mailadressen aller Vorstände")
    public static void searchMitglied(String[] args, NamiConnector con,
            PrintWriter out) throws NamiApiException, IOException {

        if (args.length < 1) {
            throw new IllegalArgumentException(
                    "Ausgabedatei als Parameter übergeben");
        }
        String filename = args[0];

        NamiGruppierung root = NamiGruppierung.getGruppierungen(con);
        List<Stamm> staemme = processGruppierung(root, con);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String s = gson.toJson(staemme);

        File f = new File(filename);
        FileOutputStream fOut = new FileOutputStream(f);
        OutputStreamWriter writer = new OutputStreamWriter(fOut);
        writer.append(s);
        writer.close();
        fOut.close();

        out.println("Vorstände in Datei geschrieben");
    }

}
