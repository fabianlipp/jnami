package nami.connector;

/**
 * Beschreibt das Geschlecht eines Mitglieds.
 * 
 * @author Fabian Lipp
 * 
 */
public enum Geschlecht {
    /**
     * Männlich.
     */
    MAENNLICH,

    /**
     * Weiblich.
     */
    WEIBLICH,

    /**
     * Keine Angabe.
     */
    KEINE_ANGABE;

    /**
     * Setzt einen String ins entsprechende Geschlecht um.
     * 
     * @param str
     *            String-Repräsentation des Geschlechts
     * @return entsprechende Ebene; <code>null</code>, wenn der String nicht
     *         umgesetzt werden kann
     */
    public static Geschlecht fromString(String str) {
        switch (str) {
        case "männlich":
        case "MAENNLICH":
            return MAENNLICH;
        case "weiblich":
        case "WEIBLICH":
            return WEIBLICH;
        case "keine Angabe":
        case "Keine Angabe":
        case "KEINE_ANGABE":
            return KEINE_ANGABE;
        case "":
            return null;
        default:
            throw new IllegalArgumentException(
                    "Unexpected String for Geschlecht: " + str);
        }
    }
}
