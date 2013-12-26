package nami.beitrag.db;

import java.util.Collection;

import org.apache.ibatis.annotations.Param;

import nami.connector.Halbjahr;

/**
 * Stellt Datenbankabfragen zur Arbeit mit Rechnungen bereit.
 * 
 * @author Fabian Lipp
 * 
 */
public interface RechnungenMapper {
    /**
     * Liefert alle Mitglieder, für die in einem bestimmten Halbjahr offene
     * Forderungen bestehen.
     * 
     * @param halbjahr
     *            Halbjahr, für das Buchungen abgefragt werden
     * @return Mitglieder, die die Bedingung erfüllen
     */
    Collection<DataMitgliederForderungen> mitgliederOffeneForderungen(
            Halbjahr halbjahr);

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
