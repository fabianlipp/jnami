package nami.beitrag.reports;

import nami.beitrag.db.ReportsMapper;
import nami.connector.Halbjahr;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.view.JasperViewer;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    /* Dateinamen der Reports */
    private static final String FILE_ABRECHNUNG_HALBJAHR = "abrechnung_halbjahr.jasper";
    private static final String FILE_ABRECHNUNG_HALBJAHR_TYPEN = "abrechnung_halbjahr_typen.jasper";
    private static final String FILE_MITGLIEDER_OHNE_SEPA = "mitglieder_ohne_sepa.jasper";
    private static final String FILE_ANZAHL_BUCHUNGEN_PRO_HALBJAHR = "anzahl_buchungen_pro_halbjahr.jasper";
    private static final String FILE_UNAUSGEGLICHENE_BEITRAGSKONTEN ="unausgeglichene_beitragskonten.jasper";
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

    private void viewReport(String reportFile, Map<String, Object> params,
                            Function<ReportsMapper, Collection<?>> query) {
        try (SqlSession session = sessionFactory.openSession()) {
            ReportsMapper mapper = session.getMapper(ReportsMapper.class);
            Collection<?> data = query.apply(mapper);
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(data);

            InputStream reportIS = ReportViewer.class.getResourceAsStream(reportFile);
            JasperPrint jasperPrint = JasperFillManager.fillReport(reportIS, params, dataSource);
            LOGGER.info("Generated Report");
            JasperViewer.viewReport(jasperPrint, false);
        } catch (JRException e) {
            LOGGER.log(Level.WARNING, "Could not generate Report", e);
        }
    }

    private void viewReport(String reportFile, Function<ReportsMapper, Collection<?>> query) {
        this.viewReport(reportFile, new HashMap<>(), query);
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
        Map<String, Object> params = new HashMap<>();
        params.put("HALBJAHR", halbjahr);
        viewReport(FILE_ABRECHNUNG_HALBJAHR, params,
                reportsMapper -> reportsMapper.abrechnungHalbjahr(halbjahr, ausgeglichen));
    }

    /**
     * Zeigt die Abrechnung für ein Halbjahr an. Diese listet die Summen der
     * Buchungen für die jeweiligen Buchungstypen auf.
     *
     * @param halbjahr
     *            Halbjahr, für das die Abrechnung erstellt werden soll
     */
    public void viewAbrechnungHalbjahrNachTypen(Halbjahr halbjahr) {
        Map<String, Object> params = new HashMap<>();
        params.put("HALBJAHR", halbjahr);
        viewReport(FILE_ABRECHNUNG_HALBJAHR_TYPEN, params,
                reportsMapper -> reportsMapper.abrechnungNachTypenHalbjahr(halbjahr));
    }

    /**
     * Zeigt eine Liste der aktiven Mitglieder, für die kein gültiges
     * SEPA-Mandat existiert.
     */
    public void viewMitgliederOhneSepaMandat() {
        viewReport(FILE_MITGLIEDER_OHNE_SEPA, ReportsMapper::mitgliederOhneSepaMandat);
    }

    /**
     * Zeigt die Anzahl der Buchungen (aufgeteilt in Vorausbuchungen
     * und endgültige Buchungen) pro Halbjahr.
     */
    public void viewAnzahlBuchungenProHalbjahr() {
        viewReport(FILE_ANZAHL_BUCHUNGEN_PRO_HALBJAHR, ReportsMapper::anzahlBuchungenProHalbjahr);
    }

    /**
     * Zeigt die unausgeglichenen Beitragskonten gruppiert nach
     * Halbjahr inkl. Saldo an.
     */
    public void viewUnausgeglicheneBeitragskontenProHalbjahr() {
        viewReport(FILE_UNAUSGEGLICHENE_BEITRAGSKONTEN, ReportsMapper::unausgeglicheneBeitragskontenProHalbjahr);
    }
}
