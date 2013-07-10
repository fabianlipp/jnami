package nami.connector.namitypes;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URISyntaxException;

import nami.connector.NamiApiException;
import nami.connector.NamiConnector;
import nami.connector.NamiResponse;
import nami.connector.NamiURIBuilder;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;

import com.google.gson.reflect.TypeToken;

public class NamiTaetigkeitAssignment {
    private int id;
    private String gruppierung;
    private int gruppierungId;
    private String taetigkeit;
    private int taetigkeitId;
    private String caeaGroup;
    private int caeaGroupId;
    private String caeaGroupForGf;
    private int caeaGroupForGfId;
    private String untergliederung;
    private int untergliederungId;
    private String aktivVon;
    private String aktivBis;
    
    public int getGruppierungId() {
        return gruppierungId;
    }
    public int getTaetigkeitId() {
        return taetigkeitId;
    }
    public int getUntergliederungId() {
        return untergliederungId;
    }
    
    public boolean istAktiv(){
        return aktivBis.isEmpty();
    }
    
    public static NamiTaetigkeitAssignment getTaetigkeit(NamiConnector con, int personId, int taetigkeitId) throws URISyntaxException, ClientProtocolException, IOException, NamiApiException {
        final String URL_NAMI_TAETIGKEIT= "/rest/api/1/2/service/nami/zugeordnete-taetigkeiten/filtered-for-navigation/gruppierung-mitglied/mitglied";
    
        NamiURIBuilder builder = con.getURIBuilder(URL_NAMI_TAETIGKEIT);
        builder.appendPath(Integer.toString(personId));
        builder.appendPath(Integer.toString(taetigkeitId));
        HttpGet httpGet = new HttpGet(builder.build());
        Type type = new TypeToken<NamiResponse<NamiTaetigkeitAssignment>>() { }.getType();
        NamiResponse<NamiTaetigkeitAssignment> resp = con.executeApiRequest(httpGet, type);
        
        if (resp.isSuccess()) {
            return resp.getData();
        } else {
            return null;
        }
    }
}
