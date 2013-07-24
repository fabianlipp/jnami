package nami.connector.namitypes;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;

import nami.connector.NamiConnector;
import nami.connector.NamiResponse;
import nami.connector.NamiURIBuilder;
import nami.connector.exception.NamiApiException;
import nami.connector.exception.NamiException;

import org.apache.http.client.methods.HttpGet;

import com.google.gson.reflect.TypeToken;

/**
 * Stellt ein Mitglied der DPSG dar.
 * 
 * @author Fabian Lipp
 * 
 */
public class NamiMitglied {
    public static class KontoverbindungType {
        private String institut;
        private String kontonummer;
        private String bankleitzahl;
        private String iban;
        private String id;
        private String bic;
        private String mitgliedsNummer;
        private String kontoinhaber;
    }

    private int id;
    private int mitgliedsNummer;

    private String beitragsarten;
    private Collection<Integer> beitragsartenId;
    private String statusId; // ENUM??
    private String status; // ENUM?? (z.B. AKTIV)

    private String vorname;
    private String nachname;
    private String strasse;
    private String plz;
    private String ort;
    private String telefon1;
    private String telefon2;
    private String telefon3;
    private String telefax;
    private String email;
    private String emailVertretungsberechtigter;

    private String staatsangehoerigkeitId; // int?
    private String staatsangehoerigkeit;
    private String staatsangehoerigkeitText;

    private String mglTypeId; // ENUM?? z.B. NICHT_MITGLIED
    private String mglType;

    private String geburtsDatumFormatted;
    private String geburtsDatum;

    private String regionId; // int? (null möglich)
    private String region;

    private String landId; // int?
    private String land;

    private String gruppierung;
    private int gruppierungId;
    // private String ersteUntergliederungId" : null, //?
    private String ersteUntergliederung;

    private String ersteTaetigkeitId;
    private String ersteTaetigkeit;
    private String stufe;

    private boolean wiederverwendenFlag;
    private boolean zeitschriftenversand;

    private String konfessionId; // int?
    private String konfession; // ENUM?
    private String geschlechtId;
    private String geschlecht;

    private String eintrittsdatum;
    private String zahlungsweise;
    private int version;
    private String lastUpdated;

    private KontoverbindungType kontoverbindung;

    // TODO: Getter
    // TODO: toString() überladen

    /**
     * Holt den Datensatz eines Mitglieds aus NaMi.
     * 
     * @param con
     *            Verbindung zum NaMi-Server
     * @param id
     *            ID des Mitglieds
     * @return Mitgliedsdatensatz
     * @throws IOException
     *             IOException
     * @throws NamiApiException
     *             API-Fehler beim Zugriff auf NaMi
     */
    public static NamiMitglied getMitgliedById(NamiConnector con, int id)
            throws IOException, NamiApiException {
        NamiURIBuilder builder = con
                .getURIBuilder(NamiURIBuilder.URL_NAMI_MITGLIED);
        builder.appendPath(Integer.toString(id));

        HttpGet httpGet = new HttpGet(builder.build());

        Type type = new TypeToken<NamiResponse<NamiMitglied>>() {
        } .getType();
        NamiResponse<NamiMitglied> resp = con.executeApiRequest(httpGet, type);

        if (resp.isSuccess()) {
            return resp.getData();
        } else {
            return null;
        }
    }

    /**
     * Fragt die ID eines Mitglieds anhand der Mitgliedsnummer ab.
     * 
     * @param con
     *            Verbindung zum NaMi-Server
     * @param mitgliedsnummer
     *            Mitgliedsnummer des Mitglieds
     * @return Mitglieds-ID
     * @throws IOException
     *             IOException
     * @throws NamiApiException
     *             API-Fehler beim Zugriff auf NaMi
     * @throws NamiException
     *             Fehler der bei der Anfrage an NaMi auftritt
     * 
     */
    public static int getIdByMitgliedsnummer(NamiConnector con,
            String mitgliedsnummer) throws IOException, NamiException {

        NamiSearchedValues search = new NamiSearchedValues();
        search.setMitgliedsnummer(mitgliedsnummer);

        NamiResponse<Collection<NamiMitgliedListElement>> resp = search
                .getSearchResult(con, 1, 1, 0);

        if (resp.getTotalEntries() == 0) {
            return -1;
        } else if (resp.getTotalEntries() > 1) {
            throw new NamiException(
                    "Mehr als ein Mitglied mit Mitgliedsnummer "
                            + mitgliedsnummer);
        } else {
            // genau ein Ergebnis -> Hol das erste Element aus Liste
            NamiMitgliedListElement result = resp.getData().iterator().next();
            return result.getId();
        }

    }
}
