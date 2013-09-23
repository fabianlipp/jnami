package nami.statistics;

import nami.connector.namitypes.NamiAbstractMitglied;

/**
 * Beschreibt eine Klasse, die Mitglieder nach einem bestimmten Kriterium
 * filtert.
 * 
 * @author Fabian Lipp
 * 
 */
public interface Filter {
    /**
     * Entscheidet, ob das Mitglied vom Filter akzeptiert wird.
     * 
     * @param mgl
     *            das Mitglied, das geprüft werden soll
     * @return <tt>true</tt>, falls das Mitglied vom Filter akzeptiert wird
     */
    boolean filter(NamiAbstractMitglied mgl);
}
