package nami.beitrag.gui.utils;

import org.apache.commons.lang3.StringUtils;

/**
 * Hilfs-Funktionen für Strings.
 * 
 * @author Fabian Lipp
 * 
 */
public final class MyStringUtils {

    private static final String[] REPLACE_UML_FROM = { "ä", "ö", "ü", "Ä", "Ö",
            "Ü", "ß", "ë" };
    private static final String[] REPLACE_UML_TO = { "ae", "oe", "ue", "Ae",
            "Oe", "Ue", "ss", "e" };

    private MyStringUtils() {
    }

    /**
     * Ersetzt alle Umlaute im übergebenen String durch ihre Umschreibung.
     * 
     * @param text
     *            der zu bearbeitende String
     * @return String mit ersetzen Umlauten
     */
    public static String replaceUmlauts(String text) {
        return StringUtils.replaceEach(text, REPLACE_UML_FROM, REPLACE_UML_TO);
    }
}
