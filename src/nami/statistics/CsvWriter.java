package nami.statistics;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * Schreibt CSV-Daten.
 * 
 * @author Fabian Lipp
 * 
 */
public class CsvWriter {
    private static final String FIELD_SEPARATOR = ",";

    private Writer out;

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
    }

    /**
     * Schließt den Ausgabestrom.
     * 
     * @throws IOException .
     */
    public void close() throws IOException {
        out.close();
    }
}
