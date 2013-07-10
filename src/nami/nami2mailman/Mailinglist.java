package nami.nami2mailman;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

import nami.connector.Ebene;
import nami.connector.namitypes.NamiMitgliedListElement;
import nami.connector.namitypes.NamiTaetigkeitAssignment;

import org.jdom2.Element;

public class Mailinglist {
    private static class MailinglistMember {
        private String name;
        private String email;

        private MailinglistMember(String name, String email) {
            this.name = name;
            this.email = email;
        }
        
        @Override
        public String toString() {
            return (name + " <" + email + ">"); 
        }
    }

    private static class GruppierungFilter {
        // Gruppierung, in der gesucht wird
        private int gruppierungId;
        // Ebene dieser Gruppierung
        private Ebene idEbene;
        // Ebene auf der die Tätigkeit stattfindet
        private Ebene filterEbene;

        /**
         * Erstellt einen Gruppierungsfilter.
         * 
         * @param gruppierungId
         *            ID, in der gesucht wird.
         * @param filterEbene
         *            Ebene, auf der die gesuchte Tätigkeit stattfinden muss.
         *            Wenn der Wert {@code null} ist, wird die Ebene ignoriert.
         */
        private GruppierungFilter(int gruppierungId, Ebene filterEbene) {
            this.gruppierungId = gruppierungId;
            idEbene = Ebene.getFromGruppierungId(gruppierungId);
            this.filterEbene = filterEbene;
        }

        private boolean isFilterSatisfied(int askedGruppierungId) {
            int significant = idEbene.getSignificantChars();
            String filterStr = Integer.toString(gruppierungId).substring(0,
                    significant);
            String askedStr = Integer.toString(askedGruppierungId).substring(0,
                    significant);
            if (!filterStr.equals(askedStr)) {
                return false;
            } else {
                if (filterEbene == null) {
                    return true;
                } else {
                    return (Ebene.getFromGruppierungId(askedGruppierungId) == filterEbene);
                }
            }
        }
    }

    private static class MailinglistFilter {
        private Collection<Integer> taetigkeitIds = new LinkedList<>();
        private Collection<Integer> untergliederungIds = new LinkedList<>();
        private Collection<GruppierungFilter> gruppierungen = new LinkedList<>();

        private MailinglistFilter(Element includeNamiResult)
                throws ConfigFormatException {
            // Lese Tätigkeiten
            for (Element taetigkeit : includeNamiResult
                    .getChildren("taetigkeit")) {
                String id = taetigkeit.getAttributeValue("id");
                try {
                    taetigkeitIds.add(Integer.valueOf(id));
                } catch (NumberFormatException e) {
                    throw new ConfigFormatException("Invalid ID for Taetigkeit");
                }
            }

            // Lese Untergliederungen
            for (Element untergliederung : includeNamiResult
                    .getChildren("untergliederung")) {
                String id = untergliederung.getAttributeValue("id");
                try {
                    untergliederungIds.add(Integer.valueOf(id));
                } catch (NumberFormatException e) {
                    throw new ConfigFormatException(
                            "Invalid ID for Untergliederung");
                }
            }

            // Lese Gruppierungen
            for (Element gruppierung : includeNamiResult
                    .getChildren("gruppierung")) {
                // Id
                String idStr = gruppierung.getAttributeValue("id");
                Integer id;
                try {
                    id = Integer.valueOf(idStr);
                } catch (NumberFormatException e) {
                    throw new ConfigFormatException(
                            "Invalid ID for Gruppierung");
                }

                // Ebene
                String ebeneStr = gruppierung.getAttributeValue("ebene");
                Ebene ebene;
                if (ebeneStr == null) {
                    // keine Ebene angegeben
                    ebene = null;
                } else {
                    ebene = Ebene.getFromString(ebeneStr);
                    if (ebene == null) {
                        // Ebene angegeben, aber nicht parsebar
                        throw new ConfigFormatException(
                                "Invalid value for ebene in Gruppierung");
                    }
                }

                gruppierungen.add(new GruppierungFilter(id, ebene));
            }
        }

        private boolean isFilterSatisfied(NamiTaetigkeitAssignment t) {
            boolean taetigkeitOk = false;
            boolean untergliederungOk = false;
            boolean gruppierungOk = false;

            // Überprüfe Tätigkeit
            if (taetigkeitIds.isEmpty()
                    || taetigkeitIds.contains(t.getTaetigkeitId())) {
                taetigkeitOk = true;
            }

            // Überprüfe Untergliederung
            if (untergliederungIds.isEmpty()
                    || untergliederungIds.contains(t.getUntergliederungId())) {
                untergliederungOk = true;
            }

            // Überprüfe Gruppierung
            if (gruppierungen.isEmpty()) {
                gruppierungOk = true;
            } else {
                for (GruppierungFilter g : gruppierungen) {
                    if (g.isFilterSatisfied(t.getGruppierungId())) {
                        gruppierungOk = true;
                    }
                }
            }

            if (taetigkeitOk && untergliederungOk && gruppierungOk) {
                return true;
            } else {
                return false;
            }
        }
    }

    private String name;
    private Collection<MailinglistMember> members = new LinkedList<>();
    private Collection<MailinglistFilter> filters = new LinkedList<>();

    public Mailinglist(Element mailinglist) throws ConfigFormatException {
        if (!mailinglist.getName().equals("mailinglist")) {
            throw new IllegalArgumentException(
                    "Wrong tag name for root element");
        }

        name = mailinglist.getAttributeValue("name");
        if (name == null || name.isEmpty()) {
            throw new ConfigFormatException(
                    "No name attribute for mailinglist.");
        }

        for (Element includePerson : mailinglist.getChildren("includePerson")) {
            String pName = includePerson.getAttributeValue("name");
            String email = includePerson.getAttributeValue("email");
            if (pName != null && !pName.isEmpty() && email != null
                    && !email.isEmpty()) {
                members.add(new MailinglistMember(pName, email));
            } else {
                throw new ConfigFormatException(
                        "Invalid includePerson declaration");
            }
        }

        for (Element includeNamiResult : mailinglist
                .getChildren("includeNamiResult")) {
            filters.add(new MailinglistFilter(includeNamiResult));
        }
    }

    private boolean checkMembership(
            Collection<NamiTaetigkeitAssignment> taetigkeiten) {
        // Teste, ob eine der Tätigkeiten einen der Filter erfüllt
        for (NamiTaetigkeitAssignment t : taetigkeiten) {
            // Teste, ob einer der Filter erfüllt ist
            for (MailinglistFilter f : filters) {
                if (t.istAktiv() && f.isFilterSatisfied(t)) {
                    return true;
                }
            }
        }

        return false;
    }

    public void checkAndAddMember(NamiMitgliedListElement mitglied,
            Collection<NamiTaetigkeitAssignment> taetigkeiten) {
        if (checkMembership(taetigkeiten)) {
            String pName = mitglied.getVorname() + " " + mitglied.getNachname();
            String email = mitglied.getEmail();
            members.add(new MailinglistMember(pName, email));
        }
    }

    public void writeToFile(String path) throws IOException {
        File f = new File(path + name);
        FileWriter fw = new FileWriter(f);
        BufferedWriter buf = new BufferedWriter(fw);
        for (MailinglistMember member : members) {
            buf.write(member.toString());
            buf.newLine();
        }
        buf.close();
        fw.close();
    }
}
