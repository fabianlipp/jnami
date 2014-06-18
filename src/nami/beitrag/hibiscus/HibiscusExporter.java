package nami.beitrag.hibiscus;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import nami.beitrag.NamiBeitragConfiguration;
import nami.beitrag.db.BeitragLastschrift;
import nami.beitrag.db.BeitragSammelLastschrift;
import nami.beitrag.db.BeitragSepaMandat;
import nami.beitrag.db.DataLastschriftMandat;
import nami.beitrag.db.MandateMapper;
import nami.beitrag.gui.utils.MyStringUtils;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

/**
 * Exportiert Lastschriften und übergibt sie per XML-RPC-Schnittstelle an
 * Hibiscus.
 * 
 * @author Fabian Lipp
 * 
 */
public class HibiscusExporter {
    // URL zur Kontaktaufnahme zu Hibiscus
    private String url;
    // ID (Hibiscus-Intern) des Kontos, für das die Lastschriften eingefügt
    // werden
    private String kontoId;
    // Gläubiger-ID des Zahlungsempfängers
    private String creditorId;
    // Verwendeter Lastschrifttyp (CORE oder COR1)
    private String sepatype;
    // Präfix, das in jeder Mandatsreferenz vor der Mandats-ID eingefügt wird
    private String mrefPrefix;

    private SqlSessionFactory sqlSessionFactory;

    private static Logger logger = Logger.getLogger(HibiscusExporter.class
            .getName());

    // Werden von den statischen Methoden verwendet, um Daten und Geldbeträge zu
    // formatieren
    // Sollten nur synchronized verwendet werden
    private static DateFormat formatter = DateFormat
            .getDateInstance(DateFormat.MEDIUM);
    private static DecimalFormat germanDecimalFormat;

    static {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.GERMAN);
        nf.setGroupingUsed(false);
        germanDecimalFormat = (DecimalFormat) nf;
    }

    /**
     * Erzeugt einen neuen Exporter.
     * 
     * @param conf
     *            Konfiguration der Beitrags-Tools
     * @param sqlSessionFactory
     *            Verbindung zur SQL-Datenbank.
     */
    public HibiscusExporter(NamiBeitragConfiguration conf,
            SqlSessionFactory sqlSessionFactory) {
        creditorId = conf.getSepaCreditorId();
        sepatype = conf.getSepaType();
        mrefPrefix = conf.getSepaMRefPrefix();
        kontoId = conf.getHibiscusKontoId();
        url = conf.getHibiscusUrl();

        this.sqlSessionFactory = sqlSessionFactory;
    }

    /**
     * Übergibt eine einzelne SEPA-Lastschrift an Hibiscus.
     * 
     * @param lastschriftMandat
     *            Daten der Lastschrift (inkl. Betrag) und des Mandats
     * @param faelligkeit
     *            Fälligkeitstermin, zu dem die Lastschrift belastet wird
     * @return <tt>true</tt>, falls die Lastschrift erfolgreich an Hibiscus
     *         übergeben wurde. Andernfalls werden Informationen zum Fehler auf
     *         dem Logger ausgegeben.
     */
    public boolean exportLastschrift(DataLastschriftMandat lastschriftMandat,
            Date faelligkeit) {
        BeitragLastschrift lastschrift = lastschriftMandat.getLastschrift();
        BeitragSepaMandat mandat = lastschriftMandat.getMandat();

        SqlSession session = sqlSessionFactory.openSession();
        try {

            // Client-Config erzeugen
            XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            // config.setBasicPassword(<Master-Passwort>);
            // config.setBasicUserName("admin");
            config.setServerURL(new URL(url));
            config.setEnabledForExtensions(true);
            config.setEncoding("ISO-8859-1");

            XmlRpcClient client = new XmlRpcClient();
            client.setConfig(config);

            // Sequenztyp bestimmen (dazu wird überprüft, ob bereits eine
            // Lastschrift von diesem Mandat gezogen wurde)
            MandateMapper mapper = session.getMapper(MandateMapper.class);
            String sequenceType;
            if (mapper.isMandatUsed(mandat.getMandatId())) {
                sequenceType = "RCUR";
            } else {
                sequenceType = "FRST";
            }

            // Objekt zur Übergabe an Hibiscus vorbereiten
            Map<String, String> params = new HashMap<>();
            params.put("konto", kontoId);
            params.put("termin", formatDate(faelligkeit));
            params.put("blz", mandat.getBic());
            params.put("kontonummer", mandat.getIban());
            params.put("name",
                    MyStringUtils.replaceUmlauts(mandat.getKontoinhaber()));
            params.put("betrag", formatBetrag(lastschrift.getBetrag().negate()));
            params.put("verwendungszweck", MyStringUtils
                    .replaceUmlauts(lastschrift.getVerwendungszweck()));
            params.put("mandateid",
                    mrefPrefix + Integer.toString(mandat.getMandatId()));
            params.put("creditorid", creditorId);
            params.put("sigdate", formatter.format(mandat.getDatum()));
            params.put("sequencetype", sequenceType);
            params.put("sepatype", sepatype);
            params.put("targetdate", formatDate(faelligkeit));

            // Aufruf der RPC-Funktion
            Object result = client.execute(
                    "hibiscus.xmlrpc.sepalastschrift.create",
                    new Object[] { params });

            // Rückgabe überprüfen
            if (result != null) {
                logger.log(
                        Level.WARNING,
                        "Fehler beim Senden an Hibiscus (Mandat-ID "
                                + mandat.getMandatId() + ", Kontoinhaber "
                                + mandat.getKontoinhaber() + "): " + result);
                return false;
            }

            return true;
        } catch (MalformedURLException e) {
            logger.log(Level.SEVERE, "Invalid URL for Hibiscus export", e);
        } catch (XmlRpcException e) {
            logger.log(Level.SEVERE,
                    "Error in XML-RPC call for Hibiscus export", e);
        } finally {
            session.close();
        }

        return false;
    }

    /**
     * Übergibt eine Reihe von Lastschriften als Sammellastschrift an Hibiscus.
     * Möglicherweise entstehen dabei zwei Sammellastschriften, da FRST- und
     * RCUR-Buchungen getrennt werden müssen (in jeder Sammellastschrift kann
     * nur einer der Sequenztypen vorkommen).
     * 
     * @param lastschriften
     *            zu exportierende Lastschriften
     * @param sl
     *            Sammellastschrift (aus dieser werden Fälligkeitsdatum und
     *            Bezeichnung entnommen)
     * @return <tt>true</tt>, falls alle Lastschriften erfolgreich an Hibiscus
     *         übergeben wurden. Andernfalls werden Informationen zum Fehler auf
     *         dem Logger ausgegeben.
     */
    public boolean exportSammellastschrift(
            List<DataLastschriftMandat> lastschriften,
            BeitragSammelLastschrift sl) {

        // Gibt an, ob bisher alle Sammellastschriften erfolgreich an Hibiscus
        // übergeben wurden
        boolean allSuccessful = true;

        SqlSession session = sqlSessionFactory.openSession();
        try {
            // Client-Config erzeugen
            XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            // config.setBasicPassword(<Master-Passwort>);
            // config.setBasicUserName("admin");
            config.setServerURL(new URL(url));
            config.setEnabledForExtensions(true);
            config.setEncoding("ISO-8859-1");

            XmlRpcClient client = new XmlRpcClient();
            client.setConfig(config);

            List<Map<String, Object>> buchungenRcur = new LinkedList<>();
            List<Map<String, Object>> buchungenFrst = new LinkedList<>();

            MandateMapper mapper = session.getMapper(MandateMapper.class);
            for (DataLastschriftMandat ls : lastschriften) {
                BeitragLastschrift lastschrift = ls.getLastschrift();
                BeitragSepaMandat mandat = ls.getMandat();
                Map<String, Object> params = new HashMap<>();
                params.put("blz", mandat.getBic());
                params.put("kontonummer", mandat.getIban());
                params.put("name",
                        MyStringUtils.replaceUmlauts(mandat.getKontoinhaber()));
                params.put("betrag", formatBetrag(lastschrift.getBetrag()
                        .negate()));
                params.put("verwendungszweck", MyStringUtils
                        .replaceUmlauts(lastschrift.getVerwendungszweck()));
                params.put("mandateid",
                        mrefPrefix + Integer.toString(mandat.getMandatId()));
                params.put("creditorid", creditorId);
                params.put("sigdate", formatDate(mandat.getDatum()));

                // Sequenztyp bestimmen (dazu wird überprüft, ob bereits eine
                // Lastschrift von diesem Mandat gezogen wurde)
                if (mapper.isMandatUsed(ls.getMandat().getMandatId())) {
                    buchungenRcur.add(params);
                } else {
                    buchungenFrst.add(params);
                }
            }

            // Objekt zur Übergabe an Hibiscus vorbereiten
            Map<String, Object> params = new HashMap<>();
            params.put("konto", kontoId);
            params.put("termin", formatDate(sl.getFaelligkeit()));
            params.put("sepatype", sepatype);
            params.put("targetdate", formatDate(sl.getFaelligkeit()));

            if (!buchungenFrst.isEmpty()) {
                params.put("name", "FRST - " + sl.getBezeichnung());
                params.put("sequencetype", "FRST");
                params.put("buchungen", buchungenFrst);

                // Aufruf der RPC-Funktion
                Object result = client.execute(
                        "hibiscus.xmlrpc.sepasammellastschrift.create",
                        new Object[] { params });

                // Rückgabe überprüfen
                if (result != null) {
                    logger.log(Level.WARNING,
                            "Fehler beim Senden an Hibiscus (FRST): " + result);
                    allSuccessful = false;
                }
            }

            if (!buchungenRcur.isEmpty()) {
                params.put("name", "RCUR - " + sl.getBezeichnung());
                params.put("sequencetype", "RCUR");
                params.put("buchungen", buchungenRcur);

                // Aufruf der RPC-Funktion
                Object result = client.execute(
                        "hibiscus.xmlrpc.sepasammellastschrift.create",
                        new Object[] { params });

                // Rückgabe überprüfen
                if (result != null) {
                    logger.log(Level.WARNING,
                            "Fehler beim Senden an Hibiscus (RCUR): " + result);
                    allSuccessful = false;
                }
            }

            return allSuccessful;
        } catch (MalformedURLException e) {
            logger.log(Level.SEVERE, "Invalid URL for Hibiscus export", e);
        } catch (XmlRpcException e) {
            logger.log(Level.SEVERE,
                    "Error in XML-RPC call for Hibiscus export", e);
        } finally {
            session.close();
        }
        return false;
    }

    /**
     * Formatiert das übergebene Datum so, wie es Hibiscus erwartet.
     * 
     * @param date
     *            zu formatierendes Datum
     * @return formatiertes Datum
     */
    private static synchronized String formatDate(Date date) {
        return formatter.format(date);
    }

    /**
     * Formatiert den übergebenen Betrag so, wie ihn Hibiscus erwartet. Das
     * heißt mit einem Komma als Dezimaltrennzeichen und ohne
     * Zifferngruppierung.
     * 
     * @param betrag
     *            zu formatierender Betrag
     * @return formatierter Betrag
     */
    private static synchronized String formatBetrag(BigDecimal betrag) {
        return germanDecimalFormat.format(betrag);
    }
}
