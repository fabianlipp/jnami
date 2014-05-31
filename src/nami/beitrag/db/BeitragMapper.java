package nami.beitrag.db;

import java.util.Collection;
import java.util.Set;

import nami.connector.Halbjahr;

import org.apache.ibatis.annotations.Param;

/**
 * Beschreibt einen Mapper für die Datenbankabfragen, die auf die Tabelle
 * <tt>beitragMitglied</tt> zugreifen.
 * 
 * @author Fabian Lipp
 * 
 */
public interface BeitragMapper {
    /**
     * Liefert den Datensatz eines Mitglieds.
     * 
     * @param mitgliedId
     *            ID des Mitglieds
     * @return kompletter Datensatz des Mitglieds
     */
    BeitragMitglied getMitglied(int mitgliedId);

    /**
     * Liefert den Datensatz eines Mitglieds.
     * 
     * @param mitgliedsnummer
     *            Mitgliedsnummer
     * @return kompletter Datensatz des Mitglieds
     */
    BeitragMitglied getMitgliedByNummer(int mitgliedsnummer);

    /**
     * Liefert die Mitglieds-IDs, die in der Datenbank vorhanden sind.
     * 
     * @return vorhandene Mitglieds-IDs
     */
    Set<Integer> getMitgliedIds();

    /**
     * Fügt ein Mitglied in die Datenbank ein. Die Mitglieds-ID muss gesetzt
     * sein.
     * 
     * @param mgl
     *            Mitglieds-Datensatz, der in die Datenbank geschrieben werden
     *            soll
     */
    void insertMitglied(BeitragMitglied mgl);

    /**
     * Aktualisiert ein Mitglied in der Datenbank. Der Datensatz wird anhand der
     * Mitglieds-ID identifiziert.
     * 
     * @param mgl
     *            Mitglieds-Datensatz, der in die Datenbank geschrieben werden
     *            soll
     */
    void updateMitglied(BeitragMitglied mgl);

    /**
     * Fragt ab, ob ein Mitglied in der Datenbank als 'deleted' markiert ist.
     * 
     * @param mitgliedsId
     *            ID des Mitglieds
     * @return <tt>true</tt>, falls das Mitglied als gelöscht markiert ist
     */
    boolean isDeleted(int mitgliedsId);

    /**
     * Markiert ein Mitglied in der Datenbank als 'deleted'.
     * 
     * @param mitgliedsId
     *            ID des Mitglieds
     */
    void setDeleted(int mitgliedsId);

    /**
     * Findet alle Mitglieder in der lokalen Datenbank, die den Suchkriterien
     * entsprechen.
     * 
     * @param mitgliedsnummer
     *            gesuchte Mitgliedsnummer; <tt>null</tt> bzw. der leere String
     *            werden ignoriert; ansonsten wird exakt verglichen
     *            (SQL-Operator =)
     * @param vorname
     *            gesuchter Vorname; <tt>null</tt> bzw. der leere String werden
     *            ignoriert; ansonsten wird mittels LIKE verglichen
     * @param nachname
     *            gesuchter Nachname; <tt>null</tt> bzw. der leere String werden
     *            ignoriert; ansonsten wird mittels LIKE verglichen
     * @return Mitglieder, die den Kriterien entsprechen
     */
    Collection<BeitragMitglied> findMitglieder(
            @Param("mitgliedsnummer") String mitgliedsnummer,
            @Param("vorname") String vorname, @Param("nachname") String nachname);

    /**
     * Liefert einen Buchungs-Datensatz aus der Datenbank.
     * 
     * @param buchungId
     *            ID der Buchung in der lokalen Datenbank
     * @return Datensatz der Buchung; <tt>null</tt>, falls keine Buchung mit der
     *         ID existiert
     */
    BeitragBuchung getBuchungById(int buchungId);

    /**
     * Liefert einen Buchungs-Datensatz aus der Datenbank.
     * 
     * @param namiBuchungId
     *            ID der Buchung in NaMi
     * @return Datensatz der Buchung; <tt>null</tt>, falls keine Buchung mit der
     *         ID existiert
     */
    BeitragBuchung getBuchungByNamiId(int namiBuchungId);

    /**
     * Gibt die Anzahl von Buchungen für das angegebene Mitglied und Halbjahr
     * an, die als RECHNUNG_BUNDESEBENE markiert sind.
     * 
     * @param halbjahr
     *            Halbjahr
     * @param mitgliedId
     *            ID des Mitglieds
     * @return Anzahl der gefundenen Buchungen
     */
    int checkForRechnungBundesebene(@Param("halbjahr") Halbjahr halbjahr,
            @Param("mitgliedId") int mitgliedId);

    /**
     * Löscht alle Buchungen für das angegebene Mitglied und Halbjahr, die als
     * Vorausberechnung markiert sind.
     * 
     * @param halbjahr
     *            Halbjahr
     * @param mitgliedId
     *            ID des Mitglieds
     */
    void deleteVorausberechnung(@Param("halbjahr") Halbjahr halbjahr,
            @Param("mitgliedId") int mitgliedId);

    /**
     * Fügt einen Buchungs-Datensatz in die Datenbank ein.
     * 
     * @param buchung
     *            die einzufügende Buchung
     */
    void insertBuchung(BeitragBuchung buchung);

    /**
     * Liefert für jedes Halbjahr die Summe aller Buchungen für ein bestimmtes
     * Mitglied.
     * 
     * @param mitgliedId
     *            Mitglied, für das die Buchungen abgefragt werden
     * @return für jedes Halbjahr die Summe der Buchungsbeträge und Anzahl der
     *         Buchungen
     */
    Collection<ZeitraumSaldo> getSaldoPerHalbjahr(int mitgliedId);

    /**
     * Liefert alle Buchungen, die den Kriterien entsprechen.
     * 
     * @param halbjahr
     *            Halbjahr, in dem die Buchung liegt
     * @param mitgliedId
     *            Mitglied, dem die Buchung zugeordnet ist
     * @return Buchungen des Mitglieds im Halbjahr
     */
    Collection<BeitragBuchung> getBuchungenByHalbjahr(
            @Param("halbjahr") Halbjahr halbjahr,
            @Param("mitgliedId") int mitgliedId);

    /**
     * Holt einen Zeitraum aus der Datenbank.
     * 
     * @param halbjahr
     *            Das Halbjahr, zu dem der entsprechende Zeitraum aus der
     *            Datenbank geholt werden soll
     * @return vollständiger Zeitraum-Datensatz
     */
    BeitragZeitraum getZeitraum(Halbjahr halbjahr);

    /**
     * Fügt einen Zeitraum in die Datenbank ein.
     * 
     * @param zeitraum
     *            einzufügender Zeitraum
     */
    void insertZeitraum(BeitragZeitraum zeitraum);

}
