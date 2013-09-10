package nami.beitrag;

/**
 * Beschreibt die Zahlungsart eines Mitglieds.
 * 
 * @author Fabian Lipp
 * 
 */
public enum Zahlungsart {
    /**
     * Der Beitrag wird vom Mitglied mittels Lastschrift eingezogen.
     */
    LASTSCHRIFT,

    /**
     * Der Beitrag wird dem Mitglied in Rechnung gestellt und muss manuell
     * eingezahlt werden.
     */
    RECHNUNG;
}
