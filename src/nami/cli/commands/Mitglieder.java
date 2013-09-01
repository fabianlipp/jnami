package nami.cli.commands;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;

import nami.cli.annotation.AlternateCommands;
import nami.cli.annotation.CliCommand;
import nami.cli.annotation.CommandDoc;
import nami.connector.NamiConnector;
import nami.connector.exception.NamiApiException;
import nami.connector.namitypes.NamiAbstractMitglied;
import nami.connector.namitypes.NamiMitglied;
import nami.connector.namitypes.NamiMitgliedListElement;
import nami.connector.namitypes.NamiSearchedValues;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Funktionen zum Suchen und Anzeigen von Mitgliedern.
 * 
 * @author Fabian Lipp
 */
public final class Mitglieder {
    private Mitglieder() {
    }

    @SuppressWarnings("static-access")
    private static Options createOptions() {
        Options options = new Options();
        Option nachname = OptionBuilder.hasArg().withArgName("nachname")
                .withLongOpt("nachname").create('n');
        options.addOption(nachname);
        Option vorname = OptionBuilder.hasArg().withArgName("vorname")
                .withLongOpt("vorname").create('v');
        options.addOption(vorname);

        return options;
    }

    private static NamiSearchedValues parseOptions(CommandLine cl) {
        NamiSearchedValues search = new NamiSearchedValues();
        // checks if any option is set (otherwise we print the help)
        boolean optionSet = false;

        if (cl.hasOption('n')) {
            search.setNachname(cl.getOptionValue('n'));
            optionSet = true;
        }
        if (cl.hasOption('v')) {
            search.setVorname(cl.getOptionValue('v'));
            optionSet = true;
        }

        String[] args = cl.getArgs();
        if (args.length == 1) {
            try {
                Integer.parseInt(args[0]); // exception thrown if not an integer
                search.setMitgliedsnummer(args[0]);
                optionSet = true;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Ungültige Mitgliedsnummer: " + args[0]);
            }
        } else if (args.length > 1) {
            throw new IllegalArgumentException(
                    "Nur ein Argument zulässig (Mitgliedsnummer)");
        }

        if (optionSet) {
            // one of the options (or mitgliedsnummer) was given
            return search;
        } else {
            return null;
        }

    }

    /**
     * Sucht nach Mitgliedern und listet deren Daten auf. Als Parameter der
     * Suche können bestimmte Felder aus den Stammdaten genutzt werden.
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
     * @throws ParseException
     *             Fehler beim Parsen der Kommandozeile
     */
    @CliCommand("search")
    @AlternateCommands({ "se", "getMitglied", "gm" })
    @CommandDoc("Sucht Mitglieder in NaMi")
    public static void searchMitglied(String[] args, NamiConnector con,
            PrintWriter out) throws NamiApiException, IOException,
            ParseException {

        Options options = createOptions();

        CommandLineParser parser = new GnuParser();
        CommandLine cl = parser.parse(options, args);

        NamiSearchedValues search = parseOptions(cl);
        if (search == null) {
            // keine Einstellungen für die Suche angegeben
            HelpFormatter hf = new HelpFormatter();
            hf.printHelp("search [OPTION] [Mitgliedsnummer]", options);
        } else {
            Collection<NamiMitgliedListElement> mitglieder = search
                    .getAllResults(con);
            if (mitglieder.size() == 0) {
                // Fehlermeldung: kein Mitglied gefunden
                out.println("Kein Mitglied gefunden.");
            } else if (mitglieder.size() == 1) {
                // gebe Mitglied mit allen Details aus
                NamiMitglied mgl = mitglieder.iterator().next()
                        .getFullData(con);
                out.print(mgl.toLongString());
            } else {
                // Liste gefundene Mitglieder auf
                out.println(mitglieder.size() + " Mitglieder gefunden:");
                for (NamiAbstractMitglied mgl : mitglieder) {
                    out.println(mgl);
                }
            }
        }
    }

}
