package nami.connector.namitypes;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Properties;
import java.util.regex.Pattern;

import nami.connector.NamiConnector;
import nami.connector.NamiURIBuilder;
import nami.connector.exception.NamiApiException;

import org.apache.http.client.methods.HttpGet;

import com.google.gson.reflect.TypeToken;

/**
 * Beschreibt eine Beitragszahlung eines Mitglieds, wie sie in NaMi erfasst ist,
 * d.h. die Berechnung des Mitgliedsbeitrages für ein bestimmtes Mitglied für
 * einen bestimmten Zeitraum.
 * 
 * @author Fabian Lipp
 * 
 */
// TODO: Debug: Lombok
@lombok.ToString
public class NamiBeitragszahlung {

    private String id;
    private String rechnungsNummer;
    private String rechnungsPosition;
    private String buchungsText;
    private String beitragsSatz;
    private String value;
    private String beitragBis;
    private String status;
    private String beitragsKonto;

    /**
     * Liefert die Liste der Beitragszahlungen eines Mitglieds.
     * 
     * @param con
     *            Verbindung zum NaMi-Server
     * @param mitgliedId
     *            ID des Mitglieds
     * @return in NaMi erfasste Beitragszahlungen des Mitglieds
     * @throws NamiApiException
     *             Fehler bei der Anfrage an NaMi
     * @throws IOException
     *             IOException
     */
    public static Collection<NamiBeitragszahlung> getBeitragszahlungen(
            NamiConnector con, int mitgliedId) throws NamiApiException,
            IOException {
        NamiURIBuilder builder = con.getURIBuilder(
                NamiURIBuilder.URL_BEITRAGSZAHLUNGEN, false);
        builder.setParameter("id", Integer.toString(mitgliedId));
        HttpGet httpGet = new HttpGet(builder.build());

        Type type = new TypeToken<Collection<NamiBeitragszahlung>>() {
        }.getType();

        // Load Regular Expression
        Properties regexpProp = new Properties();
        InputStream propXml = NamiBeitragszahlung.class
                .getResourceAsStream("regexp.xml");
        regexpProp.loadFromXML(propXml);
        String regex = regexpProp.getProperty("regex.beitragszahlungen");
        Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);

        Collection<NamiBeitragszahlung> resp = con.executeHtmlRequest(httpGet,
                pattern, type);

        return resp;
    }
}
