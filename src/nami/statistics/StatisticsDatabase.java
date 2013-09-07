package nami.statistics;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import nami.connector.Ebene;
import nami.connector.namitypes.NamiGruppierung;

import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

/**
 * Diese Klasse abstrahiert die Zugriffe auf die Datenbank. Durch die Methoden
 * können die benötigten SQL-Befehle ausgeführt werden.
 * 
 * @author Fabian Lipp
 * 
 */
public class StatisticsDatabase {
    private SqlSessionFactory sqlSessionFactory;
    private Collection<Gruppe> gruppen;
    private static Logger log = Logger.getLogger(StatisticsDatabase.class
            .getName());

    /**
     * Erzeugt ein Objekt zum Datenbankzugriff.
     * 
     * @param gruppen
     *            Gruppen, die in der Statistik erfasst werden
     * @param sqlSessionFactory
     *            die Factory aus der die SQL-Sessions erzeugt werden sollen
     * @throws SQLException
     *             Probleme beim Ausführen der SQL-Kommandos
     */
    public StatisticsDatabase(Collection<Gruppe> gruppen,
            SqlSessionFactory sqlSessionFactory) throws SQLException {
        this.gruppen = gruppen;
        this.sqlSessionFactory = sqlSessionFactory;
    }

    /**
     * Bereitet die Datenbank vor. Dazu werden alle benötigten Tabellen angelegt
     * (falls noch nicht vorhanden). Anschließend werden die verfügbaren
     * Gruppierungen und Gruppen in die entsprechenden Tabellen eingetragen.
     * 
     * @param rootGruppierung
     *            Wurzel des Gruppierungsbaums (die untergeordneten
     *            Gruppierungen müssen in den entsprechenden Feldern enthalten
     *            sein.
     * @throws SQLException
     *             Probleme beim Ausführen der SQL-Kommandos
     */
    public void createDatabase(NamiGruppierung rootGruppierung)
            throws SQLException {

        SqlSession session = sqlSessionFactory.openSession();
        try {
            StatisticsMapper mapper = session.getMapper(StatisticsMapper.class);

            // Erzeuge Tabellen
            mapper.createTableGruppierung();
            mapper.createTableStatisticsGruppe();
            mapper.createTableStatisticsRun();
            mapper.createTableStatisticsData();

            session.commit();
        } catch (Exception e) {
            log.log(Level.SEVERE, "SQL call throws exception", e);
        } finally {
            session.close();
        }

        // Füge Gruppierungen in Datenbank ein, falls noch nicht vorhanden
        writeGruppierungToDb(rootGruppierung);

        // Füge erfasste Gruppen in Datenbank ein, falls noch nicht vorhanden
        for (Gruppe gruppe : gruppen) {
            writeGruppeToDb(gruppe.getId(), gruppe.getBezeichnung());
        }
    }

    private void writeGruppierungToDb(NamiGruppierung grp) throws SQLException {
        writeGruppierungToDb(grp.getId(), grp.getDescriptor(),
                grp.getParentId(Ebene.DIOEZESE), grp.getParentId(Ebene.BEZIRK));

        for (NamiGruppierung child : grp.getChildren()) {
            writeGruppierungToDb(child);
        }
    }

    private void writeGruppierungToDb(String id, String descriptor,
            String dioezese, String bezirk) throws SQLException {

        SqlSession session = sqlSessionFactory.openSession();
        try {
            StatisticsMapper mapper = session.getMapper(StatisticsMapper.class);

            int count = mapper.checkForGruppierung(id);
            if (count == 0) {
                // Gruppierung ist noch nicht in Datenbank
                mapper.insertGruppierung(id, descriptor, dioezese, bezirk);
            }
            session.commit();

        } catch (Exception e) {
            log.log(Level.SEVERE, "SQL call throws exception", e);
        } finally {
            session.close();
        }
    }

    private void writeGruppeToDb(int id, String bezeichnung)
            throws SQLException {

        SqlSession session = sqlSessionFactory.openSession();
        try {
            StatisticsMapper mapper = session.getMapper(StatisticsMapper.class);

            int count = mapper.checkForGruppe(id);
            if (count == 0) {
                // Gruppierung ist noch nicht in Datenbank
                mapper.insertGruppe(id, bezeichnung);
            }
            session.commit();

        } catch (Exception e) {
            log.log(Level.SEVERE, "SQL call throws exception", e);
        } finally {
            session.close();
        }
    }

    /**
     * Legt einen neuen Run (also Statistik-Abruf) mit dem aktuellen Datum in
     * der Datenbank an.
     * 
     * @return ID des neu angelegten Runs
     * @throws SQLException
     *             Probleme beim Ausführen der SQL-Kommandos
     */
    public long writeNewStatisticRun() throws SQLException {
        SqlSession session = sqlSessionFactory.openSession();
        try {
            StatisticsMapper mapper = session.getMapper(StatisticsMapper.class);

            Map<String, Long> map = new HashMap<>();
            mapper.newRun(map);
            session.commit();

            Long res = map.get("runId");
            if (res != null) {
                return res;
            } else {
                return -1;
            }

        } catch (Exception e) {
            log.log(Level.SEVERE, "SQL call throws exception", e);
        } finally {
            session.close();
        }

        return -1;
    }

    /**
     * Schreibt die Anzahl gefundener Mitglieder in die Datenbank.
     * 
     * @param gruppierungsnummer
     *            Gruppierungsnummer
     * @param gruppeId
     *            ID der Gruppe
     * @param runId
     *            ID des Runs, in dem die Anzahl abgefragt wurde
     * @param anzahl
     *            Anzahl gefundener Mitglieder
     * @throws SQLException
     *             Probleme beim Ausführen der SQL-Kommandos
     */
    public void writeAnzahl(String gruppierungsnummer, int gruppeId, long runId,
            int anzahl) throws SQLException {
        SqlSession session = sqlSessionFactory.openSession();
        try {
            StatisticsMapper mapper = session.getMapper(StatisticsMapper.class);
            mapper.insertAnzahl(gruppierungsnummer, gruppeId, runId, anzahl);
            session.commit();
        } catch (Exception e) {
            log.log(Level.SEVERE, "SQL call throws exception", e);
        } finally {
            session.close();
        }
    }

    /**
     * Beschreibt einen Run, das heißt die Mitgliederzahlen zu einem bestimmten
     * Zeitpunkt.
     */
    public static class Run {
        private int runId;
        private String datum;

        /**
         * Liefert die ID.
         * 
         * @return .
         */
        public int getRunId() {
            return runId;
        }

        /**
         * Liefert den Zeitpunkt, an dem der Run ausgeführt wurde.
         * 
         * @return .
         */
        public String getDatum() {
            return datum;
        }
    }

    /**
     * Liefert die durchgeführten Runs aus der Datenbank.
     * 
     * @return Runs mit ID und Datum
     * @throws SQLException
     *             Probleme beim Ausführen der SQL-Kommandos
     */
    public List<Run> getRuns() throws SQLException {
        SqlSession session = sqlSessionFactory.openSession();
        try {
            StatisticsMapper mapper = session.getMapper(StatisticsMapper.class);
            return mapper.getRuns();
        } catch (Exception e) {
            log.log(Level.SEVERE, "SQL call throws exception", e);
        } finally {
            session.close();
        }
        return new LinkedList<>();
    }

    /**
     * Liefert die ID des zuletzt durchgeführten Runs.
     * 
     * @return ID des letzten Runs
     * @throws SQLException
     *             Probleme beim Ausführen der SQL-Kommandos
     */
    public int getLatestRunId() throws SQLException {
        SqlSession session = sqlSessionFactory.openSession();
        try {
            StatisticsMapper mapper = session.getMapper(StatisticsMapper.class);
            Integer res = mapper.getLatestRunId();
            if (res != null) {
                return res;
            } else {
                return -1;
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "SQL call throws exception", e);
        } finally {
            session.close();
        }
        return -1;
    }

    /**
     * Fragt die Mitgliederzahlen aller sichtbaren Gruppierungen im zuletzt
     * ausgeführten Run aus der Datenbank ab und übergibt sie an einen
     * <tt>ResultHandler</tt>.
     * 
     * @param cumulate
     *            falls <tt>true</tt> werden zu den Mitgliederzahlen einer
     *            Gruppierung alle untergeordneten Gruppierungen aufsummiert
     * @param handler
     *            Aktion, die für jede zurückgegebene Zeile ausgeführt wird
     * @throws SQLException
     *             Probleme beim Ausführen der SQL-Kommandos
     */
    public void getStatsAllGruppierungen(boolean cumulate, ResultHandler handler)
            throws SQLException {
        getStatsAllGruppierungen(getLatestRunId(), cumulate, handler);
    }

    /**
     * Fragt die Mitgliederzahlen aller sichtbaren Gruppierungen in einem
     * vorgegebenen Run aus der Datenbank ab und übergibt sie an einen
     * <tt>ResultHandler</tt>.
     * 
     * 
     * @param runId
     *            ID des Runs dessen Mitgliederzahlen ausgegeben werden sollen
     * @param cumulate
     *            falls <tt>true</tt> werden zu den Mitgliederzahlen einer
     *            Gruppierung alle untergeordneten Gruppierungen aufsummiert
     * @param handler
     *            Aktion, die für jede zurückgegebene Zeile ausgeführt wird
     * @throws SQLException
     *             Probleme beim Ausführen der SQL-Kommandos
     */
    public void getStatsAllGruppierungen(int runId, boolean cumulate,
            ResultHandler handler) throws SQLException {
        SqlSession session = sqlSessionFactory.openSession();
        try {
            StatisticsMapper mapper = session.getMapper(StatisticsMapper.class);
            mapper.getStatsAllGruppierungen(runId, cumulate, gruppen, handler);
        } catch (Exception e) {
            log.log(Level.SEVERE, "SQL call throws exception", e);
        } finally {
            session.close();
        }
    }

    /**
     * Fragt alle vorhandenen Mitgliederzahlen einer Gruppierung aus der
     * Datenbank ab und übergibt sie an einen <tt>ResultHandler</tt>.
     * 
     * @param gruppierungId
     *            Gruppierungsnummer
     * @param cumulate
     *            falls <tt>true</tt> werden zu den Mitgliederzahlen einer
     *            Gruppierung alle untergeordneten Gruppierungen aufsummiert
     * @param handler
     *            Aktion, die für jede zurückgegebene Zeile ausgeführt wird
     * @throws SQLException
     *             Probleme beim Ausführen der SQL-Kommandos
     */
    public void getHistory(String gruppierungId, boolean cumulate,
            ResultHandler handler) throws SQLException {
        SqlSession session = sqlSessionFactory.openSession();
        try {
            StatisticsMapper mapper = session.getMapper(StatisticsMapper.class);
            mapper.getHistory(gruppierungId, cumulate, gruppen, handler);
        } catch (Exception e) {
            log.log(Level.SEVERE, "SQL call throws exception", e);
        } finally {
            session.close();
        }
    }
}
