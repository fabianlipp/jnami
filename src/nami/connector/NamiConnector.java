package nami.connector;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import nami.connector.credentials.NamiCredentials;
import nami.connector.exception.NamiApiException;
import nami.connector.exception.NamiLoginException;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpParams;

import com.google.gson.Gson;

/**
 * Baut die Verbindung zum NaMi-Server auf und schickt die API-Anfragen an ihn.
 * 
 * @author Fabian Lipp
 */
public class NamiConnector {
    // Adresse des Nami-Servers und Zugangsdaten
    private NamiServer server;
    private NamiCredentials credentials;

    private Gson gson = new Gson();
    private DefaultHttpClient httpclient = new DefaultHttpClient();
    private DefaultHttpClient httpclientNoRedirect = new DefaultHttpClient();
    private static Logger log = Logger.getLogger(NamiConnector.class.getName());

    /**
     * Speichert, ob der Login am NaMi-Server bereits erfolgreich durchgeführt
     * wurde.
     */
    private boolean isAuthenticated = false;

    /**
     * Erzeugt eine NaMi-Verbindung. Beim Erzeugen erfolgt noch <i>kein</i>
     * Login und es wird kein Kontakt zum Server aufgenommen.
     * 
     * @param server
     *            Adresse des Servers
     * @param credentials
     *            Zugangsdaten
     */
    public NamiConnector(NamiServer server, NamiCredentials credentials) {
        // Folge Redirects auch in POST und PUT (nötig, damit der Login so
        // funktioniert wie vorgesehen)
        httpclient.setRedirectStrategy(new LaxRedirectStrategy());

        // Deaktiviere Redirects
        HttpParams params = httpclientNoRedirect.getParams();
        params.setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, Boolean.FALSE);
        httpclientNoRedirect.setParams(params);

        this.server = server;
        this.credentials = credentials;
    }

    /**
     * Führt den Login am NaMi-Server durch.
     * 
     * @throws IOException IOException
     * @throws NamiLoginException
     *             Probleme beim NaMi-Login, z. B. falsche Zugangsdaten
     */
    public void namiLogin() throws IOException, NamiLoginException {
        HttpPost httpPost = new HttpPost(getURIBuilder(NamiURIBuilder.URL_NAMI_STARTUP)
                .build());
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("Login", "API"));
        nvps.add(new BasicNameValuePair("username", credentials.getApiUser()));
        nvps.add(new BasicNameValuePair("password", credentials.getApiPass()));
        nvps.add(new BasicNameValuePair("redirectTo", "./pages/loggedin.jsp"));
        httpPost.setEntity(new UrlEncodedFormEntity(nvps));
        HttpResponse response = execute(httpPost, true);

        HttpEntity responseEntity = response.getEntity();

        Type type = NamiApiResponse.getType(Object.class);
        NamiApiResponse<Object> resp = gson.fromJson(new InputStreamReader(
                responseEntity.getContent()), type);

        if (resp.getStatusCode() == 0) {
            isAuthenticated = true;
            log.info("Authenticated to NaMi-Server.");
            // SessionToken wird automatisch als Cookie im HttpClient
            // gespeichert
        } else {
            // Fehler beim Verbinden (3000 z.B. bei falschem Passwort)
            isAuthenticated = false;
            throw new NamiLoginException(resp);
        }
    }

    private HttpResponse execute(HttpUriRequest request, boolean followRedirect)
            throws IOException {
        log.fine("Sending request to NaMi-Server: " + request.getURI());
        HttpParams params = httpclient.getParams();
        params.setBooleanParameter(ClientPNames.HANDLE_REDIRECTS,
                followRedirect);
        httpclient.setParams(params);
        return httpclient.execute(request);
    }

    /**
     * Schickt eine Anfrage an den NaMi-Server und wandelt das gelieferte JSON
     * in ein passendes Objekt um.
     * 
     * @param request
     *            Anfrage, die an den Server geschickt wird
     * @param typeOfT
     *            Typ der Rückgabe. Hier wird nur der Typ der eigentlichen
     *            Antwort (ohne den durch die API hinzugefügten Wrapper)
     *            übergeben
     * @param <T>
     *            Typ des zurückgegebenen Objekts
     * @return das gelieferte Objekt
     * @throws IOException IOException
     * @throws NamiApiException
     *             wenn die Anfrage fehlschlägt. Das kann unter anderem folgende
     *             Gründe haben:
     *             <ul>
     *             <li>Der Statuscode der Antwort ist nicht 200 (OK)</li>
     *             <li>Der Content-Type der Antwort ist nicht application/json</li>
     *             <li>Im API-Wrapper-Objekt wird ein Status-Code ungleich 0
     *             geliefert</li>
     *             </ul>
     */
    public <T> T executeApiRequest(HttpUriRequest request, final Type typeOfT)
            throws IOException, NamiApiException {
        if (!isAuthenticated) {
            throw new NamiApiException("Did not login before API Request.");
        }

        // Sende Request an Server
        Type type = NamiApiResponse.getType(typeOfT);
        HttpResponse response = execute(request, false);
        HttpEntity responseEntity = response.getEntity();

        // Teste, ob der Statuscode der Antwort 200 (OK) ist
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            // check if response is redirect to an error page
            Header locationHeader = response.getFirstHeader("Location");
            if (locationHeader != null) {
                URL redirectUrl = new URL(locationHeader.getValue());
                log.warning("Got redirect to: " + redirectUrl.toString());
                if (redirectUrl.getPath().contains("error.jsp")) {
                    String message = URLDecoder.decode(redirectUrl.getQuery(),
                            "UTF-8");
                    message = message.split("=", 2)[1];
                    throw new NamiApiException(message);
                }
            }
            throw new NamiApiException("Statuscode of response is not 200 OK.");
        }

        // Teste, ob die Antwort JSON-formatiert ist (das wird in dieser
        // Funktion erwartet)
        Header contentType = responseEntity.getContentType();
        if (contentType == null) {
            throw new NamiApiException("Response has no Content-Type.");
        } else {
            if (!contentType.getValue().equals("application/json")) {
                IOUtils.copy(responseEntity.getContent(), System.out);
                throw new NamiApiException(
                        "Content-Type of response is not application/json.");
            }
        }

        // Decodiere geliefertes JSON
        NamiApiResponse<T> resp = gson.fromJson(new InputStreamReader(
                responseEntity.getContent()), type);

        if (resp.getStatusCode() != 0) {
            throw new NamiApiException(resp);
        }
        return resp.getResponse();
    }

    /**
     * Liefert einen URIBuilder für den NaMi-Server dieser Connection.
     * @param path Pfad, der aufgerufen wird
     * @return erzeugter URIBuilder
     */
    public NamiURIBuilder getURIBuilder(String path) {
        return new NamiURIBuilder(server, path);
    }

    /**
     * Verwendet die GSON-Instanz der Connection, um ein Objekt in JSON zu konvertieren.
     * @param o zu kodierendes Objekt
     * @return erzeugter JSON-String
     */
    public String toJson(Object o) {
        return gson.toJson(o);
    }
}
