package nami.statistics;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;
import java.util.logging.Logger;

import nami.configuration.ConfigFormatException;
import nami.connector.Geschlecht;
import nami.connector.namitypes.NamiSearchedValues;
import nami.db.schema.SchemaUpdater;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaderJDOMFactory;
import org.jdom2.input.sax.XMLReaderXSDFactory;

/**
 * Stellt die Konfiguration für das Statistik-Tool dar. Diese Konfiguration wird
 * aus einer XML-Datei eingelesen.
 * 
 * @author Fabian Lipp
 * 
 */
public class StatisticsConfig {
    private static Logger log = Logger.getLogger(StatisticsConfig.class
            .getName());

    private StatisticsDatabase db = null;
    private Collection<Gruppe> gruppen = null;

    /**
     * Wurzel-Gruppierung, für die die Statistik erstellt wird. Wenn die
     * Wurzel-Gruppierung <tt>null</tt> ist, wird der gesamte verfügbare
     * Gruppierungsbaum ausgewertet.
     */
    private String rootGrp = null;

    private static final URL XSDFILE = NamiStatistics.class
            .getResource("namistatistics.xsd");
    private static final String MYBATIS_CONFIGFILE = "mybatis-config.xml";

    /**
     * Lädt die Konfiguration aus einer XML-Datei.
     * 
     * @param configFile
     *            Konfigurationsdatei
     * @throws ConfigFormatException
     *             Fehler in der Konfigurationsdatei (z. B. nicht valides XML)
     * @throws IOException
     *             Probleme beim Lesen der Konfigurationsdatei
     */
    public StatisticsConfig(File configFile) throws ConfigFormatException,
            IOException {
        // check if config already parsed
        if (db != null && gruppen != null) {
            return;
        }

        // Lese Konfigurationsdatei ein (inkl. Validierung)
        Document doc;
        try {
            XMLReaderJDOMFactory schemafac = new XMLReaderXSDFactory(XSDFILE);
            SAXBuilder builder = new SAXBuilder(schemafac);

            log.info("Using statistics config file: "
                    + configFile.getAbsolutePath());
            if (!configFile.exists() || !configFile.canRead()) {
                throw new IOException("Cannot read config file");
            }
            doc = builder.build(configFile);
        } catch (JDOMException e) {
            throw new ConfigFormatException("Could not parse config file", e);
        }

        // Parse Konfiguration aus XML
        Element namistatisticsEl = doc.getRootElement();
        if (namistatisticsEl.getName() != "namistatistics") {
            throw new ConfigFormatException("Wrong root element in config file");
        }
        rootGrp = namistatisticsEl.getAttributeValue("root");

        // Datenbankverbindung aus XML lesen
        Element databaseEl = namistatisticsEl.getChild("database");
        String dbDriver = databaseEl.getChildText("driver");
        String dbUrl = databaseEl.getChildText("url");
        String dbUsername = databaseEl.getChildText("username");
        String dbPassword = databaseEl.getChildText("password");

        // Gruppen aus XML einlesen
        gruppen = new LinkedList<>();
        TreeSet<Integer> gruppenIds = new TreeSet<>();
        for (Element gruppeEl : namistatisticsEl.getChildren("gruppe")) {
            // ID der Gruppe
            int gruppeId = Integer.parseInt(gruppeEl.getAttributeValue("id"));
            if (gruppenIds.contains(gruppeId)) {
                throw new ConfigFormatException("Duplicate ID in config file: "
                        + gruppeId);
            }
            gruppenIds.add(gruppeId);

            // Gruppenbezeichnung
            String bezeichnung = gruppeEl.getAttributeValue("bezeichnung");

            // Suchausdrücke
            List<NamiSearchedValues> searches = new LinkedList<>();
            for (Element namiSearchEl : gruppeEl.getChildren("namiSearch")) {
                searches.add(NamiSearchedValues.fromXml(namiSearchEl));
            }

            // Filter
            Element filterEl;
            List<Filter> filters = new LinkedList<>();
            filterEl = gruppeEl.getChild("geschlechtFilter");
            if (filterEl != null) {
                String value = filterEl.getAttributeValue("value");
                filters.add(new GeschlechtFilter(Geschlecht.fromString(value)));
            }

            gruppen.add(new Gruppe(gruppeId, bezeichnung, searches, filters));
        }

        // Update Database Schema
        SchemaUpdater updater = new SchemaUpdater(dbDriver, dbUrl, dbUsername,
                dbPassword);
        updater.update("statistics");

        // Initialise MyBatis
        Properties prop = new Properties();
        prop.setProperty("driver", dbDriver);
        prop.setProperty("url", dbUrl);
        prop.setProperty("username", dbUsername);
        prop.setProperty("password", dbPassword);
        InputStream is = NamiStatistics.class
                .getResourceAsStream(MYBATIS_CONFIGFILE);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder()
                .build(is, prop);

        db = new StatisticsDatabase(gruppen, sqlSessionFactory);
    }

    /**
     * Liefert die in der Konfiguration spezifizierte Datenbank-Verbindung.
     * 
     * @return Datenbank-Zugang
     */
    public StatisticsDatabase getDb() {
        return db;
    }

    /**
     * Liefert die in der Konfiguration beschriebenen Gruppen.
     * 
     * @return Gruppen
     */
    public Collection<Gruppe> getGruppen() {
        return gruppen;
    }

    /**
     * Wurzel-Gruppierung, für die die Statistik erstellt wird. Wenn die
     * Wurzel-Gruppierung <tt>null</tt> ist, wird der gesamte verfügbare
     * Gruppierungsbaum ausgewertet.
     */
    /**
     * Liefert die in der Konfigurationsdatei vorgegebene Wurzel-Gruppierung,
     * von der aus die Statistik erstellt wird.
     * 
     * @return Wurzel-Gruppierung aus der Konfigurationsdatei; <tt>null</tt>,
     *         falls keine festgelegt ist
     */
    public String getRootGrp() {
        return rootGrp;
    }
}
