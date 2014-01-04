package nami.beitrag.db;

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
     * Fügt eine Sammellastschrift in die Datenbank ein. Die
     * <tt>sammelLastschriftId</tt> der neu eingefügten Lastschrift ist
     * anschließend im Objekt gespeichert, das als Parameter übergeben wurde.
     * 
     * @param sammelLastschrift
     *            Daten der einzufügenden Sammellastschrift
     */
    void insertSammelLastschrift(BeitragSammelLastschrift sammelLastschrift);

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
}
