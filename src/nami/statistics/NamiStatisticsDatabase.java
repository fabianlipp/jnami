package nami.statistics;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.logging.Logger;

import nami.connector.Ebene;
import nami.connector.namitypes.NamiGruppierung;

/**
 * Diese Klasse abstrahiert die Zugriffe auf die Datenbank. Durch die Methoden
 * können die benötigten SQL-Befehle ausgeführt werden.
 * 
 * @author Fabian Lipp
 * 
 */
public class NamiStatisticsDatabase {

    private Connection dbcon;
    private Collection<NamiStatisticsGruppe> gruppen;
    private Logger log;

    // Prepared Statements, die in den einzelnen Methoden verwendet werden
    private PreparedStatement stmtTestGruppierung = null;
    private PreparedStatement stmtInsertGruppierung = null;
    private PreparedStatement stmtTestGruppe = null;
    private PreparedStatement stmtInsertGruppe = null;
    private PreparedStatement stmtInsertAnzahl = null;

    /**
     * Erzeugt ein Objekt zum Datenbankzugriff.
     * 
     * @param dbcon
     *            Verbindung zur Datenbank
     * @param gruppen
     *            Gruppen, die in der Statistik erfasst werden
     * @throws SQLException
     *             Probleme beim Ausführen der SQL-Kommandos
     */
    public NamiStatisticsDatabase(Connection dbcon,
            Collection<NamiStatisticsGruppe> gruppen) throws SQLException {
        this.dbcon = dbcon;
        this.gruppen = gruppen;
        log = Logger.getLogger(NamiStatisticsDatabase.class.getCanonicalName());

        String sql = "INSERT INTO statistics_data "
                + "     (gruppierungId, gruppeId, runid, anzahl) "
                + "VALUES (?, ?, ?, ?)";
        logQuery(sql, true);
        stmtInsertAnzahl = dbcon.prepareStatement(sql);
    }

    private void logQuery(String sql, boolean prepared) {
        if (prepared) {
            log.finer("Prepared Statement:" + System.lineSeparator() + sql);
        } else {
            log.finer("SQL Query:" + System.lineSeparator() + sql);
        }
    }

    private void logQuery(String sql) {
        logQuery(sql, false);
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
        String sql;
        Statement stmt;

        // Erzeuge Tabellen
        stmt = dbcon.createStatement();
        sql = "CREATE TABLE IF NOT EXISTS gruppierung ("
                + "gruppierungId CHAR(6) NOT NULL,"
                + "descriptor VARCHAR(255) NOT NULL,"
                + "dioezeseId CHAR(6) DEFAULT NULL,"
                + "bezirkId CHAR(6) DEFAULT NULL,"
                + "PRIMARY KEY (gruppierungId))";
        logQuery(sql);
        stmt.execute(sql);
        sql = "CREATE TABLE IF NOT EXISTS statistics_gruppe ("
                + "gruppeId INT NOT NULL,"
                + "bezeichnung VARCHAR(255) NOT NULL,"
                + "PRIMARY KEY (gruppeId))";
        logQuery(sql);
        stmt.execute(sql);
        sql = "CREATE TABLE IF NOT EXISTS statistics_run ("
                + "runId INT NOT NULL AUTO_INCREMENT,"
                + "datum DATETIME NOT NULL,           "
                + "PRIMARY KEY (runId))";
        logQuery(sql);
        stmt.execute(sql);
        sql = "CREATE TABLE IF NOT EXISTS statistics_data("
                + "gruppierungId CHAR(6) NOT NULL,"
                + "gruppeId INT NOT NULL,"
                + "runId INT NOT NULL,"
                + "anzahl INT NOT NULL,"
                + "PRIMARY KEY (gruppierungId, gruppeId, runId),"
                + "FOREIGN KEY (gruppierungId) REFERENCES gruppierung (gruppierungId),"
                + "FOREIGN KEY (gruppeId) REFERENCES statistics_gruppe (gruppeId),"
                + "FOREIGN KEY (runId) REFERENCES statistics_run (runId) )";
        logQuery(sql);
        stmt.execute(sql);

        // Füge Gruppierungen in Datenbank ein, falls noch nicht vorhanden
        writeGruppierungToDb(rootGruppierung);

        // Füge erfasste Gruppen in Datenbank ein, falls noch nicht vorhanden
        for (NamiStatisticsGruppe gruppe : gruppen) {
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
        String sql;
        ResultSet rs;
        if (stmtTestGruppierung == null) {
            sql = "SELECT COUNT(*) " + "FROM gruppierung "
                    + "WHERE gruppierungId = ?";
            logQuery(sql, true);
            stmtTestGruppierung = dbcon.prepareStatement(sql);
        }

        stmtTestGruppierung.setString(1, id);
        rs = stmtTestGruppierung.executeQuery();

        rs.next();
        int count = rs.getInt(1);

        if (count == 0) {
            // Gruppierung noch nicht vorhanden
            if (stmtInsertGruppierung == null) {
                sql = "INSERT INTO gruppierung "
                        + "(gruppierungId, descriptor, dioezeseId, bezirkId)"
                        + "VALUES (?, ?, ?, ?)";
                logQuery(sql, true);
                stmtInsertGruppierung = dbcon.prepareStatement(sql);
            }
            stmtInsertGruppierung.setString(1, id);
            stmtInsertGruppierung.setString(2, descriptor);
            stmtInsertGruppierung.setString(3, dioezese);
            stmtInsertGruppierung.setString(4, bezirk);
            /*
             * if (dioezese != -1) { stmtInsertGruppierung.setInt(3, parent); }
             * else { stmtInsertGruppierung.setNull(3, Types.INTEGER); }
             */
            stmtInsertGruppierung.execute();
        }
    }

    private void writeGruppeToDb(int id, String bezeichnung)
            throws SQLException {
        String sql;
        ResultSet rs;
        if (stmtTestGruppe == null) {
            sql = "SELECT COUNT(*)          "
                    + "FROM statistics_gruppe           "
                    + "WHERE gruppeId = ?";
            logQuery(sql, true);
            stmtTestGruppe = dbcon.prepareStatement(sql);
        }
        stmtTestGruppe.setInt(1, id);
        rs = stmtTestGruppe.executeQuery();

        rs.next();
        int count = rs.getInt(1);

        if (count == 0) {
            if (stmtInsertGruppe == null) {
                sql = "INSERT INTO statistics_gruppe (gruppeId, bezeichnung)"
                        + "VALUES (?, ?)";
                logQuery(sql, true);
                stmtInsertGruppe = dbcon.prepareStatement(sql);
            }
            stmtInsertGruppe.setInt(1, id);
            stmtInsertGruppe.setString(2, bezeichnung);
            stmtInsertGruppe.execute();
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
    public int writeNewStatisticRun() throws SQLException {
        String sql = "INSERT INTO statistics_run(datum)     "
                + "VALUES (NOW())";
        Statement stmt = dbcon.createStatement();
        logQuery(sql);
        stmt.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
        ResultSet rs = stmt.getGeneratedKeys();
        rs.next();
        return rs.getInt(1);
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
    public void writeAnzahl(String gruppierungsnummer, int gruppeId, int runId,
            int anzahl) throws SQLException {
        stmtInsertAnzahl.setString(1, gruppierungsnummer);
        stmtInsertAnzahl.setInt(2, gruppeId);
        stmtInsertAnzahl.setInt(3, runId);
        stmtInsertAnzahl.setInt(4, anzahl);
        stmtInsertAnzahl.executeUpdate();
    }

    /**
     * Liefert die durchgeführten Runs aus der Datenbank.
     * 
     * @return Runs mit ID und Datum
     * @throws SQLException
     *             Probleme beim Ausführen der SQL-Kommandos
     */
    public ResultSet getRuns() throws SQLException {
        String sql = "SELECT runId, datum              "
                + "FROM statistics_run                 "
                + "ORDER BY datum DESC";
        Statement stmt = dbcon.createStatement();
        logQuery(sql);
        stmt.execute(sql);
        return stmt.getResultSet();
    }

    /**
     * Liefert die ID des zuletzt durchgeführten Runs.
     * 
     * @return ID des letzten Runs
     * @throws SQLException
     *             Probleme beim Ausführen der SQL-Kommandos
     */
    public int getLatestRunId() throws SQLException {
        String sql = "SELECT runId                     "
                + "FROM statistics_run                 "
                + "ORDER BY datum DESC                             "
                + "LIMIT 1";
        Statement stmt = dbcon.createStatement();
        logQuery(sql);
        stmt.execute(sql);
        ResultSet rs = stmt.getResultSet();
        if (rs.next()) {
            return rs.getInt(1);
        } else {
            return -1;
        }
    }

    /**
     * Liefert die Mitgliederzahlen aller sichtbaren Gruppierungen im zuletzt
     * ausgeführten Run.
     * 
     * @param cumulate
     *            falls <tt>true</tt> werden zu den Mitgliederzahlen einer
     *            Gruppierung alle untergeordneten Gruppierungen aufsummiert
     * @return Gruppierungen mit ID, Bezeichnung und Mitgliederzahl in den
     *         vorgegebenen Gruppen
     * @throws SQLException
     *             Probleme beim Ausführen der SQL-Kommandos
     */
    public ResultSet getStatsAllGruppierungen(boolean cumulate)
            throws SQLException {
        return getStatsAllGruppierungen(getLatestRunId(), cumulate);
    }

    /**
     * Liefert die Mitgliederzahlen aller sichtbaren Gruppierungen in einem
     * vorgegebenen Run.
     * 
     * @param runId
     *            ID des Runs dessen Mitgliederzahlen ausgegeben werden sollen
     * @param cumulate
     *            falls <tt>true</tt> werden zu den Mitgliederzahlen einer
     *            Gruppierung alle untergeordneten Gruppierungen aufsummiert
     * @return Gruppierungen mit ID, Bezeichnung und Mitgliederzahl in den
     *         vorgegebenen Gruppen
     * @throws SQLException
     *             Probleme beim Ausführen der SQL-Kommandos
     */
    public ResultSet getStatsAllGruppierungen(int runId, boolean cumulate)
            throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT g.gruppierungId AS Gruppierungsnummer, "
                        + "g.descriptor AS Gruppierungsname");
        for (NamiStatisticsGruppe gruppe : gruppen) {
            sql.append(", SUM(CASE WHEN gruppeId = ");
            sql.append(gruppe.getId());
            sql.append(" THEN anzahl END) AS ");
            sql.append(gruppe.getBezeichnung());
        }
        sql.append(" ");
        sql.append("FROM gruppierung AS g, gruppierung AS c, statistics_data AS d ");
        sql.append("WHERE runId = ? ");
        sql.append("  AND d.gruppierungId = c.gruppierungId ");
        if (cumulate) {
            sql.append("  AND (g.gruppierungId = c.gruppierungId ");
            sql.append("    OR g.gruppierungId = c.bezirkId ");
            sql.append("    OR g.gruppierungId = c.dioezeseId) ");
        } else {
            sql.append("  AND g.gruppierungId = c.gruppierungId ");
        }
        sql.append("GROUP BY g.gruppierungId");

        logQuery(sql.toString(), true);
        PreparedStatement stmt = dbcon.prepareStatement(sql.toString());
        stmt.setInt(1, runId);
        stmt.execute();
        return stmt.getResultSet();
    }

    /**
     * Liefert alle vorhandenen Mitgliederzahlen einer Gruppierung.
     * 
     * @param gruppierungId
     *            Gruppierungsnummer
     * @param cumulate
     *            falls <tt>true</tt> werden zu den Mitgliederzahlen einer
     *            Gruppierung alle untergeordneten Gruppierungen aufsummiert
     * @return Runs mit ID, Datum und den Mitgliederzahlen der Gruppierung in
     *         den einzelnen vorgegebenen Gruppen
     * @throws SQLException
     *             Probleme beim Ausführen der SQL-Kommandos
     */
    public ResultSet getHistory(String gruppierungId, boolean cumulate)
            throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT runId AS RunID, "
                + "datum AS Datum");
        for (NamiStatisticsGruppe gruppe : gruppen) {
            sql.append(", SUM(CASE WHEN gruppeId = ");
            sql.append(gruppe.getId());
            sql.append(" THEN anzahl END) AS ");
            sql.append(gruppe.getBezeichnung());
        }
        sql.append(" ");
        sql.append("FROM statistics_data                  "
                + "    NATURAL JOIN statistics_run        ");
        if (cumulate) {
            sql.append("NATURAL JOIN gruppierung ");
            sql.append("WHERE gruppierungId = ? OR bezirkId = ? OR dioezeseId = ? "
                    + "GROUP BY runId");
        } else {
            sql.append("WHERE gruppierungId = ?                "
                    + "GROUP BY runId");
        }
        logQuery(sql.toString(), true);
        PreparedStatement stmt = dbcon.prepareStatement(sql.toString());
        for (int i = 1; i <= stmt.getParameterMetaData().getParameterCount(); i++) {
            stmt.setString(i, gruppierungId);
        }
        stmt.execute();
        return stmt.getResultSet();
    }
}
