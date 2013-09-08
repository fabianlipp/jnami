package nami.db.schema;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

/**
 * Ruft Liquibase auf, um das Datenbank-Layout auf den aktuellen Stand zu
 * bringen.
 * 
 * @author Fabian Lipp
 * 
 */
public class SchemaUpdater {

    private String dbUrl;
    private String dbUsername;
    private String dbPassword;

    /**
     * Dateiname des Liquibase-Changelogs. Die Datei wird im Classpath im
     * gleichen Package wie diese Klasse gesucht.
     */
    private static final String CHANGELOG_FILENAME = "changelog.xml";

    private static Logger log = Logger.getLogger(SchemaUpdater.class.getName());

    /**
     * Erzeugt den <tt>SchemaUpdater</tt>.
     * 
     * @param dbDriver
     *            JDBC-Driver-Name
     * @param dbUrl
     *            JDBC-URL der Datenbank
     * @param dbUsername
     *            Benutzername für die Datenbank
     * @param dbPassword
     *            Passwort für die Datenbank
     */
    public SchemaUpdater(String dbDriver, String dbUrl, String dbUsername,
            String dbPassword) {
        // dbDriver wird momentan nicht verwendet
        this.dbUrl = dbUrl;
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
    }

    /**
     * Bringt das Datenbank-Schema auf den aktuellen Stand.
     */
    public void update() {
        Connection con = null;
        try {
            con = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
            DatabaseFactory dbFac = DatabaseFactory.getInstance();
            JdbcConnection jdbcCon = new JdbcConnection(con);
            Database db = dbFac.findCorrectDatabaseImplementation(jdbcCon);

            String changelog = SchemaUpdater.class.getPackage().getName();
            changelog = changelog.replace('.', '/');
            changelog = changelog + '/' + CHANGELOG_FILENAME;

            Liquibase liquibase = new Liquibase(changelog,
                    new ClassLoaderResourceAccessor(), db);
            liquibase.update(null);

        } catch (SQLException | LiquibaseException e) {
            log.log(Level.WARNING, "Error updating database schema", e);
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    // Ignore
                }
            }
        }
    }
}
