package nami.beitrag;

import java.util.Set;

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
}
