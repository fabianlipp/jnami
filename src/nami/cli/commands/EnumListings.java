package nami.cli.commands;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import nami.cli.annotation.AlternateCommands;
import nami.cli.annotation.CliCommand;
import nami.cli.annotation.CommandDoc;
import nami.connector.NamiConnector;
import nami.connector.exception.NamiApiException;
import nami.connector.namitypes.NamiEnum;

/**
 * Listet verschiedene Enums aus Nami auf (z. B. Tätigkeiten,
 * Untergliederungen).
 * 
 * @author Fabian Lipp
 * 
 */
public final class EnumListings {
    private EnumListings() {
    }

    /**
     * Listet die verfügbaren Tätigkeiten auf.
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
    @CliCommand("listTaetigkeiten")
    @AlternateCommands("lt")
    @CommandDoc("Listet die verfügbaren Tätigkeiten auf")
    public static void listTaetigkeiten(String[] args, NamiConnector con,
            PrintWriter out) throws NamiApiException, IOException {
        List<NamiEnum> allTaetigkeiten = NamiEnum.getTaetigkeiten(con);
        for (NamiEnum e : allTaetigkeiten) {
            System.out.println(e);
        }
    }

    /**
     * Listet die verfügbaren Stufen/Abteilungen auf.
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
    @CliCommand("listUntergliederungen")
    @AlternateCommands("lu")
    @CommandDoc("Listet die verfügbaren Stufen/Abteilungen auf")
    public static void listUntergliederungen(String[] args, NamiConnector con,
            PrintWriter out) throws NamiApiException, IOException {
        List<NamiEnum> allUntergliederungen = NamiEnum
                .getUntergliederungen(con);
        for (NamiEnum e : allUntergliederungen) {
            System.out.println(e);
        }
    }
}
