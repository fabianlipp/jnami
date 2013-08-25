package nami.connector;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.DomSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.w3c.dom.Document;

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
     * @throws IOException
     *             IOException
     * @throws NamiLoginException
     *             Probleme beim NaMi-Login, z. B. falsche Zugangsdaten
     */
    public void namiLogin() throws IOException, NamiLoginException {
        HttpPost httpPost = new HttpPost(NamiURIBuilder.getLoginURIBuilder(
                server).build());
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();

        nvps.add(new BasicNameValuePair("username", credentials.getApiUser()));
        nvps.add(new BasicNameValuePair("password", credentials.getApiPass()));

        if (server.useApiAccess()) {
            nvps.add(new BasicNameValuePair("Login", "API"));
            nvps.add(new BasicNameValuePair("redirectTo",
                    "./pages/loggedin.jsp"));
            httpPost.setEntity(new UrlEncodedFormEntity(nvps));
            HttpResponse response = execute(httpPost, true);
            HttpEntity responseEntity = response.getEntity();

            Type type = NamiApiResponse.getType(Object.class);
            NamiApiResponse<Object> resp = gson.fromJson(new InputStreamReader(
                    responseEntity.getContent()), type);

            if (resp.getStatusCode() == 0) {
                isAuthenticated = true;
                log.info("Authenticated to NaMi-Server using API.");
                // SessionToken wird automatisch als Cookie im HttpClient
                // gespeichert
            } else {
                // Fehler beim Verbinden (3000 z.B. bei falschem Passwort)
                isAuthenticated = false;
                throw new NamiLoginException(resp);
            }
        } else {
            nvps.add(new BasicNameValuePair("redirectTo", "app.jsp"));
            nvps.add(new BasicNameValuePair("Login", "Anmelden"));
            httpPost.setEntity(new UrlEncodedFormEntity(nvps));
            HttpResponse response = execute(httpPost, false);

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY) {
                // need to follow one redirect
                Header locationHeader = response.getFirstHeader("Location");
                EntityUtils.consume(response.getEntity());
                if (locationHeader != null) {
                    String redirectUrl = locationHeader.getValue();
                    HttpGet httpGet = new HttpGet(redirectUrl);
                    response = execute(httpGet, false);
                    log.info("Got redirect to: " + redirectUrl);

                    if (response.getStatusLine().getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY) {
                        // login successful
                        EntityUtils.consume(response.getEntity());
                        isAuthenticated = true;
                        log.info("Authenticated to NaMi-Server without API.");
                    }
                }
            } else {
                // login not successful
                isAuthenticated = false;
                String error = "";
                try {
                    TagNode tagNode = new HtmlCleaner().clean(response
                            .getEntity().getContent());

                    /*
                     * DocumentBuilderFactory dbf = DocumentBuilderFactory
                     * .newInstance(); DocumentBuilder builder =
                     * dbf.newDocumentBuilder(); Document doc =
                     * builder.parse(response.getEntity().getContent());
                     */
                    Document doc = new DomSerializer(new CleanerProperties())
                            .createDOM(tagNode);
                    XPathFactory xpathFac = XPathFactory.newInstance();
                    XPath xpath = xpathFac.newXPath();
                    XPathExpression expr = xpath.compile("/html/body//p[1]");
                    error = (String) expr.evaluate(doc, XPathConstants.STRING);
                } catch (Exception e) {
                    throw new NamiLoginException(e);
                }
                throw new NamiLoginException(error);
            }
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
     * @throws IOException
     *             IOException
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
        HttpResponse response = execute(request, false);
        HttpEntity responseEntity = response.getEntity();

        // Teste, ob der Statuscode der Antwort 200 (OK) ist
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            if (server.useApiAccess()) {
                // check if response is redirect to an error page
                Header locationHeader = response.getFirstHeader("Location");
                if (locationHeader != null) {
                    // Extract error message from URL in location
                    URL redirectUrl = new URL(locationHeader.getValue());
                    log.warning("Got redirect to: " + redirectUrl.toString());
                    if (redirectUrl.getPath().contains("error.jsp")) {
                        String message = URLDecoder.decode(
                                redirectUrl.getQuery(), "UTF-8");
                        message = message.split("=", 2)[1];
                        throw new NamiApiException(message);
                    }
                }
            } else {
                // extract description from JBoss error page
                String error = "";
                try {
                    TagNode tagNode = new HtmlCleaner().clean(response
                            .getEntity().getContent());

                    /*
                     * DocumentBuilderFactory dbf = DocumentBuilderFactory
                     * .newInstance(); DocumentBuilder builder =
                     * dbf.newDocumentBuilder(); Document doc =
                     * builder.parse(response.getEntity().getContent());
                     */
                    Document doc = new DomSerializer(new CleanerProperties())
                            .createDOM(tagNode);
                    XPathFactory xpathFac = XPathFactory.newInstance();
                    XPath xpath = xpathFac.newXPath();
                    // XPath describes content of description field
                    XPathExpression expr = xpath.compile("/html/body/p[3]/u");
                    error = (String) expr.evaluate(doc, XPathConstants.STRING);
                } catch (Exception e) {
                    throw new NamiApiException(e);
                }
                throw new NamiApiException(error);
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
        if (server.useApiAccess()) {
            Type type = NamiApiResponse.getType(typeOfT);
            NamiApiResponse<T> resp = gson.fromJson(new InputStreamReader(
                    responseEntity.getContent()), type);

            if (resp.getStatusCode() != 0) {
                throw new NamiApiException(resp);
            }
            return resp.getResponse();
        } else {
            T resp = gson.fromJson(new InputStreamReader(responseEntity.getContent()),
                            typeOfT);
            return resp;
        }
    }

    /**
     * Liefert einen URIBuilder für den NaMi-Server dieser Connection.
     * 
     * @param path
     *            Pfad, der aufgerufen wird
     * @return erzeugter URIBuilder
     */
    public NamiURIBuilder getURIBuilder(String path) {
        return new NamiURIBuilder(server, path);
    }

    /**
     * Verwendet die GSON-Instanz der Connection, um ein Objekt in JSON zu
     * konvertieren.
     * 
     * @param o
     *            zu kodierendes Objekt
     * @return erzeugter JSON-String
     */
    public String toJson(Object o) {
        return gson.toJson(o);
    }
}
