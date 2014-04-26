package nami.beitrag.letters;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import nami.beitrag.db.BeitragBrief;
import nami.beitrag.db.BeitragMahnung;
import nami.beitrag.db.BeitragMapper;
import nami.beitrag.db.BeitragMitglied;
import nami.beitrag.db.BeitragRechnung;
import nami.beitrag.db.BriefeMapper;
import nami.beitrag.db.RechnungenMapper;
import nami.beitrag.db.RechnungenMapper.DataRechnungMitBuchungen;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.tools.generic.DateTool;
import org.apache.velocity.tools.generic.MathTool;

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
    private static final String PATH_TEMPLATES = LetterGenerator.class
            .getPackage().getName().replace(".", "/")
            + "/";

    private SqlSessionFactory sqlSessionFactory;
    private LetterDirectory dir;

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
     */
    public LetterGenerator(SqlSessionFactory sqlSessionFactory,
            LetterDirectory dir) {
        this.sqlSessionFactory = sqlSessionFactory;
        this.dir = dir;
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
        return generateLetters(type, ids, f);
    }

    /**
     * Erzeugt eine Sammlung von Briefen. Die übergebenen Parameter werden teilweise
     * zur Erstellung des Dateinamens benötigt.
     * 
     * @param type
     *            Typ der Briefe (bestimmt u. a. die verwendete Vorlage und die
     *            Datenbank-Tabelle aus der die Daten geholt werden)
     * @param ids
     *            IDs in der jeweiligen Datenbank-Tabelle (z. B. rechnungId,
     *            mahnungId)
     * @param datum
     *            Datum, an dem die Briefe versandt werden (nur für den Dateinamen)
     * @return erstelltes Brief-Objekt (enthält briefId und Dateiname)
     */
    public BeitragBrief generateLetters(LetterType type,
            Collection<Integer> ids, Date datum) {
        File f = dir.createFilenameMultiple(type, datum);
        return generateLetters(type, ids, f);
    }

    /**
     * Erstellt den/die übergebenen Brief(e) in der angegebenen Datei.
     * @param type Typ der Briefe
     * @param ids IDs in der Datenbank
     * @param file Dateiname, unter dem die Briefe geschrieben werden
     * @return erstelltes Brief-Objekt (enthält briefId und Dateiname)
     */
    private BeitragBrief generateLetters(LetterType type,
            Collection<Integer> ids, File file) {
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
            default:
                throw new IllegalArgumentException(
                        "Letter type not implemented: " + type);
            }
            w.close();

            if (file.exists() && file.length() > 0) {
                BeitragBrief brief = new BeitragBrief();
                brief.setDateiname(file.getName());
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
            context.put("rechnungen", rechnungen);

            // Template füllen
            Template template = getTemplate(TEMPLATE_RECHNUNGEN);
            template.merge(context, w);

        } finally {
            session.close();
        }
    }

    private boolean generateMahnungen(Collection<Integer> mahnungIds, Writer w) {

        // TODO
        return false;
    }

    private boolean generatePrenotifications(
            Collection<Integer> prenotificationIds, Writer w) {

        // TODO
        return false;
    }

    // deprecated
    public boolean generateMahnung(BeitragMahnung mahnung) {

        Template template = getTemplate(TEMPLATE_MAHNUNGEN);
        VelocityContext context = new VelocityContext();
        context.put("date", new DateTool());
        context.put("math", new MathTool());
        context.put("mahnung", mahnung);

        SqlSession session = sqlSessionFactory.openSession();
        try {
            BeitragMapper beitragMapper = session
                    .getMapper(BeitragMapper.class);
            RechnungenMapper rechnungMapper = session
                    .getMapper(RechnungenMapper.class);

            BeitragRechnung rechnung = rechnungMapper.getRechnung(mahnung
                    .getRechnungId());
            BeitragMitglied mitglied = beitragMapper.getMitglied(rechnung
                    .getMitgliedId());
            context.put("rechnung", rechnung);
            context.put("mgl", mitglied);
        } finally {
            session.close();
        }

        StringWriter w = new StringWriter();
        template.merge(context, w);

        System.out.println(w);

        // TODO: zusätzlich Original-Rechnung auf zweiter Seite in PDF ausgeben

        // TODO: richtige Rückgabe (überprüfe, ob Datei erzeugt wurde)
        return false;
    }
}
