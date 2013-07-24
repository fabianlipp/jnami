package nami.connector.namitypes;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;

import nami.connector.Ebene;
import nami.connector.NamiConnector;
import nami.connector.NamiResponse;
import nami.connector.NamiURIBuilder;
import nami.connector.exception.NamiApiException;

import org.apache.http.client.methods.HttpGet;

import com.google.gson.reflect.TypeToken;

/**
 * Beschreibt eine Anfrage für die Suchfunktion in NaMi.
 * 
 * @author Fabian Lipp
 * 
 */
// TODO: an neue NaMi-Suchfunktion anpassen
public class NamiSearchedValues {
    private String vorname = "";
    private String nachname = "";
    private String alterVon = "";
    private String alterBis = "";
    private String mitgliedsNummber = ""; // Rechtschreibfehler in NaMi
    private Integer untergliederungId = null;
    private Integer taetigkeitId = null;
    private boolean withEndedTaetigkeiten = false;

    private Integer ebeneId = null;
    private Integer gruppierungDioezeseId = null;
    private Integer gruppierungBezirkId = null;
    private Integer gruppierungStammId = null;
    private Integer tagId = null;
    private Integer bausteinIncludedId = null;

    private String id = "";
    private String searchName = "";

    /**
     * Maximale Anzahl der gefundenen Datensätze, wenn kein Limit vorgegeben
     * wird.
     */
    // transient bewirkt, dass die Variable nicht in die JSON-Darstellung
    // aufgenommen wird
    private static final transient int INITIAL_LIMIT = 5000;

    /**
     * Liefert die Mitgliedsnummer in der Suchanfrage.
     * 
     * @return Mitgliedsnummer
     */
    public String getMitgliedsnummer() {
        return mitgliedsNummber;
    }

    /**
     * Setzt die Mitgliedsnummer, nach der gesucht werden soll.
     * 
     * @param mitgliedsnummer
     *            .
     */
    public void setMitgliedsnummer(String mitgliedsnummer) {
        this.mitgliedsNummber = mitgliedsnummer;
    }

    /**
     * Liefert die Untergliederung (Stufe/Abteilung) in der Suchanfrage.
     * 
     * @return Untergliederungs-ID
     */
    public Integer getUntergliederungId() {
        return untergliederungId;
    }

    /**
     * Setzt die Untergliederungs-ID, nach der gesucht werden soll.
     * 
     * @param untergliederungId
     *            .
     */
    public void setUntergliederungId(Integer untergliederungId) {
        this.untergliederungId = untergliederungId;
    }

    /**
     * Liefert die Tätigkeit in der Suchanfrage.
     * 
     * @return Tätigkeits-ID
     */
    public Integer getTaetigkeitId() {
        return taetigkeitId;
    }

    /**
     * Setzt die Tätigkeits-ID, nach der gesucht werden soll.
     * 
     * @param taetigkeitId
     *            .
     */
    public void setTaetigkeitId(Integer taetigkeitId) {
        this.taetigkeitId = taetigkeitId;
    }

    /**
     * Setzt die Gruppierung, in der gesucht werden soll.
     * 
     * @param gruppierungsnummer
     *            .
     */
    public void setGruppierungId(Integer gruppierungsnummer) {
        Ebene ebene = Ebene.getFromGruppierungId(gruppierungsnummer);
        gruppierungDioezeseId = null;
        gruppierungBezirkId = null;
        gruppierungStammId = null;
        switch (ebene) {
        case BUND:
            break;
        case DIOEZESE:
            gruppierungDioezeseId = gruppierungsnummer;
            break;
        case BEZIRK:
            gruppierungBezirkId = gruppierungsnummer;
            break;
        case STAMM:
            gruppierungStammId = gruppierungsnummer;
            break;
        default:
        }
    }

    /**
     * Liefert einen Teil der Mitglieder, die der Suchanfrage entsprechen.
     * 
     * @param con
     *            Verbindung zum NaMi-Server
     * @param limit
     *            maximale Anzahl an gelieferten Ergebnissen
     * @param page
     *            Seite
     * @param start
     *            Index des ersten zurückgegeben Datensatzes in der gesamten
     *            Ergebnismenge
     * @return gefundene Mitglieder //TODO: stimmt momentan nicht exakt wegen
     *         NamiRepsonse
     * @throws IOException
     *             IOException
     * @throws NamiApiException
     *             API-Fehler beim Zugriff auf NaMi
     */
    // TODO: Warum NamiResponse nötig
    // -> gebe stattdessen direkt die Collection zurück oder null, wenn kein
    // success
    // TODO: wird hier überhaupt von außen zugegriffen oder reicht diese Methode
    // private?
    public NamiResponse<Collection<NamiMitgliedListElement>> getSearchResult(
            NamiConnector con, int limit, int page, int start)
            throws IOException, NamiApiException {

        NamiURIBuilder builder = con
                .getURIBuilder(NamiURIBuilder.URL_NAMI_SEARCH);
        builder.setParameter("limit", Integer.toString(limit));
        builder.setParameter("page", Integer.toString(page));
        builder.setParameter("start", Integer.toString(start));
        builder.setParameter("searchedValues", con.toJson(this));
        HttpGet httpGet = new HttpGet(builder.build());

        Type type = new TypeToken<NamiResponse<Collection<NamiMitgliedListElement>>>() {
        }.getType();
        NamiResponse<Collection<NamiMitgliedListElement>> resp = con
                .executeApiRequest(httpGet, type);

        return resp;
    }

    // TODO: Teste was passiert, wenn es keine Treffer gibt bzw. die Suchanfrage
    // ungültig ist
    /**
     * Liefert alle Mitglieder, die der Suchanfrage entsprechen.
     * 
     * @param con
     *            Verbindung zum NaMi-Server
     * @return gefundene Mitglieder
     * @throws IOException
     *             IOException
     * @throws NamiApiException
     *             API-Fehler beim Zugriff auf NaMi
     */
    public Collection<NamiMitgliedListElement> getAllResults(NamiConnector con)
            throws IOException, NamiApiException {
        NamiResponse<Collection<NamiMitgliedListElement>> resp = getSearchResult(
                con, INITIAL_LIMIT, 1, 0);

        if (resp.getTotalEntries() > INITIAL_LIMIT) {
            resp = getSearchResult(con, resp.getTotalEntries(), 1, 0);
        }
        return resp.getData();
    }

    // TODO: Anzahl der Ergebnisse abfragen
}
