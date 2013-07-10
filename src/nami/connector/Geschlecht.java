package nami.connector;

public enum Geschlecht {
    MAENNLICH, WEIBLICH;
    
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
            throw new IllegalArgumentException("Unexpected String for Geschlecht");
        }
    }
}
