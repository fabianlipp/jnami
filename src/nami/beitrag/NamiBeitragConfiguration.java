package nami.beitrag;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import lombok.Getter;
import nami.configuration.ConfigFormatException;
import nami.connector.Beitragsart;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaderJDOMFactory;
import org.jdom2.input.sax.XMLReaderXSDFactory;

/**
 * Lädt die Konfiguration für das Nami-Beitrags-Tool.
 * 
 * @author Fabian Lipp
 * 
 */
@Getter
public class NamiBeitragConfiguration {
    private String gruppierungsnummer;
    private Properties databaseConfig;
    private Map<Beitragsart, BigDecimal> beitragssaetze;

    private String sepaCreditorId;
    private String sepaType;
    private String sepaMRefPrefix;
    private String hibiscusUrl;
    private String hibiscusKontoId;

    private String pdfViewer;
    private String letterOutputPath;

    private static Logger log = Logger.getLogger(NamiBeitragConfiguration.class
            .getName());
    private static final URL XSDFILE = NamiBeitragConfiguration.class
            .getResource("namibeitrag.xsd");

    /**
     * Lädt die Konfiguration aus der übergebenen Konfigurationsdatei.
     * 
     * @param configFile
     *            Konfigurationsdatei im XML-Format
     * @throws ConfigFormatException
     *             wenn ein Fehler beim Lesen oder Parsen der
     *             Konfigurationsdatei auftritt
     */
    public NamiBeitragConfiguration(File configFile)
            throws ConfigFormatException {
        // Lese Konfigurationsdatei ein (inkl. Validierung)
        Document doc;
        try {
            XMLReaderJDOMFactory schemafac = new XMLReaderXSDFactory(XSDFILE);
            SAXBuilder builder = new SAXBuilder(schemafac);

            log.info("Using Beitrag config file: "
                    + configFile.getAbsolutePath());
            if (!configFile.exists() || !configFile.canRead()) {
                throw new ConfigFormatException("Cannot read config file");
            }
            doc = builder.build(configFile);
        } catch (JDOMException e) {
            throw new ConfigFormatException("Could not parse config file", e);
        } catch (IOException e) {
            throw new ConfigFormatException("Could not read config file", e);
        }

        // Parse Konfiguration aus XML
        Element namibeitragEl = doc.getRootElement();
        if (namibeitragEl.getName() != "namibeitrag") {
            throw new ConfigFormatException("Wrong root element in config file");
        }

        gruppierungsnummer = namibeitragEl
                .getAttributeValue("gruppierungsnummer");

        // Datenbankverbindung aus XML lesen
        Element databaseEl = namibeitragEl.getChild("database");
        databaseConfig = new Properties();
        databaseConfig.setProperty("driver", databaseEl.getChildText("driver"));
        databaseConfig.setProperty("url", databaseEl.getChildText("url"));
        databaseConfig.setProperty("username",
                databaseEl.getChildText("username"));
        databaseConfig.setProperty("password",
                databaseEl.getChildText("password"));

        // Beitragssätze aus XML einlesen
        Element beitragssaetzeEl = namibeitragEl.getChild("beitragssaetze");
        beitragssaetze = new HashMap<>();
        for (Element satzEl : beitragssaetzeEl.getChildren("beitragssatz")) {
            String typStr = satzEl.getAttributeValue("typ");
            Beitragsart typ = Beitragsart.fromString(typStr);
            String betragStr = satzEl.getAttributeValue("betrag");
            BigDecimal betrag = new BigDecimal(betragStr);

            beitragssaetze.put(typ, betrag);
        }

        // SEPA-Parameter einlesen
        Element sepaEl = namibeitragEl.getChild("sepa");
        sepaCreditorId = sepaEl.getChildText("creditorId");
        sepaType = sepaEl.getChildText("lastschriftType");
        if (sepaType == null || sepaType.isEmpty()) {
            sepaType = "CORE";
        }
        sepaMRefPrefix = sepaEl.getChildText("mrefPrefix");
        if (sepaMRefPrefix == null) {
            sepaMRefPrefix = "";
        }
        hibiscusUrl = sepaEl.getChildText("hibiscusUrl");
        hibiscusKontoId = sepaEl.getChildText("hibiscusKontoId");

        pdfViewer = namibeitragEl.getChildText("pdfViewer");
        letterOutputPath = namibeitragEl.getChildText("letterOutputPath");
    }
}
