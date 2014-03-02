package nami.beitrag.hibiscus;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import nami.beitrag.db.BeitragLastschrift;
import nami.beitrag.db.BeitragSepaMandat;
import nami.beitrag.db.MandateMapper;

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
    // TODO: Aus Konfigurationsdatei lesen
    // URL zur Kontaktaufnahme zu Hibiscus
    private String url = "http://localhost:8080/xmlrpc/";
    // ID (Hibiscus-Intern) des Kontos, für das die Lastschriften eingefügt
    // werden
    private String kontoId = "1";
    // Gläubiger-ID des Zahlungsempfängers
    private String creditorId = "CREDID";
    // Verwendeter Lastschrifttyp (CORE oder COR1)
    private String sepatype = "CORE";
    // Präfix, das in jeder Mandatsreferenz vor der Mandats-ID eingefügt wird
    private String mrefPrefix = "M";

    private SqlSessionFactory sqlSessionFactory;

    private static Logger logger = Logger.getLogger(HibiscusExporter.class
            .getName());

    /**
     * Erzeugt einen neuen Exporter.
     * 
     * @param sqlSessionFactory
     *            Verbindung zur SQL-Datenbank.
     */
    public HibiscusExporter(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    /**
     * Übergibt eine einzelne SEPA-Lastschrift an Hibiscus.
     * 
     * @param lastschrift
     *            Daten der Lastschrift (inkl. Betrag)
     * @param mandat
     *            Mandatsinformationen
     * @param faelligkeit
     *            Fälligkeitstermin, zu dem die Lastschrift belastet wird
     * @return <tt>true</tt>, falls die Lastschrift erfolgreich an Hibiscus
     *         übergeben wurde. Andernfalls werden Informationen zum Fehler auf
     *         dem Logger ausgegeben.
     */
    public boolean exportLastschrift(BeitragLastschrift lastschrift,
            BeitragSepaMandat mandat, Date faelligkeit) {
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
            DateFormat formatter = DateFormat
                    .getDateInstance(DateFormat.MEDIUM);
            Map<String, String> params = new HashMap<>();
            params.put("konto", kontoId);
            params.put("termin", formatter.format(faelligkeit));
            params.put("blz", mandat.getBic());
            params.put("kontonummer", mandat.getIban());
            params.put("name", mandat.getKontoinhaber());
            params.put("betrag", lastschrift.getBetrag().toPlainString());
            params.put("verwendungszweck", lastschrift.getVerwendungszweck());
            params.put("mandateid",
                    mrefPrefix + Integer.toString(mandat.getMandatId()));
            params.put("creditorid", creditorId);
            params.put("sigdate", formatter.format(mandat.getDatum()));
            params.put("sequencetype", sequenceType);
            params.put("sepatype", sepatype);
            params.put("targetdate", formatter.format(faelligkeit));

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

}
