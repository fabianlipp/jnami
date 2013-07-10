package nami.connector;
public enum Ebene {
    BUND(0),
    DIOEZESE(2),
    BEZIRK(4),
    STAMM(6);

    private int significantChars;

    private Ebene(int significantChars) {
        this.significantChars = significantChars;
    }

    public int getSignificantChars() {
        return significantChars;
    }

    public static Ebene getFromGruppierungId(int gruppierungId) {
        return getFromGruppierungId(Integer.toString(gruppierungId));
    }

    public static Ebene getFromGruppierungId(String gruppierungId) {
        if (gruppierungId.equals("000000")) {
            return BUND;
        } else if (gruppierungId.substring(2).equals("0000")) {
            return DIOEZESE;
        } else if (gruppierungId.substring(4).equals("00")) {
            return BEZIRK;
        } else {
            return STAMM;
        }
    }

    public static Ebene getFromString(String ebene) {
        if (ebene == null) {
            return null;
        }
        
        if (ebene.equals("stamm")) {
            return STAMM;
        } else if (ebene.equals("bezirk")) {
            return BEZIRK;
        } else if (ebene.equals("dioezese")) {
            return DIOEZESE;
        } else if (ebene.equals("bund")) {
            return BUND;
        } else {
            return null;
        }
    }
}
