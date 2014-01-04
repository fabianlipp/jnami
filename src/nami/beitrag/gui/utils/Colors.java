package nami.beitrag.gui.utils;

import java.awt.Color;

import javax.swing.UIDefaults;

/**
 * Definiert Farben, die an verschiedenen Stellen im Programm verwendet werden.
 * 
 * @author Fabian Lipp
 * 
 */
public final class Colors {

    // Farben für die Hervorhebung von Zeilen in einer TreeTable
    private static final UIDefaults UIDEFAULTS = javax.swing.UIManager
            .getDefaults();
    /**
     * Farbe, die für den Hintergrund der Kindeinträge in einer TreeTable der
     * Tiefe 2 verwendet wird.
     */
    public static final Color TT_CHILD_BG = UIDEFAULTS
            .getColor("Label.background");

    /**
     * Farbe die zum Kennzeichnen von selektierten Einträgen in einer Tabelle
     * verwendet wird.
     */
    public static final Color TT_SEL_BG = UIDEFAULTS
            .getColor("List.selectionBackground");

    /**
     * Schriftfarbe zum Kennzeichnen von deaktivierten Einträgen in Tabellen.
     */
    public static final Color TT_DEACTIV_FG = UIDEFAULTS
            .getColor("Label.disabledForeground");

    private Colors() {
    }
}
