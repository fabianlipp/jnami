package nami.beitrag.db;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import lombok.Data;
import lombok.Getter;

import org.apache.ibatis.annotations.Param;

/**
 * Stellt Datenbankabfragen zur Arbeit mit SEPA-Lastschrift-Mandaten bereit.
 * 
 * @author Fabian Lipp
 * 
 */
public interface LastschriftenMapper {

    /**
     * Beschreibt Filterkriterien für die Suche nach Mandaten und Buchungen.
     */
    @Data
    public static class FilterSettings {
        /**
         * Dieser Parameter filtert die Rechnungen nach dem Erstellungsdatum.
         * Wenn dieser Parameter <tt>null</tt> ist, wird nicht nach dem
         * Rechnungsdatum gefiltert.
         */
        private Date rechnungsdatum = null;

        /**
         * Gibt an, ob auch Rechnungen selektiert werden, für die bereits eine
         * Lastschrift erstellt wurde.
         */
        private boolean bereitsErstellt;
    }

    /**
     * Ergebnis-Datentyp bei der Suche nach Mandaten und Rechnungen, die vom
     * jeweiligen Mandat eingezogen werden können.
     */
    @Getter
    public static class DataMandateRechnungen {
        private BeitragSepaMandat mandat;
        private List<DataRechnungMitglied> rechnungen;
    }

    /**
     * Fasst ein Mitglied und eine Rechnung zusammen.
     */
    @Getter
    public static class DataRechnungMitglied {
        private BeitragMitglied mitglied;
        private BeitragRechnung rechnung;
    }

    /**
     * Liefert alle offenen Rechnungen und die zugehörigen aktiven, gültigen
     * Mandate, die die übergebenen Filterkriterien erfüllen.
     * 
     * @param filterSettings
     *            Kriterien nach denen gefiltert wird
     * @return Mandate und jeweils offene Rechnungen
     */
    Collection<DataMandateRechnungen> mandateOffeneRechnungen(
            @Param("filterSettings") FilterSettings filterSettings);

    /**
     * Fügt eine Lastschrift in die Datenbank ein. Die <tt>lastschriftId</tt>
     * der neu eingefügten Lastschrift ist anschließend im Objekt gespeichert,
     * das als Parameter übergeben wurde.
     * 
     * @param lastschrift
     *            Daten der einzufügenden Lastschrift
     */
    void insertLastschrift(BeitragLastschrift lastschrift);

    /**
     * Löscht eine Lastschrift aus der Datenbank. Die enthaltenen Rechnungen
     * sollten vorher schon gelöscht worden seien (siehe
     * {@link #deleteAllRechnungenFromLastschrift(int)}).
     * 
     * @param lastschriftId
     *            ID der Lastschrift, die gelöscht werden soll
     */
    void deleteLastschrift(int lastschriftId);

    /**
     * Fügt eine Rechnung zu einer Lastschrift hinzu, d. h. diese Rechnung wird
     * mit der jeweiligen Lastschrift eingezogen.
     * 
     * @param lastschriftId
     *            ID der Lastschrift
     * @param rechnungId
     *            ID der Rechnung
     */
    void addRechnungToLastschrift(@Param("lastschriftId") int lastschriftId,
            @Param("rechnungId") int rechnungId);

    /**
     * Löscht alle Rechnungen aus einer Lastschrift. Die Rechnungen werden dabei
     * in der Datenbank belassen, nur ihre Verbindungen zur Lastschrift werden
     * entfernt.
     * 
     * @param lastschriftId
     *            ID der Lastschrift
     */
    void deleteAllRechnungenFromLastschrift(int lastschriftId);

    /**
     * Fügt eine Sammellastschrift in die Datenbank ein. Die
     * <tt>sammelLastschriftId</tt> der neu eingefügten Lastschrift ist
     * anschließend im Objekt gespeichert, das als Parameter übergeben wurde.
     * 
     * @param sammelLastschrift
     *            Daten der einzufügenden Sammellastschrift
     */
    void insertSammelLastschrift(BeitragSammelLastschrift sammelLastschrift);

    /**
     * Aktualisiert den Datensatz einer Sammellastschrift.
     * 
     * @param sammelLastschrift
     *            Objekt, dessen geänderte Felder in die Datenbank gespeichert
     *            werden sollen
     */
    void updateSammelLastschrift(BeitragSammelLastschrift sammelLastschrift);

    /**
     * Löscht eine Sammellastschrift aus der Datenbank. Die enthaltenen
     * Lastschriften sollten vorher schon gelöscht worden seien.
     * 
     * @param sammelLastschriftId
     *            ID der Sammellastschrift
     */
    void deleteSammelLastschrift(int sammelLastschriftId);

    /**
     * Fragt alle Sammellastschriften (inkl. der Anzahl der enthaltenen
     * Einzellastschriften und des Gesamtbetrages) aus der Datenbank ab, die dem
     * übergebenen Filterkriterium entsprechen.
     * 
     * @param ausgefuehrt
     *            Gibt an, ob ausgeführte Sammellastschriften angezeigt werden
     *            sollen.
     *            <ul>
     *            <li><tt>True</tt>: nur ausgeführte werden angezeigt</li>
     *            <li><tt>False</tt>: nur <em>nicht</em> ausgeführte werden
     *            angezeigt</li>
     *            <li><tt>null</tt>: Filterkriterium wird ignoriert</li>
     *            </ul>
     * @return Sammellastschriften, die dem angegebenen Filterkriterium
     *         entsprechen
     */
    ArrayList<BeitragSammelLastschrift> findSammelLastschriften(
            @Param("ausgefuehrt") Boolean ausgefuehrt);

    /**
     * Ergebnis-Datentyp, der Kombinationen aus einer Lastschrift und dem
     * zugehörigen Mandat aufnimmt.
     */
    @Getter
    public static class DataLastschriftMandat {
        private BeitragLastschrift lastschrift;
        private BeitragSepaMandat mandat;
    }

    /**
     * Liefert alle Lastschriften (inkl. der zugehörigen Mandate), die in einer
     * Sammellastschrift enthalten sind.
     * 
     * @param sammelLastschriftId
     *            ID der Sammellastschrift
     * @return enthaltenen (Einzel-)Lastschriften
     */
    ArrayList<DataLastschriftMandat> getLastschriften(int sammelLastschriftId);

    /**
     * Liefert alle Rechnungen, die in einer Sammellastschrift enthalten sind.
     * Das heißt es werden die zugeordneten Rechnungen aller enthaltenen
     * Einzellastschriften vereinigt.
     * 
     * @param sammelLastschriftId
     *            ID der Sammellastschrift
     * @return zugeordnete Rechnungen
     */
    List<BeitragRechnung> getRechnungenInSammelLastschrift(
            int sammelLastschriftId);

    /**
     * Fügt eine Prenotification in die Datenbank ein. Die
     * <tt>prenotificationId</tt> der neu eingefügten Prenotification ist
     * anschließend im Objekt gespeichert, das als Parameter übergeben wurde.
     * 
     * @param pre
     *            Daten der einzufügenden Prenotification
     */
    void insertPrenotification(BeitragPrenotification pre);

    /**
     * Überprüft, ob die aktuellste (nach Ausstellungsdatum), als regelmäßig
     * gekennzeichnete Prenotification im Betrag mit den Parametern
     * übereinstimmt. In diesem Fall muss also keine neue erstellt werden.
     * 
     * @param mandatId
     *            ID des Mandats
     * @param betrag
     *            benötigter Lastschriftbetrag
     * @return <tt>true</tt>, falls die aktuellste Prenotification den passenden
     *         Betrag enthält
     */
    boolean existsValidPrenotification(@Param("mandatId") int mandatId,
            @Param("betrag") BigDecimal betrag);
}
