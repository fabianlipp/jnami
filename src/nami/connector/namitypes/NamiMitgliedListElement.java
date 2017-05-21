package nami.connector.namitypes;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;

import nami.connector.Geschlecht;
import nami.connector.MitgliedStatus;
import nami.connector.Mitgliedstyp;
import nami.connector.NamiConnector;
import nami.connector.NamiResponse;
import nami.connector.NamiURIBuilder;
import nami.connector.exception.NamiApiException;

import org.apache.http.client.methods.HttpGet;

import com.google.gson.reflect.TypeToken;

public class NamiMitgliedListElement extends NamiAbstractMitglied implements
        Comparable<NamiMitgliedListElement> {
    private int entries_id;

    private String entries_vorname;
    private String entries_nachname;

    private String entries_email;
    private String entries_emailVertretungsberechtigter;
    private String entries_telefon1;
    private String entries_telefon2;
    private String entries_telefon3;
    private String entries_telefax;

    // nur in Suche, nicht in Mitgliederverwaltung
    private String entries_gruppierungId;
    private String entries_gruppierung;

    private String entries_stufe;
    private String entries_geburtsDatum;

    private String entries_mglType;
    private String entries_status;

    private String entries_staatsangehoerigkeit;
    private String entries_staatangehoerigkeitText;
    private String entries_geschlecht;
    private String entries_konfession;
    private String entries_rowCssClass;
    private String entries_lastUpdated;
    private int entries_version;
    private boolean entries_wiederverwendenFlag;
    private int entries_mitgliedsNummer;
    private String entries_eintrittsdatum;

    private String descriptor;
    private int id;

    @Override
    public int getId() {
        return id;
    }

    @Override
    public int getGruppierungId() {
        return Integer.parseInt(entries_gruppierungId);
    }

    @Override
    public String getGruppierung() {
        return entries_gruppierung;
    }

    @Override
    public String getVorname() {
        return entries_vorname;
    }

    @Override
    public String getNachname() {
        return entries_nachname;
    }

    @Override
    public String getEmail() {
        return entries_email;
    }

    @Override
    public MitgliedStatus getStatus() {
        return MitgliedStatus.fromString(entries_status);
    }

    @Override
    public Mitgliedstyp getMitgliedstyp() {
        return Mitgliedstyp.fromString(entries_mglType);
    }

    @Override
    public Geschlecht getGeschlecht() {
        return Geschlecht.fromString(entries_geschlecht);
    }

    @Override
    public int getMitgliedsnummer() {
        return entries_mitgliedsNummer;
    }

    @Override
    public int getVersion() {
        return entries_version;
    }

    @Override
    public NamiMitglied getFullData(NamiConnector con) throws NamiApiException,
            IOException {
        return NamiMitglied.getMitgliedById(con, id);
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

    /**
     * Liefert die Mitglieder, die einer bestimmten Gruppierung angehören
     * (entweder als Stammgruppierung oder sie üben dort eine Tätigkeit aus).
     * 
     * @param con
     *            Verbindung zum NaMi-Server
     * @param gruppierungsnummer
     *            Nummer der Gruppierung, in der gesucht werden soll
     * @return gefundene Mitglieder
     * @throws NamiApiException
     *             API-Fehler beim Zugriff auf NaMi
     * @throws IOException
     *             IOException
     */
    public static Collection<NamiMitgliedListElement> getMitgliederFromGruppierung(
            NamiConnector con, String gruppierungsnummer)
            throws NamiApiException, IOException {

        String url = String.format(
                NamiURIBuilder.URL_MITGLIEDER_FROM_GRUPPIERUNG,
                gruppierungsnummer);
        NamiURIBuilder builder = con.getURIBuilder(url);
        builder.setParameter("limit", "5000");
        builder.setParameter("page", "1");
        builder.setParameter("start", "0");
        HttpGet httpGet = new HttpGet(builder.build());

        Type type = new TypeToken<NamiResponse<Collection<NamiMitgliedListElement>>>() {
        }.getType();
        NamiResponse<Collection<NamiMitgliedListElement>> resp = con
                .executeApiRequest(httpGet, type);

        if (resp.isSuccess()) {
            return resp.getData();
        } else {
            throw new NamiApiException("Could not get member list from Nami: "
                    + resp.getMessage());
        }

    }

}
