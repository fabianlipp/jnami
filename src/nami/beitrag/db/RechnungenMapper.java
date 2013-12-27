package nami.beitrag.db;

import java.util.Collection;

import lombok.Data;

import org.apache.ibatis.annotations.Param;

import nami.beitrag.Zahlungsart;
import nami.connector.Halbjahr;

/**
 * Stellt Datenbankabfragen zur Arbeit mit Rechnungen bereit.
 * 
 * @author Fabian Lipp
 * 
 */
public interface RechnungenMapper {
    /**
     * Möglichkeiten Buchungen nach dem Vorausberechnungs-Feld zu filtern.
     */
    static enum VorausberechnungFilter {
        /**
         * Keine Vorausberechnungen ausgeben.
         */
        KEINE,

        /**
         * Alle Buchungen ausgeben.
         */
        AUCH,

        /**
         * Nur Vorausberechnungen ausgeben.
         */
        NUR
    }

    /**
     * Beschreibt Filterkriterien für die Suche nach Personen und Buchungen.
     */
    @Data
    static class FilterSettings {
        /**
         * Frühestes Halbjahr, dessen Buchungen gewählt werden. Falls der Wert
         * <tt>null</tt> ist, wird nicht nach diesem Kriterium gefiltert.
         */
        private Halbjahr halbjahrVon = null;

        /**
         * Spätestes Halbjahr, dessen Buchungen gewählt werden. Falls der Wert
         * <tt>null</tt> ist, wird nicht nach diesem Kriterium gefiltert.
         */
        private Halbjahr halbjahrBis = null;

        /**
         * Gibt an, ob Buchungen selektiert werden sollen, die
         * Vorausberechnungen sind.
         */
        private VorausberechnungFilter vorausberechnung;

        /**
         * Hiermit können nur Mitglieder selektiert werden, für die eine
         * bestimmte Zahlungsart eingetragen ist. Falls der Wert <tt>null</tt>
         * ist, wird nicht nach diesem Kriterium gefiltert.
         */
        private Zahlungsart zahlungsart = null;

        /**
         * Gibt an, ob auch Buchungen selektiert werden, die bereits in einer
         * Rechnung enthalten sind.
         */
        private boolean bereitsBerechnet;
    }

    /**
     * Liefert alle Mitglieder, bei denen das Beitragskonto nicht ausgeglichen
     * sind. Dabei werden nur Buchungen berücksichtigt, die den übergebenen
     * Filterkriterien entsprechen.
     * 
     * @param filterSettings
     *            Kriterien, nach denen die Buchungen gefiltert werden
     * @return Mitglieder, deren Beitragskonto (bezogen auf die Buchungen, die
     *         <tt>filterSettings</tt> erfüllen) nicht ausgeglichen ist
     */
    Collection<DataMitgliederForderungen> mitgliederOffeneForderungen(
            @Param("filterSettings") FilterSettings filterSettings);

    /**
     * Liefert für ein bestimmtes Mitglied alle Buchungen, die vorgegebene
     * Kriterien erfüllen.
     * 
     * @param mitgliedId
     *            ID des Mitglieds
     * @param filterSettings
     *            Kriterien, nach denen die Buchungen gefiltert werden
     * @return Buchungen, die die Kriterien erfüllen
     */
    Collection<BeitragBuchung> getBuchungenFiltered(
            @Param("mitgliedId") int mitgliedId,
            @Param("filterSettings") FilterSettings filterSettings);

    /**
     * Liefert die höchste Rechnungsnummer, die im übergebenen Jahr bisher
     * verwendet wurde.
     * 
     * @param jahr
     *            angefragtes Jahr
     * @return höchste bisher verwendete Rechnungsnummer
     */
    int maxRechnungsnummer(int jahr);

    /**
     * Fügt eine Rechnung in die Datenbank ein. Die <tt>rechnungId</tt> der neu
     * eingefügten Rechnung ist anschließend im Objekt gespeichert, das als
     * Parameter übergeben wurde.
     * 
     * @param rechnung
     *            einzufügende Rechnung
     */
    void insertRechnung(BeitragRechnung rechnung);

    /**
     * Holt den vollen Datensatz einer Rechnung aus der Datenbank.
     * 
     * @param rechnungId
     *            ID der Rechnung
     * @return alle Daten der Rechnung
     */
    BeitragRechnung getRechnung(int rechnungId);

    /**
     * Fügt einen Posten zu einer Rechnung hinzu.
     * 
     * @param rechnungId
     *            Rechnung, zu der der Posten gehört
     * @param buchungId
     *            Buchung, die damit beglichen werden soll
     * @param buchungstext
     *            Text, der auf der Rechnung angezeigt wird
     */
    void insertPosten(@Param("rechnungId") int rechnungId,
            @Param("buchungId") int buchungId,
            @Param("buchungstext") String buchungstext);
}
