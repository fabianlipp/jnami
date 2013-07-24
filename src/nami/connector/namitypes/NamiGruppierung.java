package nami.connector.namitypes;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedList;

import nami.connector.NamiConnector;
import nami.connector.NamiResponse;
import nami.connector.NamiURIBuilder;
import nami.connector.exception.NamiApiException;

import org.apache.http.client.methods.HttpGet;

import com.google.gson.reflect.TypeToken;

/**
 * Beschreibt eine Gruppierung der DPSG.
 * 
 * @author Fabian Lipp
 * 
 */
public class NamiGruppierung {
    // Die folgenden Variablen stammen aus NaMi. Keinesfalls umbenennen.
    private String descriptor;
    private int id;

    // die Kindknoten werden nicht automatisch von NaMi geliefert, sondern
    // müssen extra abgefragt werden.
    private Collection<NamiGruppierung> children;

    /**
     * Liefert den Namen der Gruppierung ("Stamm XYZ").
     * 
     * @return Name
     */
    public String getDescriptor() {
        return descriptor;
    }

    /**
     * Liefert die Gruppierungsnummer.
     * 
     * @return Gruppierungsnummer
     */
    public int getId() {
        return id;
    }

    /**
     * Liefert die untergeordneten Gruppierungen.
     * 
     * @return Kind-Gruppierungen
     */
    public Collection<NamiGruppierung> getChildren() {
        return children;
    }

    @Override
    public String toString() {
        return descriptor;
    }

    /**
     * Liest den kompletten Gruppierungsbaum aus, auf den der Benutzer Zugriff
     * hat.
     * 
     * @param con
     *            Verbindung zum NaMi-Server
     * @return Root-Gruppierung (in dieser sind die Kinder gespeichert)
     * @throws IOException
     *             IOException
     * @throws NamiApiException
     *             API-Fehler beim Zugriff auf NaMi
     */
    public static NamiGruppierung getGruppierungen(NamiConnector con)
            throws IOException, NamiApiException {
        NamiGruppierung rootGrp = getRootGruppierung(con);

        rootGrp.children = getChildGruppierungen(con,
                Integer.toString(rootGrp.id));

        return rootGrp;
    }

    // TODO: in der Live-Version wohl nicht nötig
    private boolean isActive() {
        return !descriptor.contains("***");
    }

    /**
     * Liest die Root-Gruppierung aus NaMi aus. Es wird nicht der gesamte
     * Gruppierungsbaum gelesen, d.h. in der Root-Gruppierung wird anstelle der
     * Liste der Kinder nur <code>null</code> gespeichert.
     * 
     * @param con
     *            Verbindung zum NaMi-Server
     * @return Root-Gruppierung ohne Kinder
     * @throws IOException
     *             IOException
     * @throws NamiApiException
     *             API-Fehler beim Zugriff auf NaMi
     */
    public static NamiGruppierung getRootGruppierung(NamiConnector con)
            throws IOException, NamiApiException {
        NamiURIBuilder builder = con.getURIBuilder(NamiURIBuilder.URL_NAMI_GRP);
        builder.appendPath("root");
        builder.addParameter("node", "root");
        HttpGet httpGet = new HttpGet(builder.build());

        Type type = new TypeToken<NamiResponse<Collection<NamiGruppierung>>>() {
        } .getType();
        NamiResponse<Collection<NamiGruppierung>> resp = con.executeApiRequest(
                httpGet, type);

        NamiGruppierung rootGrp = resp.getData().iterator().next();
        rootGrp.children = null;

        return rootGrp;
    }

    private static Collection<NamiGruppierung> getChildGruppierungen(
            NamiConnector con, String rootGruppierung) throws IOException,
            NamiApiException {
        NamiURIBuilder builder = con.getURIBuilder(NamiURIBuilder.URL_NAMI_GRP);
        builder.appendPath(rootGruppierung);
        builder.addParameter("node", rootGruppierung);
        HttpGet httpGet = new HttpGet(builder.build());

        Type type = new TypeToken<NamiResponse<Collection<NamiGruppierung>>>() {
        } .getType();
        NamiResponse<Collection<NamiGruppierung>> resp = con.executeApiRequest(
                httpGet, type);

        Collection<NamiGruppierung> allChildren = resp.getData();
        Collection<NamiGruppierung> activeChildren = new LinkedList<>();
        for (NamiGruppierung child : allChildren) {
            if (child.isActive()) {
                activeChildren.add(child);
                child.children = getChildGruppierungen(con,
                        Integer.toString(child.id));
            }
        }

        return activeChildren;
    }
}
