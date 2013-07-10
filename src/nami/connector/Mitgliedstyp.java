package nami.connector;

public enum Mitgliedstyp {
    MITGLIED, NICHT_MITGLIED, SCHNUPPERMITGLIED;
    
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
            throw new IllegalArgumentException("Unexpected String for Mitgliedstyp");
        }
    }
}
