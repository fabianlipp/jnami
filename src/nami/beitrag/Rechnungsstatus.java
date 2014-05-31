package nami.beitrag;

/**
 * Beschreibt die Stati, die eine Rechnung haben kann.
 * 
 * @author Fabian Lipp
 * 
 */
public enum Rechnungsstatus {
    /**
     * Die Rechnung wurde erstellt, aber noch nicht bezahlt.
     */
    OFFEN,
    /**
     * Die Rechnung wurde bezahlt.
     */
    BEGLICHEN,
    /**
     * Die Rechnung wurde noch nicht bezahlt und man kann nicht davon ausgehen,
     * dass sie noch bezahlt wird.
     */
    ABGESCHRIEBEN
}
