package nami.beitrag.db;

import java.util.Collection;
import java.util.Set;

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
     * @param mitgliedsId
     *            ID des Mitglieds
     * @return kompletter Datensatz des Mitglieds
     */
    BeitragMitglied getMitglied(int mitgliedsId);

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
     * Liefert einen Buchungs-Datensatz aus der Datenbank.
     * 
     * @param namiBuchungId
     *            ID der Buchung in NaMi
     * @return Datensatz der Buchung; <tt>null</tt>, falls keine Buchung mit der
     *         ID existiert
     */
    BeitragBuchung getBuchungByNamiId(int namiBuchungId);

    /**
     * Fügt einen Buchungs-Datensatz in die Datenbank ein.
     * 
     * @param buchung
     *            die einzufügende Buchung
     */
    void insertBuchung(BeitragBuchung buchung);

    /**
     * Holt einen Zeitraum aus der Datenbank.
     * 
     * @param zeitraum
     *            Beschreibt den Zeitraum, der abgefragt werden soll. Dazu
     *            müssen Halbjahr und Jahr gesetzt sein
     * @return vollständiger Zeitraum-Datensatz
     */
    BeitragZeitraum getZeitraum(BeitragZeitraum zeitraum);

    /**
     * Fügt einen Zeitraum in die Datenbank ein.
     * 
     * @param zeitraum
     *            einzufügender Zeitraum
     */
    void insertZeitraum(BeitragZeitraum zeitraum);

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
}
