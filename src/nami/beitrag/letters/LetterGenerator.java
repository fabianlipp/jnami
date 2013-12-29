package nami.beitrag.letters;

import java.io.StringWriter;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import lombok.Getter;
import nami.beitrag.db.BeitragBuchung;
import nami.beitrag.db.BeitragMahnung;
import nami.beitrag.db.BeitragMapper;
import nami.beitrag.db.BeitragMitglied;
import nami.beitrag.db.BeitragRechnung;
import nami.beitrag.db.DataMitgliederForderungen;
import nami.beitrag.db.RechnungenMapper;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.tools.generic.DateTool;
import org.apache.velocity.tools.generic.MathTool;

public class LetterGenerator {

    private static final String TEMPLATE_RECHNUNGEN = "rechnung.vm";
    private static final String TEMPLATE_MAHNUNGEN = "mahnung.vm";
    private static final String PATH_TEMPLATES = LetterGenerator.class
            .getPackage().getName().replace(".", "/")
            + "/";

    private SqlSessionFactory sqlSessionFactory;

    static {
        Velocity.setProperty("resource.loader", "class");
        Velocity.setProperty("class.resource.loader.class",
                "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        Velocity.init();

    }

    public LetterGenerator(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    private static Template getTemplate(String templateName) {
        return Velocity.getTemplate(PATH_TEMPLATES + templateName);
    }

    @Getter
    public class RechnungRecipient {
        private BeitragMitglied mgl;
        private Collection<BeitragBuchung> buchungen;
        private String rechnungsNummer;
    }

    public boolean generateRechnungen(
            LinkedHashMap<Integer, Collection<BeitragBuchung>> rechnungen,
            Map<Integer, String> rechnungsNummern, Date rechnungsdatum,
            Date frist) {

        Template template = getTemplate(TEMPLATE_RECHNUNGEN);

        Collection<RechnungRecipient> recipients = new LinkedList<>();
        SqlSession session = sqlSessionFactory.openSession();
        try {
            BeitragMapper mapper = session.getMapper(BeitragMapper.class);
            for (Entry<Integer, Collection<BeitragBuchung>> rechnung : rechnungen
                    .entrySet()) {
                int mitgliedId = rechnung.getKey();
                BeitragMitglied mgl = mapper.getMitglied(mitgliedId);
                RechnungRecipient rcpt = new RechnungRecipient();
                rcpt.mgl = mgl;
                rcpt.buchungen = rechnung.getValue();
                rcpt.rechnungsNummer = rechnungsNummern.get(mitgliedId);
                recipients.add(rcpt);
            }
        } finally {
            session.close();
        }

        VelocityContext context = new VelocityContext();
        context.put("date", new DateTool());
        context.put("math", new MathTool());
        context.put("recipientList", recipients);
        context.put("rechnungsdatum", rechnungsdatum);
        context.put("frist", frist);

        StringWriter w = new StringWriter();
        template.merge(context, w);

        System.out.println(w);

        // TODO: richtige Rückgabe (überprüfe, ob Datei erzeugt wurde)
        return false;
    }

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
