package nami.connector.namitypes;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.Collection;

import nami.connector.Ebene;
import nami.connector.NamiApiException;
import nami.connector.NamiConnector;
import nami.connector.NamiResponse;
import nami.connector.NamiURIBuilder;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;

import com.google.gson.reflect.TypeToken;

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

    public transient final int INITIAL_LIMIT = 100;

    // public NamiSearchedValues(String mitgliedsNummber) {
    // this.mitgliedsNummber = mitgliedsNummber;
    // }

    public String getMitgliedsNummer() {
        return mitgliedsNummber;
    }

    public void setMitgliedsNummer(String mitgliedsNummer) {
        this.mitgliedsNummber = mitgliedsNummer;
    }

    public Integer getUntergliederungId() {
        return untergliederungId;
    }

    public void setUntergliederungId(Integer untergliederungId) {
        this.untergliederungId = untergliederungId;
    }

    public Integer getTaetigkeitId() {
        return taetigkeitId;
    }

    public void setTaetigkeitId(Integer taetigkeitId) {
        this.taetigkeitId = taetigkeitId;
    }

    public void setGruppierungId(Integer id) {
        Ebene ebene = Ebene.getFromGruppierungId(id);
        gruppierungDioezeseId = null;
        gruppierungBezirkId = null;
        gruppierungStammId = null;
        switch (ebene) {
        case BUND:
            break;
        case DIOEZESE:
            gruppierungDioezeseId = id;
            break;
        case BEZIRK:
            gruppierungBezirkId = id;
            break;
        case STAMM:
            gruppierungStammId = id;
            break;
        }
    }

    public NamiResponse<Collection<NamiMitgliedListElement>> getSearchResult(
            NamiConnector con, int limit, int page, int start)
            throws URISyntaxException, ClientProtocolException, IOException,
            NamiApiException {
        final String URL_NAMI_SEARCH = "/rest/api/1/2/service/nami/search/result-list";

        NamiURIBuilder builder = con.getURIBuilder(URL_NAMI_SEARCH);
        builder.setParameter("limit", Integer.toString(limit));
        builder.setParameter("page", Integer.toString(page));
        builder.setParameter("start", Integer.toString(start));
        builder.setParameter("searchedValues", con.gson.toJson(this));
        HttpGet httpGet = new HttpGet(builder.build());

        Type type = new TypeToken<NamiResponse<Collection<NamiMitgliedListElement>>>() {
        }.getType();
        NamiResponse<Collection<NamiMitgliedListElement>> resp = con
                .executeApiRequest(httpGet, type);

        return resp;
    }

    public Collection<NamiMitgliedListElement> getAllResults(NamiConnector con)
            throws ClientProtocolException, NamiApiException,
            URISyntaxException, IOException {
        NamiResponse<Collection<NamiMitgliedListElement>> resp = getSearchResult(
                con, INITIAL_LIMIT, 1, 0);
        if (resp.getTotalEntries() > INITIAL_LIMIT) {
            resp = getSearchResult(con, resp.getTotalEntries(), 1, 0);
        }
        return resp.getData();
    }
}
