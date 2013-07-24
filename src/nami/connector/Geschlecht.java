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
    WEIBLICH;

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
            return MAENNLICH;
        case "weiblich":
            return WEIBLICH;
        case "":
            // TODO: nur zu DEBUG-Zwecken für die Testversion
            return MAENNLICH;
        default:
            throw new IllegalArgumentException(
                    "Unexpected String for Geschlecht");
        }
    }
}
