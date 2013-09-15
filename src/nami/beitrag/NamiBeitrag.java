package nami.beitrag;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import jline.console.completer.Completer;
import nami.beitrag.db.BeitragBuchung;
import nami.beitrag.db.BeitragMapper;
import nami.beitrag.db.BeitragMitglied;
import nami.beitrag.db.BeitragZeitraum;
import nami.cli.CliParser;
import nami.cli.CompleterFactory;
import nami.cli.annotation.AlternateCommands;
import nami.cli.annotation.CliCommand;
import nami.cli.annotation.ParamCompleter;
import nami.cli.annotation.ParentCommand;
import nami.configuration.ApplicationDirectoryException;
import nami.configuration.ConfigFormatException;
import nami.configuration.Configuration;
import nami.connector.NamiConnector;
import nami.connector.NamiServer;
import nami.connector.credentials.NamiCredentials;
import nami.connector.exception.NamiApiException;
import nami.connector.namitypes.NamiBeitragszahlung;
import nami.connector.namitypes.NamiMitgliedListElement;
import nami.connector.namitypes.NamiSearchedValues;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

/**
 * Liest Mitgliederzahlen aus NaMi aus und erstellt Statistiken daraus.
 * 
 * @author Fabian Lipp
 * 
 */
public final class NamiBeitrag {
    private NamiBeitrag() {
    }

    private static SqlSessionFactory sqlSessionFactory;

    // TODO: Aus Konfigurationsdatei lesen
    private static final String GRUPPIERUNGS_NUMMER = "220309";

    private static Logger log = Logger.getLogger(NamiBeitrag.class.getName());
    private static CliParser parser = new CliParser(NamiBeitrag.class,
            "beitrag");

    private static final String MYBATIS_CONFIGFILE = "db/mybatis-config.xml";

    static {
        // TODO: Datenbank aus Konfigurationsdatei lesen
        final String dbDriver = "com.mysql.jdbc.Driver";
        final String dbUrl = "jdbc:mysql://localhost:3306/batistest";
        final String dbUsername = "batistest";
        final String dbPassword = "batistest";

        // TODO: Liquibase verwenden
        // Update Database Schema
        // SchemaUpdater updater = new SchemaUpdater(dbDriver, dbUrl,
        // dbUsername, dbPassword);
        // updater.update();

        // Initialise MyBatis
        Properties prop = new Properties();
        prop.setProperty("driver", dbDriver);
        prop.setProperty("url", dbUrl);
        prop.setProperty("username", dbUsername);
        prop.setProperty("password", dbPassword);
        InputStream is = NamiBeitrag.class
                .getResourceAsStream(MYBATIS_CONFIGFILE);
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(is, prop);
    }

    /**
     * Main-Funktion, wenn diese Klasse direkt von der Kommandozeile aufgerufen
     * wird.
     * 
     * @param args
     *            Kommandozeilen-Argumente
     * @throws Exception .
     */
    public static void main(String[] args) throws Exception {
        try {
            Properties p = Configuration.getGeneralProperties();

            NamiCredentials credentials = NamiCredentials
                    .getCredentialsFromProperties(p);

            NamiConnector con;
            if (Boolean.parseBoolean(p.getProperty("nami.useApi"))) {
                con = new NamiConnector(NamiServer.LIVESERVER_WITH_API,
                        credentials);
            } else {
                con = new NamiConnector(NamiServer.LIVESERVER, credentials);
            }

            PrintWriter out = new PrintWriter(System.out);
            beitrag(args, con, out);
            out.close();
            System.exit(0);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Liefert den Completer, der die Kommandos vervollständigt, die
     * NamiStatistics versteht.
     * 
     */
    public static class BeitragCompleter implements CompleterFactory {
        @Override
        public Completer getCompleter() {
            return parser.getCompleter();
        }
    }

    /**
     * Haupt-Funktion, wenn das Statistik-Tool durch NamiCli aufgerufen wird.
     * 
     * @param args
     *            nicht verwendet
     * @param namicon
     *            Verbindung zum NaMi-Server
     * @param out
     *            Writer, auf dem die Ausgabe erfolgt
     * @throws IOException
     *             IOException
     * @throws ConfigFormatException
     *             Fehler in der Konfigurationsdatei
     * @throws SQLException
     *             Fehler bei einer SQL-Anfrage
     * @throws ApplicationDirectoryException
     *             Probleme beim Zugriff auf das Konfigurationsverzeichnis
     */
    @CliCommand("beitrag")
    @ParamCompleter(BeitragCompleter.class)
    public static void beitrag(String[] args, NamiConnector namicon,
            PrintWriter out) throws IOException, ConfigFormatException,
            SQLException, ApplicationDirectoryException {

        // Logger dbLogger =
        // Logger.getLogger(StatisticsDatabase.class.getName());
        // dbLogger.setLevel(Level.FINEST);
        Handler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        // Logger.getLogger("").addHandler(handler);
        // Logger.getLogger("").setLevel(Level.ALL);

        parser.callMethod(args, namicon, out);
    }

    /**
     * Bringt die lokale Mitgliederdatenbank auf den aktuellen Stand. Dazu
     * werden folgende Schritte ausgeführt:
     * <ul>
     * <li>in NaMi neu angelegte Mitglieder in die lokale Datenbank einfügen</li>
     * <li>in NaMi geänderte Mitglieder (erkennbar an der Versionsnummer) in der
     * lokalen Datenbank aktualisieren</li>
     * <li>in NaMi gelöschte Mitglieder in der lokalen Datenbank als gelöscht
     * markieren</li>
     * </ul>
     * Es werden keine Daten aus der lokalen Datenbank nach NaMi übertragen.
     * 
     * @param args
     *            nicht verwendet
     * @param namicon
     *            Verbindung zum NaMi-Server
     * @param out
     *            Writer, auf dem die Ausgabe erfolgt
     * @throws IOException
     *             IOException
     * @throws NamiApiException
     *             Fehler bei einer Anfrage an NaMi
     */
    @CliCommand("syncMitglieder")
    @AlternateCommands("sync")
    @ParentCommand("beitrag")
    public static void syncMitglieder(String[] args, NamiConnector namicon,
            PrintWriter out) throws NamiApiException, IOException {
        SqlSession session = sqlSessionFactory.openSession();
        try {
            BeitragMapper mapper = session.getMapper(BeitragMapper.class);

            Set<Integer> localMglIds = mapper.getMitgliedIds();

            namicon.namiLogin();
            Collection<NamiMitgliedListElement> mitgliederFromNami = NamiSearchedValues
                    .withStammgruppierung(GRUPPIERUNGS_NUMMER).getAllResults(
                            namicon);

            // DEBUG: nehme nur 10 Mitglieder aus Nami
            Collection<NamiMitgliedListElement> tmp = new LinkedList<>();
            Iterator<NamiMitgliedListElement> iter = mitgliederFromNami
                    .iterator();
            for (int i = 0; i <= 9; i++) {
                if (iter.hasNext()) {
                    tmp.add(iter.next());
                }
            }
            mitgliederFromNami = tmp;

            for (NamiMitgliedListElement namiMgl : mitgliederFromNami) {
                BeitragMitglied beitMgl = mapper.getMitglied(namiMgl.getId());
                if (beitMgl != null) {
                    // Mitglied existiert bereits lokal
                    if (namiMgl.getVersion() > beitMgl.getVersion()) {
                        // in Nami geupdated
                        beitMgl.updateFromNami(namiMgl.getFullData(namicon));
                        log.log(Level.INFO,
                                "Updating MitgliedID {0,number,#} in local database",
                                beitMgl.getMitgliedId());
                        mapper.updateMitglied(beitMgl);
                    }
                } else {
                    // Mitglied existiert noch nicht lokal
                    beitMgl = new BeitragMitglied(namiMgl.getFullData(namicon));
                    log.log(Level.INFO,
                            "Inserting MitgliedID {0,number,#} into local database",
                            beitMgl.getMitgliedId());
                    mapper.insertMitglied(beitMgl);
                }
                localMglIds.remove(namiMgl.getId());
            }

            // IDs, die jetzt noch in localMglIds sind, existieren nur lokal
            // aber nicht in Nami
            for (int mglId : localMglIds) {
                if (!mapper.isDeleted(mglId)) {
                    log.log(Level.INFO,
                            "MitgliedID {0,number,#} does not exist in NaMi; "
                                    + "marking as deleted in local database",
                            mglId);
                    mapper.setDeleted(mglId);
                }
            }

            session.commit();
        } finally {
            session.close();
        }
    }

    /**
     * Holt die Beitragszahlungen aller Mitglieder ab und fügt sie in die lokale
     * Datenbank ein, falls sie noch nicht vorhanden sind.
     * 
     * @param args
     *            nicht verwendet
     * @param con
     *            Verbindung zum NaMi-Server
     * @param out
     *            Writer, auf dem die Ausgabe erfolgt
     * @throws IOException
     *             IOException
     * @throws NamiApiException
     *             Fehler bei einer Anfrage an NaMi
     */
    @CliCommand("fetchBeitragszahlungen")
    @ParentCommand("beitrag")
    public static void fetchBeitragszahlungen(String[] args, NamiConnector con,
            PrintWriter out) throws NamiApiException, IOException {
        SqlSession session = sqlSessionFactory.openSession();
        try {
            BeitragMapper mapper = session.getMapper(BeitragMapper.class);
            con.namiLogin();

            Set<Integer> localMglIds = mapper.getMitgliedIds();
            for (int mglId : localMglIds) {
                fetchBeitragszahlungen(con, mglId, mapper);
            }

            session.commit();
        } finally {
            session.close();
        }
    }

    private static void fetchBeitragszahlungen(NamiConnector con,
            int mitgliedId, BeitragMapper mapper) throws NamiApiException,
            IOException {
        Collection<NamiBeitragszahlung> zahlungen = NamiBeitragszahlung
                .getBeitragszahlungen(con, mitgliedId);
        for (NamiBeitragszahlung zahlung : zahlungen) {
            BeitragBuchung buchung = mapper.getBuchungByNamiId(zahlung.getId());
            if (buchung == null) {
                // Buchung existiert noch nicht lokal -> einfügen
                buchung = new BeitragBuchung(mitgliedId, zahlung);
                log.log(Level.INFO,
                        "Inserting BuchungID {0,number,#} for MitgliedID "
                                + "{1,number,#} into local database",
                        new Object[] { zahlung.getId(), mitgliedId });

                BeitragZeitraum zeitr = buchung.getZeitraum();
                if (mapper.getZeitraum(zeitr) == null) {
                    mapper.insertZeitraum(zeitr);
                }

                mapper.insertBuchung(buchung);
            }
        }
    }

}
