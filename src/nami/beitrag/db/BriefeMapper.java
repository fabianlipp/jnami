package nami.beitrag.db;

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
}
