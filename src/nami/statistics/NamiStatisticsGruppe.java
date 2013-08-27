package nami.statistics;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import nami.connector.NamiConnector;
import nami.connector.exception.NamiApiException;
import nami.connector.namitypes.NamiMitgliedListElement;
import nami.connector.namitypes.NamiSearchedValues;

/**
 * Beschreibt eine Gruppe, die in der Statistik erfasst wird. Mit Gruppe ist
 * eine Menge von Mitgliedern gemeint, die bestimmte Bedingungen erfüllen.
 * 
 * @author Fabian Lipp
 * 
 */
public class NamiStatisticsGruppe {
    private int id;
    private String bezeichnung;
    private Collection<NamiSearchedValues> searches;

    /**
     * Legt eine neue Gruppe an.
     * 
     * @param id
     *            ID, mit der die Gruppe in der Datenbank erfasst wird
     * @param bezeichnung
     *            Bezeichnung der Gruppe
     * @param searches
     *            Suchanfragen, die an Nami gestellt werden, um die Mitglieder
     *            dieser Gruppe zu finden (Es wird die Vereinigung der
     *            Ergebnisse dieser Suchanfragen gebildet)
     */
    public NamiStatisticsGruppe(int id, String bezeichnung,
            Collection<NamiSearchedValues> searches) {
        this.id = id;
        this.bezeichnung = bezeichnung;
        this.searches = searches;
    }

    /**
     * Liefert die ID, mit der die Gruppe in der Datenbank erfasst wird.
     * 
     * @return ID
     */
    public int getId() {
        return id;
    }

    /**
     * Liefert die Bezeichnung der Gruppe.
     * 
     * @return Bezeichnung
     */
    public String getBezeichnung() {
        return bezeichnung;
    }

    /**
     * Liefert die Anzahl der Personen in dieser Gruppe in einer bestimmten
     * Gruppierung.
     * 
     * @param con
     *            Verbindung zum NaMi-Server
     * @param gruppierungsnummer
     *            Gruppierung, in der gesucht werden soll (Kindgruppierungen
     *            werden nicht mit durchsucht)
     * @return Anzahl gefundener Personen, die den Bedingungen dieser Gruppe
     *         entsprechen
     * @throws IOException
     *             IOException
     * @throws NamiApiException
     *             API-Fehler beim Zugriff auf NaMi
     */
    public int getAnzahl(NamiConnector con, String gruppierungsnummer)
            throws NamiApiException, IOException {

        if (searches.size() == 1) {
            // es gibt nur eine Suchanfrage -> die Anzahl reicht aus
            NamiSearchedValues search = searches.iterator().next();
            search.setGruppierungsnummer(gruppierungsnummer);
            return search.getCount(con);
        } else if (searches.size() > 1) {
            // Das HashSet vergleicht die Mitglieder anhand der Mitgliedsnummer,
            // d.h. jedes Mitglied wird auch bei mehreren Tätigkeiten nur einmal
            // gezählt
            Set<NamiMitgliedListElement> searchResults = new HashSet<>();

            // Führe Suchen aus
            for (NamiSearchedValues search : searches) {
                search.setGruppierungsnummer(gruppierungsnummer);
                searchResults.addAll(search.getAllResults(con));
            }

            return searchResults.size();
        } else {
            return 0;
        }
    }
}
