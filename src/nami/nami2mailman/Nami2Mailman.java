package nami.nami2mailman;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;

import nami.configuration.ConfigFormatException;
import nami.connector.NamiConnector;
import nami.connector.NamiResponse;
import nami.connector.NamiServer;
import nami.connector.credentials.NamiConsoleCredentials;
import nami.connector.credentials.NamiCredentials;
import nami.connector.exception.NamiApiException;
import nami.connector.namitypes.NamiMitgliedListElement;
import nami.connector.namitypes.NamiSearchedValues;
import nami.connector.namitypes.NamiTaetigkeitAssignment;
import nami.connector.namitypes.NamiTaetigkeitAssignmentListElement;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.xml.sax.SAXException;



public class Nami2Mailman {

    private static final int MAX_SEARCH_RESULTS = 1000;

    /**
     * @param args
     * @throws IOException
     * @throws JDOMException
     * @throws ConfigFormatException
     * @throws NamiApiException
     * @throws SAXException
     */
    public static void main(String[] args) throws IOException, JDOMException,
            ConfigFormatException, NamiApiException {

        String configFile = args[0];
        String outputPath = "./";

        // Lese Konfigurationsdatei ein
        File f = new File(configFile);
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(f);

        // Parse Konfiguration aus XML
        Collection<NamiSearchedValues> namiSearches = new LinkedList<>();
        Collection<Mailinglist> mailinglists = new LinkedList<>();
        Element namimailman = doc.getRootElement();
        if (namimailman.getName() != "namimailman") {
            throw new ConfigFormatException("Wrong root element in config file");
        }

        for (Element namiSearchEl : namimailman.getChildren("namiSearch")) {
            NamiSearchedValues namiSearch = new NamiSearchedValues();
            Element taetigkeitEl = namiSearchEl.getChild("taetigkeit");
            if (taetigkeitEl != null) {
                String id = taetigkeitEl.getAttributeValue("id");
                try {
                    namiSearch.setTaetigkeitId(Integer.valueOf(id));
                } catch (NumberFormatException e) {
                    throw new ConfigFormatException("Invalid ID for Taetigkeit");
                }
            }
            Element untergliederungEl = namiSearchEl
                    .getChild("untergliederung");
            if (untergliederungEl != null) {
                String id = untergliederungEl.getAttributeValue("id");
                try {
                    namiSearch.setUntergliederungId(Integer.valueOf(id));
                } catch (NumberFormatException e) {
                    throw new ConfigFormatException(
                            "Invalid ID for Untergliederung");
                }
            }
            namiSearches.add(namiSearch);
        }
        for (Element mailinglistEl : namimailman.getChildren("mailinglist")) {
            mailinglists.add(new Mailinglist(mailinglistEl));
        }

        // Nami-Verbindung aufbauen
        NamiCredentials cred = new NamiConsoleCredentials();
        NamiConnector namicon = new NamiConnector(NamiServer.TESTSERVER, cred);
        namicon.namiLogin();

        // Führe Suchen in Nami aus
        Set<NamiMitgliedListElement> persons = new TreeSet<>();
        for (NamiSearchedValues search : namiSearches) {
            NamiResponse<Collection<NamiMitgliedListElement>> resp = search
                    .getSearchResult(namicon, MAX_SEARCH_RESULTS, 0, 0);
            if (resp.isSuccess()) {
                persons.addAll(resp.getData());
            }
        }

        // Für jede Person im Suchergebnis:
        // Frage Tätigkeiten ab
        // Teste auf Zugehörigkeit in allen Mailinglisten
        for (NamiMitgliedListElement mgl : persons) {
            Collection<NamiTaetigkeitAssignmentListElement> respTaetigkeiten = NamiTaetigkeitAssignmentListElement
                    .getTaetigkeiten(namicon, mgl.getId());
            Collection<NamiTaetigkeitAssignment> taetigkeiten = new LinkedList<>();
            for (NamiTaetigkeitAssignmentListElement taetigkeitLe : respTaetigkeiten) {
                NamiTaetigkeitAssignment respTaetigkeit = NamiTaetigkeitAssignment
                        .getTaetigkeit(namicon, mgl.getId(),
                                taetigkeitLe.getId());
                if (respTaetigkeit != null) {
                    taetigkeiten.add(respTaetigkeit);
                }
            }

            for (Mailinglist ml : mailinglists) {
                ml.checkAndAddMember(mgl, taetigkeiten);
            }
        }

        // Schreibe Mailinglisten in Ausgabedatei
        for (Mailinglist ml : mailinglists) {
            ml.writeToFile(outputPath);
        }
    }

}
