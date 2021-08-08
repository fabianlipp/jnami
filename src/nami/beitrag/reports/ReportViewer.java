package nami.beitrag.reports;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import nami.beitrag.db.BeitragMitglied;
import nami.beitrag.db.ReportsMapper;
import nami.connector.Halbjahr;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.view.JasperViewer;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

/**
 * Führt Auswertungen in der Datenbank durch und stellt diese mittels
 * JasperReports dar. Nachdem die Abfrage ausgeführt wurde, wird das Ergebnis in
 * einem eigenen Fenster dargestellt und kann aus diesem ggf. gespeichert
 * werden.
 * 
 * Die Klasse stellt für jede der möglichen Abfrage eine Methode bereit. Eine
 * Instanz des ReportViewers kann mehrfach verwendet werden, um verschiedene
 * Abfragen auszuführen. Er muss also nur einmal beim Aufruf des Programms
 * initialisiert werden.
 * 
 * @author Fabian Lipp
 * 
 */
public class ReportViewer {
    private final SqlSessionFactory sessionFactory;

    /* Dateinamen der Reports (daran wird jeweils .jasper ergänzt) */
    private static final String FILE_ABRECHNUNG_HALBJAHR = "abrechnung_halbjahr";
    private static final String FILE_MITGLIEDER_OHNE_SEPA = "mitglieder_ohne_sepa";

    private static final Logger LOGGER = Logger.getLogger(ReportViewer.class.getName());

    /**
     * Initialisiert den ReportViewer.
     * 
     * @param sessionFactory
     *            Verbindung zur Datenbank
     */
    public ReportViewer(SqlSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    private <T> void viewReport(String reportFile, Map<String, Object> params,
            Collection<T> data) {
        try {
            InputStream reportIS = ReportViewer.class
                    .getResourceAsStream(reportFile + ".jasper");
            JasperPrint jasperPrint;
            jasperPrint = JasperFillManager.fillReport(reportIS, params,
                    new JRBeanCollectionDataSource(data));
            // JasperExportManager.exportReportToPdfFile(jasperPrint,
            // "/home/fabian/test.pdf");
            LOGGER.info("Generated Report");
            JasperViewer.viewReport(jasperPrint, false);
        } catch (JRException e) {
            LOGGER.log(Level.WARNING, "Could not generate Report", e);
        }
    }

    /**
     * Zeigt die Abrechnung für ein Halbjahr an. Diese listet alle Mitglieder
     * und deren jeweilige Buchungen auf.
     * 
     * @param halbjahr
     *            Halbjahr, für das die Abrechnung erstellt werden soll
     * @param ausgeglichen
     *            Zeige auch Mitglieder mit ausgeglichenen Beitragskonten
     */
    public void viewAbrechnungHalbjahr(Halbjahr halbjahr, boolean ausgeglichen) {
        try (SqlSession session = sessionFactory.openSession()) {
            ReportsMapper mapper = session.getMapper(ReportsMapper.class);
            Collection<DataAbrechnungHalbjahr> data = mapper
                    .abrechnungHalbjahr(halbjahr, ausgeglichen);

            Map<String, Object> params = new HashMap<>();
            params.put("HALBJAHR", halbjahr);

            viewReport(FILE_ABRECHNUNG_HALBJAHR, params, data);
        }
    }

    /**
     * Zeigt eine Liste der aktiven Mitglieder, für die kein gültiges
     * SEPA-Mandat existiert.
     */
    public void viewMitgliederOhneSepaMandat() {
        try (SqlSession session = sessionFactory.openSession()) {
            ReportsMapper mapper = session.getMapper(ReportsMapper.class);
            Collection<BeitragMitglied> data = mapper
                    .mitgliederOhneSepaMandat();

            Map<String, Object> params = new HashMap<>();

            viewReport(FILE_MITGLIEDER_OHNE_SEPA, params, data);
        }
    }
}
