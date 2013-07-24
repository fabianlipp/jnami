package nami.statistics;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.http.client.ClientProtocolException;

import nami.connector.Geschlecht;
import nami.connector.NamiConnector;
import nami.connector.NamiServer;
import nami.connector.credentials.NamiCredentials;
import nami.connector.exception.NamiApiException;
import nami.connector.namitypes.NamiGruppierung;

public class NamiStatisticsTest {

    public static void main(String[] args) throws Exception {
        // Register the JDBC driver for MySQL.
        Class.forName("com.mysql.jdbc.Driver");

        // Define URL of database server for
        // database named mysql on the localhost
        // with the default port number 3306.
        String url = "jdbc:mysql://localhost:3306/jnami?useServerPrepStmts=true";

        // Get a connection to the database for a
        // user named root with a blank password.
        // This user is the default administrator
        // having full privileges to do anything.
        Connection dbcon = DriverManager.getConnection(url, "jnami",
                "yxz");

        NamiCredentials cred = new NamiCredentials("123456", "xyz");
        NamiConnector namicon = new NamiConnector(NamiServer.TESTSERVER, cred);
        namicon.namiLogin();
        
        NamiGruppierung rootGruppierung = NamiGruppierung.getGruppierungen(namicon);
        
        NamiStatisticsDatabase db = new NamiStatisticsDatabase(dbcon, namicon);
        db.createDatabase(rootGruppierung);
        
        int runId = db.writeNewStatisticRun();
        writeAnzahlForGruppierungAndChildren(db, namicon, runId, rootGruppierung);
        
        dbcon.close();
    }
    
    private static void writeAnzahlForGruppierungAndChildren(NamiStatisticsDatabase db, NamiConnector namicon, int runId, NamiGruppierung gruppierung) throws ClientProtocolException, NamiApiException, IOException, SQLException {
        for (NamiStatisticsGruppe gruppe : NamiStatisticsGruppe.GRUPPEN) {
            int[] anzahl = gruppe.getAnzahl(namicon, gruppierung.getId());
            db.writeAnzahl(gruppierung.getId(), gruppe.getId(), runId, Geschlecht.MAENNLICH, anzahl[0]);
            db.writeAnzahl(gruppierung.getId(), gruppe.getId(), runId, Geschlecht.WEIBLICH, anzahl[1]);
        }
        
        for (NamiGruppierung child : gruppierung.getChildren()) {
            writeAnzahlForGruppierungAndChildren(db, namicon, runId, child);
        }
    }
}
