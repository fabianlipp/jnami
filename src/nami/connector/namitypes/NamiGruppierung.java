package nami.connector.namitypes;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.LinkedList;

import nami.connector.NamiApiException;
import nami.connector.NamiConnector;
import nami.connector.NamiResponse;
import nami.connector.NamiURIBuilder;

import org.apache.http.client.methods.HttpGet;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

public class NamiGruppierung {
    private String descriptor;
    private int id;
    private Collection<NamiGruppierung> children;

    public String getDescriptor() {
        return descriptor;
    }

    public int getId() {
        return id;
    }

    public Collection<NamiGruppierung> getChildren() {
        return children;
    }

    @Override
    public String toString() {
        return descriptor;
    }

    // Baue Baum aus Gruppierungen auf
    public static NamiGruppierung getGruppierungen(NamiConnector con)
            throws JsonIOException, JsonSyntaxException, IllegalStateException,
            IOException, URISyntaxException, NamiApiException {
        NamiGruppierung rootGrp = getRootGruppierung(con);

        rootGrp.children = getChildGruppierungen(con,
                Integer.toString(rootGrp.id));

        return rootGrp;
    }

    public boolean isActive() {
        // scheinbar gibt es keine Möglichkeit die kompletten Stammdaten für
        // eine Gruppierung zu bekommen, für die man nicht alle Rechte hat.
        // Deswegen werden inaktive Gruppierungen anhand mehrerer
        // Sterne in der Bezeichnung erkannt.
        if (descriptor.contains("***")) {
            return false;
        } else {
            return true;
        }
    }

    public static NamiGruppierung getRootGruppierung(NamiConnector con)
            throws JsonIOException, JsonSyntaxException, IllegalStateException,
            IOException, URISyntaxException, NamiApiException {
        final String URL_NAMI_GRP = "/rest/api/1/2/service/nami/gruppierungen/filtered-for-navigation/gruppierung";
        NamiURIBuilder builder = con.getURIBuilder(URL_NAMI_GRP);
        builder.appendPath("root");
        builder.addParameter("node", "root");
        HttpGet httpGet = new HttpGet(builder.build());

        Type type = new TypeToken<NamiResponse<Collection<NamiGruppierung>>>() {
        }.getType();
        NamiResponse<Collection<NamiGruppierung>> resp = con.executeApiRequest(
                httpGet, type);

        return resp.getData().iterator().next();
    }

    private static Collection<NamiGruppierung> getChildGruppierungen(
            NamiConnector con, String rootGruppierung) throws JsonIOException,
            JsonSyntaxException, IllegalStateException, IOException,
            URISyntaxException, NamiApiException {
        final String URL_NAMI_GRP = "/rest/api/1/2/service/nami/gruppierungen/filtered-for-navigation/gruppierung";
        NamiURIBuilder builder = con.getURIBuilder(URL_NAMI_GRP);
        builder.appendPath(rootGruppierung);
        builder.addParameter("node", rootGruppierung);
        HttpGet httpGet = new HttpGet(builder.build());

        Type type = new TypeToken<NamiResponse<Collection<NamiGruppierung>>>() {
        }.getType();
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
