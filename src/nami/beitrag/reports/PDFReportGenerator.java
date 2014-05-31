package nami.beitrag.reports;

import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

public class PDFReportGenerator {

    private static Logger log = Logger.getLogger(PDFReportGenerator.class
            .getName());

    public static <T> void generateReport(String report,
            Map<String, Object> params, Collection<T> data,
            String outputFilename) {
        try {
            InputStream reportStream = PDFReportGenerator.class
                    .getResourceAsStream(report + ".jasper");
            JasperPrint jasperPrint;
            jasperPrint = JasperFillManager.fillReport(reportStream, params,
                    new JRBeanCollectionDataSource(data));
            JasperExportManager.exportReportToPdfFile(jasperPrint,
                    outputFilename);

            log.info("Generated Report");
        } catch (JRException e) {
            log.log(Level.WARNING, "Could not generate Report", e);
        }
    }
}
