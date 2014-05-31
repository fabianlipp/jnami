package nami.beitrag.db;

import java.util.ArrayList;
import java.util.Date;

import lombok.Data;
import nami.beitrag.letters.LetterType;

import org.apache.ibatis.annotations.Param;

/**
 * Stellt Datenbankabfragen zur Arbeit mit der Brief-Tabelle bereit.
 * 
 * @author Fabian Lipp
 * 
 */
public interface BriefeMapper {
    /**
     * Fügt einen Brief in die Datenbank ein. Die <tt>briefId</tt> des neu
     * eingefügten Briefs ist anschließend im Objekt gespeichert, das als
     * Parameter übergeben wurde.
     * 
     * @param brief
     *            einzufügender Brief
     */
    void insertBrief(BeitragBrief brief);

    /**
     * Holt den vollen Datensatz eines Briefs aus der Datenbank.
     * 
     * @param briefId
     *            ID des Briefs
     * @return alle Daten des Briefs
     */
    BeitragBrief getBrief(int briefId);

    /**
     * Aktualisiert einen BrieF in der Datenbank.
     * 
     * @param brief
     *            Daten des Briefs
     */
    void updateBrief(BeitragBrief brief);

    /**
     * Beschreibt Filterkriterien für die Suche nach Briefen.
     */
    @Data
    static class FilterSettings {
        /**
         * Frühestes Datum, dessen Briefe gewählt werden. Falls der Wert
         * <tt>null</tt> ist, wird nicht nach diesem Kriterium gefiltert.
         */
        private Date datumVon = null;

        /**
         * Spätestes Datum, dessen Briefe gewählt werden. Falls der Wert
         * <tt>null</tt> ist, wird nicht nach diesem Kriterium gefiltert.
         */
        private Date datumBis = null;

        /**
         * Filtert die Briefe nach einem bestimmten Typ. Falls der Wert
         * <tt>null</tt> ist, werden alle Brieftypen angezeigt.
         */
        private LetterType typ = null;
    }

    /**
     * Liefert alle Briefe, die den übergebenen Filterkriterien entsprechen.
     * 
     * @param filterSettings
     *            Kriterien, nach denen die Briefe gefiltert werden
     * @return Briefe, die den vorgegebenen Bedingungen entsprechen
     */
    ArrayList<BeitragBrief> findBriefe(
            @Param("filterSettings") FilterSettings filterSettings);
}
