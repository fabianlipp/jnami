package nami.statistics;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;

/**
 * Schreibt CSV-Daten.
 * 
 * @author Fabian Lipp
 * 
 */
public class CsvWriter implements ResultHandler {
    private static final String FIELD_SEPARATOR = ",";

    private Writer out;
    private boolean headerWritten = false;

    private static Logger log = Logger.getLogger(CsvWriter.class.getName());

    /**
     * Erzeugt einen neuen <tt>CsvWriter</tt>, der in den übergebenen
     * <tt>Writer</tt> schreibt.
     * 
     * @param out
     *            <tt>Writer</tt>, in den die Ausgabe geschrieben wird
     * @throws IOException .
     */
    public CsvWriter(Writer out) throws IOException {
        this.out = new BufferedWriter(out);
    }

    /**
     * Schreibt das Ergebnis einer SQL-Anfrage im CSV-Format in den
     * Ausgabe-Strom. In der ersten Zeile werden dabei die Spaltenlabel aus der
     * Abfrage ausgegeben.
     * 
     * @param rs
     *            das Ergebnis der Abfrage, das ausgegeben werden soll
     * @throws IOException .
     * @throws SQLException
     *             Probleme beim Auslesen des SQL-ResultSets
     */
    public void writeResultSet(ResultSet rs) throws IOException, SQLException {
        ResultSetMetaData rsmd = rs.getMetaData();
        int num = rsmd.getColumnCount();
        for (int i = 1; i <= num; i++) {
            out.write(rsmd.getColumnLabel(i));
            if (i != num) {
                out.write(FIELD_SEPARATOR);
            }
        }
        out.write(System.lineSeparator());

        while (rs.next()) {
            for (int i = 1; i <= num; i++) {
                String str = rs.getString(i);
                if (str.contains(FIELD_SEPARATOR)) {
                    str = "\"" + str + "\"";
                }
                out.write(str);
                if (i != num) {
                    out.write(FIELD_SEPARATOR);
                }
            }
            out.write(System.lineSeparator());
        }

        // Hier flushen, da der BufferedWriter nirgends ordentlich geschlossen
        // wird
        out.flush();
    }

    @Override
    public void handleResult(ResultContext context) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = ((Map<String, Object>) context
                    .getResultObject());

            if (!headerWritten) {
                out.write(StringUtils.join(map.keySet(), FIELD_SEPARATOR));
                out.write(System.lineSeparator());
                headerWritten = true;
            }
            LinkedList<String> values = new LinkedList<>();
            for (Entry<String, Object> entry : map.entrySet()) {
                String str = entry.getValue().toString();
                if (str.contains(FIELD_SEPARATOR)) {
                    str = "\"" + str + "\"";
                }
                values.add(str);
            }
            out.write(StringUtils.join(values, FIELD_SEPARATOR));
            out.write(System.lineSeparator());

            // Hier flushen, da der BufferedWriter nirgends ordentlich
            // geschlossen wird
            out.flush();
        } catch (ClassCastException e) {
            log.warning("ResultContext does not contain a map or contents "
                    + "of map have wrong type. Classname is: "
                    + context.getResultObject().getClass().getName());
        } catch (IOException e) {
            log.log(Level.WARNING, "Could not write to CSV file", e);
        }
    }
}
