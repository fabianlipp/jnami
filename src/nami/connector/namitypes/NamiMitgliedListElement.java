package nami.connector.namitypes;

import nami.connector.Geschlecht;
import nami.connector.Mitgliedstyp;

public class NamiMitgliedListElement implements Comparable<NamiMitgliedListElement> {
    public static class EntriesType {
        private String id;
        
        private String vorname;
        private String nachname;
        
        private String email;
        private String telefon1;
        private String telefon2;
        private String telefon3;
        private String telefax;
 
        // nur in Suche, nicht in Mitgliederverwaltung
        private String gruppierungId;
        private String gruppierung;
        
        private String stufe;
        private String geburtsDatum;
        
        private String mglType;
        private String status;
        
        private String staatsangehoerigkeit;
        private String staatangehoerigkeitText;
        private String geschlecht;
        private String konfession;
        private String emailVertretungsberechtigter;
        private String rowCssClass;
        private String lastUpdated;
        private String version;
        private String wiederverwendenFlag;
        private String mitgliedsNummer;
    }

    private String descriptor;
    private EntriesType entries;
    private int id;

    public int getId() {
        return id;
    }

    public int getGruppierungId() {
        return Integer.parseInt(entries.gruppierungId);
    }

    public String getVorname() {
        return entries.vorname;
    }

    public String getEmail() {
        return entries.email;
    }

    public String getNachname() {
        return entries.nachname;
    }
    
    public Mitgliedstyp getMitgliedstyp() {
        return Mitgliedstyp.fromString(entries.mglType);
    }
    
    public Geschlecht getGeschlecht() {
        return Geschlecht.fromString(entries.geschlecht);
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder();
        res.append(entries.vorname).append(" ").append(entries.nachname);
        res.append(" (").append(entries.mitgliedsNummer).append(")");
        res.append(System.lineSeparator());

        res.append(entries.email).append(System.lineSeparator());
        return res.toString();
    }

    @Override
    public int compareTo(NamiMitgliedListElement o) {
        return Integer.valueOf(this.id).compareTo(Integer.valueOf(o.id));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof NamiMitgliedListElement)) {
            return false;
        }
        NamiMitgliedListElement other = (NamiMitgliedListElement) obj;
        if (id != other.id) {
            return false;
        }
        return true;
    }
}
