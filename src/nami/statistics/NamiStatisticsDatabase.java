package nami.statistics;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import nami.connector.Geschlecht;
import nami.connector.NamiApiException;
import nami.connector.NamiConnector;
import nami.connector.namitypes.NamiGruppierung;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;


public class NamiStatisticsDatabase {
    
    private Connection dbcon;
    private NamiConnector namicon;
    
    private PreparedStatement stmtTestGruppierung = null;
    private PreparedStatement stmtInsertGruppierung = null;
    private PreparedStatement stmtTestGruppe = null;
    private PreparedStatement stmtInsertGruppe = null;
    
    private PreparedStatement stmtInsertAnzahl = null;
    
    public NamiStatisticsDatabase(Connection dbcon, NamiConnector namicon) throws SQLException {
        this.dbcon = dbcon;
        this.namicon = namicon;
        
        String sql = "INSERT INTO statistics_data " +
        		"     (gruppierungId, gruppeId, runid, geschlecht, anzahl) " +
        		"VALUES (?, ?, ?, ?, ?)";
        stmtInsertAnzahl = dbcon.prepareStatement(sql);
    }
    
    public void createDatabase(NamiGruppierung rootGruppierung) throws JsonIOException, JsonSyntaxException, IllegalStateException, NamiApiException, IOException, URISyntaxException, SQLException {
        String sql;
        Statement stmt;
        
        // Erzeuge Tabellen
        stmt = dbcon.createStatement();
        sql = "CREATE TABLE IF NOT EXISTS gruppierung (" +
                "gruppierungid INT NOT NULL AUTO_INCREMENT," + 
                "descriptor VARCHAR(255) NOT NULL," +
                "parent INT NULL," +
                "PRIMARY KEY (gruppierungid)," +
                "FOREIGN KEY (parent) REFERENCES gruppierung (gruppierungid))";
        stmt.execute(sql);
        sql = "CREATE TABLE IF NOT EXISTS statistics_gruppe (" +
                "gruppeid INT NOT NULL," + 
                "bezeichnung VARCHAR(255) NOT NULL," +
                "PRIMARY KEY (gruppeid))";
        stmt.execute(sql);
        sql = "CREATE TABLE IF NOT EXISTS statistics_run (" +
                "runid INT NOT NULL AUTO_INCREMENT," + 
                "datum DATETIME NOT NULL," +
                "PRIMARY KEY (runid))";        
        stmt.execute(sql);
        sql = "CREATE TABLE IF NOT EXISTS statistics_data(" +
                "gruppierungid INT NOT NULL," +
                "gruppeid INT NOT NULL," +
                "runid INT NOT NULL," +
                "geschlecht ENUM('m','w') NOT NULL," +
                "anzahl INT NOT NULL," +
                "PRIMARY KEY (gruppierungid, gruppeid, runid, geschlecht)," +
                "FOREIGN KEY (gruppierungid) REFERENCES gruppierung (gruppierungid)," +
                "FOREIGN KEY (gruppeid) REFERENCES statistics_gruppe (gruppeid)," +
                "FOREIGN KEY (runid) REFERENCES statistics_run (runid) )";
        stmt.execute(sql);
                
        // Füge Gruppierungen in Datenbank ein, falls noch nicht vorhanden
        writeGruppierungToDb(rootGruppierung, -1);
        
        // Füge erfasste Gruppen in Datenbank ein, falls noch nicht vorhanden
        for (NamiStatisticsGruppe gruppe : NamiStatisticsGruppe.GRUPPEN) {
            writeGruppeToDb(gruppe.getId(), gruppe.getBezeichnung());
        }
    }
    
    private void writeGruppierungToDb(NamiGruppierung grp, int parent) throws SQLException {
        writeGruppierungToDb(grp.getId(), grp.getDescriptor(), parent);
        
        for (NamiGruppierung child : grp.getChildren()) {
            writeGruppierungToDb(child, grp.getId());
        }
    }
    
    private void writeGruppierungToDb(int id, String descriptor, int parent) throws SQLException {
        String sql;
        ResultSet rs;
        if (stmtTestGruppierung == null) {
            sql = "SELECT COUNT(*) " +
                    "FROM gruppierung " + 
                    "WHERE gruppierungid = ?";
            stmtTestGruppierung = dbcon.prepareStatement(sql);
        }
        
        stmtTestGruppierung.setInt(1, id);
        rs = stmtTestGruppierung.executeQuery();
        
        rs.next();
        int count = rs.getInt(1);
        
        if (count == 0) {
            if (stmtInsertGruppierung == null) {
                sql = "INSERT INTO gruppierung (gruppierungid, descriptor, parent)" +
                        "VALUES (?, ?, ?)";
                stmtInsertGruppierung = dbcon.prepareStatement(sql);
            }
            stmtInsertGruppierung.setInt(1, id);
            stmtInsertGruppierung.setString(2, descriptor);
            if (parent != -1) {
                stmtInsertGruppierung.setInt(3, parent);
            } else {
                stmtInsertGruppierung.setNull(3, Types.INTEGER);                
            }
            stmtInsertGruppierung.execute();
        }
    }
    
    private void writeGruppeToDb(int id, String bezeichnung) throws SQLException {
        String sql;
        ResultSet rs;
        if (stmtTestGruppe == null) {
            sql = "SELECT COUNT(*) " +
                    "FROM statistics_gruppe " + 
                    "WHERE gruppeid = ?";
            stmtTestGruppe = dbcon.prepareStatement(sql);
        }
        stmtTestGruppe.setInt(1, id);   
        rs = stmtTestGruppe.executeQuery();
        
        rs.next();
        int count = rs.getInt(1);
        
        if (count == 0) {
            if (stmtInsertGruppe == null) {
                sql = "INSERT INTO statistics_gruppe (gruppeid, bezeichnung)" +
                        "VALUES (?, ?)";
                stmtInsertGruppe= dbcon.prepareStatement(sql);
            }
            stmtInsertGruppe.setInt(1, id);
            stmtInsertGruppe.setString(2, bezeichnung);
            stmtInsertGruppe.execute();
        }
    }

    public int writeNewStatisticRun() throws SQLException {
        String sql = "INSERT INTO statistics_run(datum) " +
        		"VALUES (NOW())";
        Statement stmt = dbcon.createStatement();
        stmt.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
        ResultSet rs = stmt.getGeneratedKeys();
        rs.next();
        return rs.getInt(1);
    }
    
    public void writeAnzahl(int gruppierungId, int gruppeId, int runId, Geschlecht geschlecht, int anzahl) throws SQLException {
        stmtInsertAnzahl.setInt(1, gruppierungId);
        stmtInsertAnzahl.setInt(2, gruppeId);
        stmtInsertAnzahl.setInt(3, runId);
        switch (geschlecht) {
        case MAENNLICH:
            stmtInsertAnzahl.setString(4, "m");
            break;
        case WEIBLICH:
            stmtInsertAnzahl.setString(4, "w");
            break;
        }
        stmtInsertAnzahl.setInt(5, anzahl);
        stmtInsertAnzahl.executeUpdate();
    }
}

