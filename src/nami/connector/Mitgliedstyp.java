package nami.connector;

/**
 * Mitgliedstypen, die es in NaMi gibt.
 * 
 * @author Fabian Lipp
 * 
 */
public enum Mitgliedstyp {
    /**
     * Normale Mitgliedschaft.
     */
    MITGLIED,

    /**
     * Keine Mitgliedschaft.
     */
    NICHT_MITGLIED,

    /**
     * Schnuppermitgliedschaft.
     */
    SCHNUPPERMITGLIED;

    /**
     * Setzt einen String in den entsprechenden Mitgliedstyp um.
     * 
     * @param str
     *            String-Repräsentation des Mitgliedstyps
     * @return entsprechender Mitgliedstyp; <code>null</code>, wenn der String
     *         nicht umgesetzt werden kann
     */
    public static Mitgliedstyp fromString(String str) {
        switch (str) {
        case "Mitglied":
            return MITGLIED;
        case "Nicht-Mitglied":
            return NICHT_MITGLIED;
        case "Nicht Mitglied":
            return NICHT_MITGLIED;
        case "Schnuppermitglied":
            return SCHNUPPERMITGLIED;
        case "":
            return null;
        default:
            throw new IllegalArgumentException(
                    "Unexpected String for Mitgliedstyp");
        }
    }
}
