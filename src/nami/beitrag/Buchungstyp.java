package nami.beitrag;

/**
 * Beschreibt die Typen von Buchungen, die das System verarbeitet.
 * @author Fabian Lipp
 *
 */
public enum Buchungstyp {
    /**
     * Beitrag, der von der Bundesebene für ein Mitglied berechnet wird.
     */
    RECHNUNG_BUNDESEBENE,

    /**
     * Gutschrift die von Bundesebene für ein Mitglied erstellt wird.
     */
    GUTSCHRIFT_BUNDESEBENE,

    /**
     * Überweisung (eingehend oder ausgehend), die von einem oder an ein
     * Mitglied erfolgt.
     */
    UEBERWEISUNG,

    /**
     * Lastschrift, die von einem Mitglied gezogen wird.
     */
    LASTSCHRIFT,

    /**
     * Rücklastschrift, die durch ein Mitglied (bzw. dessen Bank) veranlasst
     * wird.
     */
    RUECKLASTSCHRIFT,

    /**
     * Barzahlung, die von einem Mitglied entgegengenommen wird.
     */
    BAR,

    /**
     * Gutschrift, die ohne Gegenleistung des Mitglieds erfolgt, also Erlass
     * eines Mitgliedsbeitrages.
     */
    ERLASS,

    /**
     * Belastung eines Mitglieds-Kontos (z. B. mit Auslagen für Mahnungen,
     * Gebühren für Rücklastschrift).
     */
    BELASTUNG;
}
