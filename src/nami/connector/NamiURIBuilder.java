package nami.connector;

import java.net.URI;
import java.net.URISyntaxException;

import nami.connector.exception.NamiURISyntaxException;

import org.apache.http.client.utils.URIBuilder;

/**
 * Baut URIs für die Anfragen an NaMi zusammen.
 * 
 * @author Fabian Lipp
 * 
 */
public class NamiURIBuilder extends URIBuilder {
    /**
     * URL, die zum Login in NaMi verwendet wird.
     */
    public static final String URL_NAMI_STARTUP = "/rest/nami/auth/manual/sessionStartup";

    /**
     * URL, mit der die Root-Gruppierung und die Kinder für jede Gruppierung
     * abgefragt werden.
     */
    public static final String URL_NAMI_GRP = "/rest/api/1/2/service/nami/gruppierungen/filtered-for-navigation/gruppierung";

    /**
     * URL, mit der der Datensatz eines Mitglieds (identifiziert durch seine ID)
     * abgefragt wird.
     */
    // Am Ende der URL müsste eigentlich die GruppierungsID angegeben sein.
    // Scheinbar kann man aber auch immer "0" angeben und bekommt
    // trotzdem jedes Mitglied geliefert
    public static final String URL_NAMI_MITGLIED = "/rest/api/1/2/service/nami/mitglied/filtered-for-navigation/gruppierung/gruppierung/0";

    /**
     * URL, mit der ein Tätigkeitszuordnung eines Mitglieds abgefragt wird.
     */
    public static final String URL_NAMI_TAETIGKEIT = "/rest/api/1/2/service/nami/zugeordnete-taetigkeiten/filtered-for-navigation/gruppierung-mitglied/mitglied";

    /**
     * URL, um eine Suchanfrage an NaMi zu senden.
     */
    public static final String URL_NAMI_SEARCH = "/rest/api/1/2/service/nami/search/result-list";

    /**
     * Erzeugt einen URIBuilder für den gegebenen Server und hängt sofort einen
     * Pfad an die URI an.
     * 
     * @param server
     *            Server, auf den die URI zeigt
     * @param path
     *            Pfad, der an die URI angefügt wird. Das NamiDeploy-Verzeichnis
     *            (z.B. ica) aus der Server-Konfiguration wird automatisch vorne
     *            angefügt.
     */
    public NamiURIBuilder(NamiServer server, String path) {
        super();
        if (server.getUseSsl()) {
            setScheme("https");
        } else {
            setScheme("http");
        }
        setHost(server.getNamiServer());
        setPath("/" + server.getNamiDeploy());
        appendPath(path);
    }

    /**
     * Hängt einen weiteren Abschnitt an den Pfad an. Bei Bedarf wird ein '/'
     * vorne eingefügt.
     * 
     * @param pathAppendix
     *            zu ergänzender Pfad
     */
    public void appendPath(String pathAppendix) {
        String path = getPath();
        if ((path.charAt(path.length() - 1) != '/')
                && (pathAppendix.charAt(0) != '/')) {
            setPath(path + "/" + pathAppendix);
        } else {
            setPath(path + pathAppendix);
        }
    }

    @Override
    public URI build() {
        try {
            return super.build();
        } catch (URISyntaxException e) {
            throw new NamiURISyntaxException(e);
        }
    }

}
