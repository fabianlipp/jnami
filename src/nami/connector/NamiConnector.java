package nami.connector;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import nami.connector.credentials.NamiCredentials;
import nami.connector.namitypes.NamiGruppierung;
import nami.connector.namitypes.NamiMitglied;
import nami.connector.namitypes.NamiTaetigkeitAssignmentListElement;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.message.BasicNameValuePair;

import com.google.gson.Gson;

public class NamiConnector {
    private String namiServer;
    private boolean useSsl;
    private String namiDeploy;
    private NamiCredentials credentials;

    public Gson gson = new Gson();
    private DefaultHttpClient httpclient = new DefaultHttpClient();
    private boolean isAuthenticated = false;
    
    public NamiConnector(NamiServer server, NamiCredentials credentials) {
        httpclient.setRedirectStrategy(new LaxRedirectStrategy());
        
        namiServer = server.getNamiServer();
        useSsl = server.getUseSsl();
        namiDeploy = server.getNamiDeploy();
        
        this.credentials = credentials;
    }

    public static void main(String[] args) throws IOException,
            URISyntaxException, NamiException {
        NamiCredentials cred = new NamiCredentials("123456", "xyz");
        NamiConnector con = new NamiConnector(NamiServer.TESTSERVER, cred);
        con.namiLogin();

        int id = NamiMitglied.getIdByMitgliedsnummer(con, "123456");
        NamiTaetigkeitAssignmentListElement.getTaetigkeiten(con, id);
        NamiMitglied mgl = NamiMitglied.getMitgliedById(con, id);
        System.out.println(mgl);

        NamiGruppierung rootGrp = NamiGruppierung.getGruppierungen(con);
        System.out.println(rootGrp);

        System.exit(0);
    }

    public void namiLogin() throws IOException, URISyntaxException,
            NamiApiException {
        final String URL_NAMI_STARTUP = "/rest/nami/auth/manual/sessionStartup";

        HttpPost httpPost = new HttpPost(getURIBuilder(URL_NAMI_STARTUP)
                .build());
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("Login", "API"));
        nvps.add(new BasicNameValuePair("username", credentials.getApiUser()));
        nvps.add(new BasicNameValuePair("password", credentials.getApiPass()));
        nvps.add(new BasicNameValuePair("redirectTo", "./pages/loggedin.jsp"));
        httpPost.setEntity(new UrlEncodedFormEntity(nvps));
        HttpResponse response = execute(httpPost);

        HttpEntity responseEntity = response.getEntity();
        
        Type type = NamiApiResponse.getType(Object.class);
        NamiApiResponse<Object> resp = gson.fromJson(new InputStreamReader(
                responseEntity.getContent()), type);

        if (resp.getStatusCode() == 0) {
            isAuthenticated = true;
            // SessionToken wird automatisch als Cookie im HttpClient
            // gespeichert
        } else {
            // Fehler beim Verbinden (3000 z.B. bei falschem Passwort)
            isAuthenticated = false;
            throw new NamiApiException(resp);
        }
    }

    private HttpResponse execute(HttpUriRequest request)
            throws ClientProtocolException, IOException {
        System.out.println(request.getURI());
        return httpclient.execute(request);
    }

    public <T> T executeApiRequest(HttpUriRequest request, final Type typeOfT)
            throws ClientProtocolException, IOException, NamiApiException {
        if (!isAuthenticated) {
            throw new NamiApiException("Did not login before API Request.");
        }

        Type type = NamiApiResponse.getType(typeOfT);
        HttpResponse response = execute(request);
        HttpEntity responseEntity = response.getEntity();
        NamiApiResponse<T> resp = gson.fromJson(new InputStreamReader(
                responseEntity.getContent()), type);

        if (resp.getStatusCode() != 0) {
            throw new NamiApiException(resp);
        }
        return resp.getResponse();
    }

    public NamiURIBuilder getURIBuilder() {
        return new NamiURIBuilder(useSsl, namiServer, namiDeploy);
    }

    public NamiURIBuilder getURIBuilder(String path) {
        return new NamiURIBuilder(useSsl, namiServer, namiDeploy, path);
    }
}
