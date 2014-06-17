package nami.beitrag.letters;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import nami.beitrag.NamiBeitragConfiguration;
import nami.beitrag.db.BeitragBrief;
import nami.beitrag.db.BriefeMapper;
import nami.beitrag.db.LastschriftenMapper;
import nami.beitrag.db.LastschriftenMapper.DataPrenotificationMandat;
import nami.beitrag.db.RechnungenMapper;
import nami.beitrag.db.RechnungenMapper.DataMahnungKomplett;
import nami.beitrag.db.RechnungenMapper.DataRechnungMitBuchungen;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.tools.generic.DateTool;

/**
 * Erstellt Briefe als LaTeX-Quelltexte. Dazu werden die Daten für die Briefe
 * aus der Datenbank geholt und mit Hilfe von Velocity in gegebene Templates
 * eingesetzt.
 * 
 * @author Fabian Lipp
 * 
 */
public class LetterGenerator {
    // Namen und Pfad zu den Template-Dateien
    private static final String TEMPLATE_RECHNUNGEN = "rechnung.vm";
    private static final String TEMPLATE_MAHNUNGEN = "mahnung.vm";
    private static final String TEMPLATE_PRENOTIFICATIONS = "prenotification.vm";
    private static final String PATH_TEMPLATES = LetterGenerator.class
            .getPackage().getName().replace(".", "/")
            + "/";

    private SqlSessionFactory sqlSessionFactory;
    private LetterDirectory dir;

    private String mRefPrefix;
    private String credId;

    private static Logger logger = Logger.getLogger(LetterGenerator.class
            .getName());

    // Globale Einstellungen für Velocity
    static {
        Velocity.setProperty("resource.loader", "class");
        Velocity.setProperty("class.resource.loader.class",
                "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        Velocity.init();

    }

    /**
     * Erzeugt einen neuen Brief-Generator.
     * 
     * @param sqlSessionFactory
     *            Verbindung zur Datenbank, aus der die Daten geholt werden
     * @param dir
     *            Verzeichnis, in dem die erstellten Briefe (als
     *            LaTeX-Quelltexte) abgelegt werden
     * @param conf
     *            Konfiguration des Nami-Beitrags-Tools
     */
    public LetterGenerator(SqlSessionFactory sqlSessionFactory,
            LetterDirectory dir, NamiBeitragConfiguration conf) {
        this.sqlSessionFactory = sqlSessionFactory;
        this.dir = dir;
        mRefPrefix = conf.getSepaMRefPrefix();
        credId = conf.getSepaCreditorId();
    }

    private static Template getTemplate(String templateName) {
        return Velocity.getTemplate(PATH_TEMPLATES + templateName);
    }

    /**
     * Erzeugt einen einzelnen Brief. Die übergebenen Parameter werden teilweise
     * zur Erstellung des Dateinamens benötigt.
     * 
     * @param type
     *            Typ des Briefs (bestimmt u. a. die verwendete Vorlage und die
     *            Datenbank-Tabelle aus der die Daten geholt werden)
     * @param id
     *            ID in der jeweiligen Datenbank-Tabelle (z. B. rechnungId,
     *            mahnungId)
     * @param datum
     *            Datum, an dem der Brief versandt wird (nur für den Dateinamen)
     * @param nachname
     *            Nachname des Empfängers (nur für den Dateinamen)
     * @param vorname
     *            Vorname des Empfängers (nur für den Dateinamen
     * @return Brief-Objekt, das erstellt wurde (enthält briefId und Dateiname)
     */
    public BeitragBrief generateLetter(LetterType type, int id, Date datum,
            String nachname, String vorname) {
        LinkedList<Integer> ids = new LinkedList<Integer>();
        ids.add(id);
        File f = dir.createFilenameSingle(type, datum, nachname, vorname);
        return generateLetters(type, ids, datum, f);
    }

    /**
     * Erzeugt eine Sammlung von Briefen. Die übergebenen Parameter werden
     * teilweise zur Erstellung des Dateinamens benötigt.
     * 
     * @param type
     *            Typ der Briefe (bestimmt u. a. die verwendete Vorlage und die
     *            Datenbank-Tabelle aus der die Daten geholt werden)
     * @param ids
     *            IDs in der jeweiligen Datenbank-Tabelle (z. B. rechnungId,
     *            mahnungId)
     * @param datum
     *            Datum, an dem die Briefe versandt werden (nur für den
     *            Dateinamen)
     * @return erstelltes Brief-Objekt (enthält briefId und Dateiname)
     */
    public BeitragBrief generateLetters(LetterType type,
            Collection<Integer> ids, Date datum) {
        File f = dir.createFilenameMultiple(type, datum);
        return generateLetters(type, ids, datum, f);
    }

    /**
     * Erstellt den/die übergebenen Brief(e) in der angegebenen Datei.
     * 
     * @param type
     *            Typ der Briefe
     * @param ids
     *            IDs in der Datenbank
     * @param file
     *            Dateiname, unter dem die Briefe geschrieben werden
     * @return erstelltes Brief-Objekt (enthält briefId und Dateiname)
     */
    private BeitragBrief generateLetters(LetterType type,
            Collection<Integer> ids, Date datum, File file) {
        if (ids.size() < 1) {
            throw new IllegalArgumentException(
                    "Did not get any ids to generate letters");
        }

        FileWriter fileWriter;
        SqlSession session = sqlSessionFactory.openSession();
        try {
            fileWriter = new FileWriter(file);
            Writer w = new BufferedWriter(fileWriter);

            switch (type) {
            case RECHNUNG:
                generateRechnungen(ids, w);
                break;
            case MAHNUNG:
                generateMahnungen(ids, w);
                break;
            case PRENOTIFICATION:
                generatePrenotifications(ids, w);
                break;
            default:
                throw new IllegalArgumentException(
                        "Letter type not implemented: " + type);
            }
            w.close();

            if (file.exists() && file.length() > 0) {
                BeitragBrief brief = new BeitragBrief();
                brief.setDateiname(file.getName());
                brief.setDatum(datum);
                brief.setTyp(type);
                brief.setKompiliert(null);

                BriefeMapper mapper = session.getMapper(BriefeMapper.class);
                mapper.insertBrief(brief);
                session.commit();

                return brief;
            } else {
                logger.log(Level.SEVERE, "Did not write letter file");
                return null;
            }

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not write letter file", e);
            return null;
        } finally {
            session.close();
        }
    }

    private void generateRechnungen(Collection<Integer> rechnungIds, Writer w) {
        SqlSession session = sqlSessionFactory.openSession();
        try {
            RechnungenMapper mapper = session.getMapper(RechnungenMapper.class);

            // Lade Rechnungen und zugehörige Buchungen aus Datenbank
            LinkedList<DataRechnungMitBuchungen> rechnungen = new LinkedList<>();
            for (int rechnungId : rechnungIds) {
                rechnungen.add(mapper.getRechnungMitBuchungen(rechnungId));
            }

            // Übergebe Daten an Velocity
            VelocityContext context = new VelocityContext();
            context.put("date", new DateTool());
            context.put("df", getCurrencyFormatter());
            context.put("rechnungen", rechnungen);

            // Template füllen
            Template template = getTemplate(TEMPLATE_RECHNUNGEN);
            template.merge(context, w);
        } finally {
            session.close();
        }
    }

    private void generateMahnungen(Collection<Integer> mahnungIds, Writer w) {
        SqlSession session = sqlSessionFactory.openSession();
        try {
            RechnungenMapper mapper = session.getMapper(RechnungenMapper.class);

            // Lade Rechnungen und zugehörige Buchungen aus Datenbank
            LinkedList<DataMahnungKomplett> mahnungen = new LinkedList<>();
            for (int mahnungId : mahnungIds) {
                mahnungen.add(mapper.getMahnungKomplett(mahnungId));
            }

            // Übergebe Daten an Velocity
            VelocityContext context = new VelocityContext();
            context.put("date", new DateTool());
            context.put("df", getCurrencyFormatter());
            context.put("mahnungen", mahnungen);

            // Template füllen
            Template template = getTemplate(TEMPLATE_MAHNUNGEN);
            template.merge(context, w);
        } finally {
            session.close();
        }
    }

    private void generatePrenotifications(
            Collection<Integer> prenotificationIds, Writer w) {
        SqlSession session = sqlSessionFactory.openSession();
        try {
            LastschriftenMapper mapper = session
                    .getMapper(LastschriftenMapper.class);

            // Lade Rechnungen und zugehörige Buchungen aus Datenbank
            LinkedList<DataPrenotificationMandat> prenots = new LinkedList<>();
            for (int prenotId : prenotificationIds) {
                prenots.add(mapper.getPrenotificationMitMandat(prenotId));
            }

            // Übergebe Daten an Velocity
            VelocityContext context = new VelocityContext();
            context.put("date", new DateTool());
            context.put("df", getCurrencyFormatter());
            context.put("prenots", prenots);
            context.put("creditorId", credId);
            context.put("mrefPrefix", mRefPrefix);

            // Template füllen
            Template template = getTemplate(TEMPLATE_PRENOTIFICATIONS);
            template.merge(context, w);
        } finally {
            session.close();
        }
    }

    private DecimalFormat getCurrencyFormatter() {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.GERMAN);
        return new DecimalFormat("#,##0.00", sym);
    }
}
